package com.base.find_phone_clap_detector.ui.fragments

import android.Manifest
import android.app.Dialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdSize
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.horse.identification.extensions.gone
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.databinding.DialogChooseActionBinding
import com.base.find_phone_clap_detector.databinding.FragmentActivateDetectionBinding
import com.base.find_phone_clap_detector.managers.AnalyticsManager
import com.base.find_phone_clap_detector.managers.PreferenceManager
import com.base.find_phone_clap_detector.myApplication.MyApplication
import com.base.find_phone_clap_detector.ui.activities.InstructionActivity
import com.base.find_phone_clap_detector.ui.activities.PasscodeActivity
import com.base.find_phone_clap_detector.ui.activities.SelectSoundActivity
import com.base.find_phone_clap_detector.utils.Constants
import com.base.find_phone_clap_detector.utils.DetectorWorkerStarter
import com.base.find_phone_clap_detector.utils.disableMultipleClicking
import com.base.find_phone_clap_detector.utils.visible
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ActivateDetectionFragment : Fragment() {
    private var _binding: FragmentActivateDetectionBinding? = null
    private val binding get() = _binding!!
    private var arrowAnimationJob: Job? = null
    private var isDetectorActive = MyApplication.mInstance.preferenceManager.getBoolean(
        PreferenceManager.Key.isDetectorActive,
        false
    )
    private var isVibrationActive = MyApplication.mInstance.preferenceManager.getBoolean(
        PreferenceManager.Key.isVibrationActive,
        false
    )
    private var isFlashActive = MyApplication.mInstance.preferenceManager.getBoolean(
        PreferenceManager.Key.isFlashActive,
        false
    )

    private val recordAudioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { isGranted ->

            val grantedPermissions = isGranted.filter { it.value }.map { it.key }
            val deniedPermissions = isGranted.filterNot { it.value }.map { it.key }


            if (grantedPermissions.isNotEmpty()) {
                if (grantedPermissions.size == isGranted.size) {
                    selectSoundAndStart()
                }
            }
            if (deniedPermissions.isNotEmpty()) {
                if (deniedPermissions.all {
                        !ActivityCompat.shouldShowRequestPermissionRationale(
                            requireActivity(),
                            it
                        )
                    }
                ) {
                    showSettingsDialog(requireActivity())
                } else {
                    selectSoundAndStart()
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_activate_detection,
            container,
            false
        )
        initListeners()
        return binding.root
    }

    private fun initViews() {

        when (Constants.SERVICE_TYPE) {
            Constants.BY_CLAP -> {
                binding.lottieAnimation.visible()
                binding.gifImgView.gone()
                binding.imageView3.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.state_off_bell
                    )
                )
            }

            Constants.DONT_TOUCH -> {
                binding.lottieAnimation.gone()
                binding.gifImgView.visible()
                Glide.with(this)
                    .load(R.drawable.dont_touch) // can also use a URL
                    .into(binding.gifImgView)
                binding.imageView3.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.dont_touch_icon
                    )
                )
            }

            Constants.POCKET_MODE -> {
                binding.lottieAnimation.gone()
                binding.gifImgView.visible()
                Glide.with(this)
                    .load(R.drawable.pocket) // can also use a URL
                    .into(binding.gifImgView)
                binding.imageView3.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.pocket_icon
                    )
                )
            }

            Constants.BY_WHISTLE -> {
                binding.lottieAnimation.gone()
                binding.gifImgView.visible()
                Glide.with(this)
                    .load(R.drawable.sport_whistle) // can also use a URL
                    .into(binding.gifImgView)
                binding.imageView3.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.whistle_icon
                    )
                )

            }
        }

    }

    private fun initListeners() {
        val shake: Animation =
            AnimationUtils.loadAnimation(requireContext().applicationContext, R.anim.shake)
        arrowAnimationJob?.cancel()
        arrowAnimationJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                _binding?.iconArrow1?.startAnimation(shake)
                delay(4000)
            }
        }

        if (Constants.SERVICE_TYPE == Constants.BY_PASSCODE) binding.changePasscode.visibility =
            View.VISIBLE
        else binding.changePasscode.visibility = View.GONE


        binding.changePasscode.setOnClickListener {
            disableMultipleClicking(it, 1000)
            val intent = Intent(requireContext(), PasscodeActivity::class.java)
            intent.putExtra(Constants.FROM_CHANGE_PASSCODE, true)
            startActivity(intent)
        }
        binding.hintIcon.setOnClickListener {
            disableMultipleClicking(it, 1000)
            requireContext().startActivity(
                Intent(
                    requireContext(),
                    InstructionActivity::class.java
                )
            )
        }

        binding.apply {
            if (isDetectorActive) {
                binding.enableView.visibility = View.VISIBLE
                binding.disableView.visibility = View.INVISIBLE
            } else {
                binding.enableView.visibility = View.INVISIBLE
                binding.disableView.visibility = View.VISIBLE
            }

            if (isVibrationActive) {
                vibrateCard.setCardBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.orangeCardLight
                    )
                )
                vibrateCard.setStrokeColor(
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.orangeCard
                    )
                )
                vibrationCheck.visibility = View.VISIBLE
                vibrationText.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.textColor
                    )
                )
            } else {
                vibrateCard.setCardBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.grayCard
                    )
                )
                vibrateCard.setStrokeColor(
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.cardClr
                    )
                )
                vibrationCheck.visibility = View.INVISIBLE
                vibrationText.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            }

            if (isFlashActive) {
                flashCard.setCardBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.cyanCardLight
                    )
                )
                flashCard.setStrokeColor(
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.cyanCard
                    )
                )
                flashCheck.visibility = View.VISIBLE
                flashText.setTextColor(ContextCompat.getColor(requireContext(), R.color.textColor))
            } else {
                flashCard.setCardBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.grayCard
                    )
                )
                flashCard.setStrokeColor(
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.cardClr
                    )
                )
                flashCheck.visibility = View.INVISIBLE
                flashText.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            }
        }

        binding.enableView.setOnClickListener {
            disableMultipleClicking(it, 1000)

            // Stop service immediately
            stopDetectionService()

            if (binding.enableView.visibility == View.VISIBLE) {
                binding.enableView.visibility = View.INVISIBLE
                binding.disableView.visibility = View.VISIBLE
            }

        }


        binding.disableView.setOnClickListener {
            disableMultipleClicking(it, 1000)
            AnalyticsManager.logEvent("ActivateDetectionFragment.disableView")
            launchPermission()
        }

        binding.vibrateCard.setOnClickListener {
            disableMultipleClicking(it, 1000)
            isVibrationActive = !isVibrationActive
            binding.apply {
                if (isVibrationActive) {
                    vibrateCard.setCardBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.orangeCardLight
                        )
                    )
                    vibrateCard.setStrokeColor(
                        ContextCompat.getColorStateList(
                            requireContext(),
                            R.color.orangeCard
                        )
                    )
                    vibrationCheck.visibility = View.VISIBLE
                    vibrationText.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.textColor
                        )
                    )
                    MyApplication.mInstance.preferenceManager.put(
                        PreferenceManager.Key.isVibrationActive,
                        true
                    )
                    restartService()
                } else {
                    vibrateCard.setCardBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.grayCard
                        )
                    )
                    vibrateCard.setStrokeColor(
                        ContextCompat.getColorStateList(
                            requireContext(),
                            R.color.cardClr
                        )
                    )
                    vibrationCheck.visibility = View.INVISIBLE
                    vibrationText.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.black
                        )
                    )
                    MyApplication.mInstance.preferenceManager.put(
                        PreferenceManager.Key.isVibrationActive,
                        false
                    )
                    restartService()
                }
            }
        }

        binding.flashCard.setOnClickListener {
            disableMultipleClicking(it, 1000)
            isFlashActive = !isFlashActive
            binding.apply {
                if (isFlashActive) {
                    flashCard.setCardBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.cyanCardLight
                        )
                    )
                    flashCard.setStrokeColor(
                        ContextCompat.getColorStateList(
                            requireContext(),
                            R.color.cyanCard
                        )
                    )
                    flashCheck.visibility = View.VISIBLE
                    flashText.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.textColor
                        )
                    )
                    MyApplication.mInstance.preferenceManager.put(
                        PreferenceManager.Key.isFlashActive,
                        true
                    )
                    restartService()
                } else {
                    flashCard.setCardBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.grayCard
                        )
                    )
                    flashCard.setStrokeColor(
                        ContextCompat.getColorStateList(
                            requireContext(),
                            R.color.cardClr
                        )
                    )
                    flashCheck.visibility = View.INVISIBLE
                    flashText.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                    MyApplication.mInstance.preferenceManager.put(
                        PreferenceManager.Key.isFlashActive,
                        false
                    )
                    restartService()
                }
            }
        }

        binding.addSoundBtn.setOnClickListener {
            disableMultipleClicking(it, 1000)
            startActivity(Intent(requireContext(), SelectSoundActivity::class.java))
        }

    }

    private fun selectSoundAndStart() {
        if (hasRequiredPermissions()) {

            startActivity(Intent(requireContext(), SelectSoundActivity::class.java))

        } else {
            recordAudioPermissionLauncher.launch(getRequiredPermissions())
        }
    }

    override fun onResume() {
        super.onResume()
        initViews()
        syncDetectorUi()

        if (MyApplication.isFromStopClap) {
            MyApplication.isFromStopClap = false
        }
    }

    private fun showAd() {
        MyApplication.mInstance.adsManager.showBanner(
            requireContext(),
            AdSize.LARGE_BANNER,
            binding.adFrame,
            this.getString(R.string.ADMOB_BANNER_V2),
            binding.shimmerLayout
        )
    }

    private fun restartService() {
        val isDetectorActive = MyApplication.mInstance.preferenceManager.getBoolean(
            PreferenceManager.Key.isDetectorActive,
            false
        )
        if (isDetectorActive) {
            startDetectionService()
        } else {
            Log.d(TAG, "restartService: Nothing Happens")
        }
    }

    private fun syncDetectorUi() {
        isDetectorActive = MyApplication.mInstance.preferenceManager.getBoolean(
            PreferenceManager.Key.isDetectorActive,
            false
        )
        if (isDetectorActive) {
            binding.enableView.visibility = View.VISIBLE
            binding.disableView.visibility = View.INVISIBLE
        } else {
            binding.enableView.visibility = View.INVISIBLE
            binding.disableView.visibility = View.VISIBLE
        }
    }

    private fun startDetectionService() {
        DetectorWorkerStarter.requestStart(
            requireContext(),
            "ActivateDetectionFragment.startDetectionService"
        )
    }

    private fun stopDetectionService() {
        MyApplication.mInstance.preferenceManager.put(
            PreferenceManager.Key.isDetectorActive,
            false
        )
        DetectorWorkerStarter.requestStop(
            requireContext(),
            "ActivateDetectionFragment.stopDetectionService",
            sendStopBroadcast = true
        )
    }

    override fun onDestroyView() {
        arrowAnimationJob?.cancel()
        arrowAnimationJob = null
        _binding = null
        super.onDestroyView()
    }

    private fun launchPermission() {
        selectSoundAndStart()
    }

    private fun hasRequiredPermissions(): Boolean {
        return getRequiredPermissions().all {
            ContextCompat.checkSelfPermission(
                requireContext(),
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun getRequiredPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.RECORD_AUDIO
        )
    }

    private fun showSettingsDialog(requireActivity: FragmentActivity) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.setting_dialogue)

        val width = (requireContext().resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

        dialog.findViewById<TextView>(R.id.yes).setOnClickListener {
            openAppSettingsStorage(requireActivity)
            dialog.dismiss()
        }

        dialog.findViewById<ImageView>(R.id.closeBtn).setOnClickListener {
            dialog.dismiss()
        }

        try {
            dialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openAppSettingsStorage(requireActivity: FragmentActivity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", requireActivity.packageName, null)
        intent.data = uri
        requireActivity.startActivity(intent)
    }

    fun showChooseActionDialog(context: Context?) {
        val binding = DialogChooseActionBinding.inflate(LayoutInflater.from(context))

        val dialog = context?.let { MaterialAlertDialogBuilder(it) }
            ?.setView(binding.root)
            ?.setCancelable(true)
            ?.create()

        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.show()

        binding.apply {
            btnUseDefault.setOnClickListener {

                dialog?.dismiss()
            }

            btnSelectSound.setOnClickListener {
                disableMultipleClicking(it)
                startActivity(Intent(requireContext(), SelectSoundActivity::class.java))

                dialog?.dismiss()
            }


        }
    }

}
