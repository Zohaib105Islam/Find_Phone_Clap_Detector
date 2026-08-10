package com.base.find_phone_clap_detector.ui.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.RatingBar
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.databinding.DialogPremiumUpgradeBinding
import com.base.find_phone_clap_detector.databinding.DialogueServiceStartTimerBinding
import com.base.find_phone_clap_detector.databinding.FragmentHomeBinding
import com.base.find_phone_clap_detector.managers.AdsManager
import com.base.find_phone_clap_detector.managers.AnalyticsManager
import com.base.find_phone_clap_detector.managers.PreferenceManager
import com.base.find_phone_clap_detector.managers.PreferenceManager.Key
import com.base.find_phone_clap_detector.myApplication.MyApplication
import com.base.find_phone_clap_detector.ui.activities.ImportSoundActivity
import com.base.find_phone_clap_detector.ui.activities.PremiumScreenActivity
import com.base.find_phone_clap_detector.ui.activities.RecordSoundActivity
import com.base.find_phone_clap_detector.ui.activities.SoundPreviewActivity
import com.base.find_phone_clap_detector.ui.activities.SuccessfullyActivatedActivity
import com.base.find_phone_clap_detector.ui.adapters.SoundsAdapter
import com.base.find_phone_clap_detector.ui.dataClasses.SoundsDataClass
import com.base.find_phone_clap_detector.ui.interfaces.SoundInterface
import com.base.find_phone_clap_detector.utils.AdsCounter
import com.base.find_phone_clap_detector.utils.AdsCounter.isAppPremium
import com.base.find_phone_clap_detector.utils.AudioPermissionUtil
import com.base.find_phone_clap_detector.utils.Constants
import com.base.find_phone_clap_detector.utils.DetectorWorkerStarter
import com.base.find_phone_clap_detector.utils.NotificationPermissionUtil
import com.base.find_phone_clap_detector.utils.RemoteConfigAds
import com.base.find_phone_clap_detector.utils.StoragePermissionUtil
import com.base.find_phone_clap_detector.utils.disableMultipleClicking
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeFragment : Fragment(), SoundInterface {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefs: SharedPreferences

    private var soundsAdapter: SoundsAdapter? = null

    private lateinit var audioPermissionUtil: AudioPermissionUtil
    private lateinit var notificationPermissionUtil: NotificationPermissionUtil
    private lateinit var storagePermissionUtil: StoragePermissionUtil

    private var soundsArrayList: ArrayList<SoundsDataClass> = arrayListOf()

    private var uri: Uri? = null

//    private var isDetectorActive = MyApplication.mInstance.preferenceManager.getBoolean(
//        PreferenceManager.Key.isDetectorActive,
//        false
//    )
    private var isDetectorActive = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        LoadAd()
        initListeners()
        initAdapter()
        // restore the last selected mode instead of forcing the first one
        val restoredTopServiceType = resolveTopServiceType(Constants.SERVICE_TYPE)
        selectTopService(restoredTopServiceType)
        Constants.SERVICE_TYPE = when (restoredTopServiceType) {
            TopServiceType.FIND_PHONE -> Constants.BY_CLAP
            TopServiceType.DONT_TOUCH -> Constants.DONT_TOUCH
            TopServiceType.POCKET_MODE -> Constants.POCKET_MODE
            TopServiceType.BY_WHISTLE -> Constants.BY_WHISTLE
        }
        MyApplication.mInstance.byCreateAudioService = false

        audioPermissionUtil = AudioPermissionUtil(
            AudioPermissionUtil.FromFragment(this)
        )
        notificationPermissionUtil = NotificationPermissionUtil(
            NotificationPermissionUtil.FromFragment(this)
        )
        storagePermissionUtil = StoragePermissionUtil(this)

    }

    private fun LoadAd() {
        Log.d("HomeAdTest", "LoadAd called")
        MyApplication.mInstance.adsManager.loadNativeAd(
            requireActivity(),
            binding.adFrame,
            AdsManager.NativeAdType.MEDIA_SMALL_NEW,
            this.getString(R.string.ADMOB_NATIVE_WITHOUT_MEDIA_HOME_V2),
            binding.shimmerLayout
        )
    }

    override fun onStart() {
        super.onStart()
        prefs =
            requireContext().getSharedPreferences("feature_attempts_prefs", Context.MODE_PRIVATE)
        val attempts = prefs.getInt("dont_touch_attempts", 1)
        updateFreeTrialIconsDontTouch(attempts)
        val attemptss = prefs.getInt("pocket_mode_attempts", 1)
        updateFreeTrialIconsPocket(attemptss)
        syncDetectorUi(resetSelectionWhenInactive = false)
    }

    private fun initListeners() {

        binding.findPhoneCard.setOnClickListener {
            disableMultipleClicking(it, 1000)
            selectTopService(TopServiceType.FIND_PHONE)
            MyApplication.mInstance.byCreateAudioService = false
            Constants.SERVICE_TYPE = Constants.BY_CLAP
            //  (activity as MainActivity).openServiceActivationScreen(R.string.find_your_phone_by_clap)
        }

        binding.dontTouch.setOnClickListener {
            disableMultipleClicking(it, 1000)
            MyApplication.mInstance.byCreateAudioService = false

            if (isAppPremium()) {
                Constants.SERVICE_TYPE = Constants.DONT_TOUCH
                selectTopService(TopServiceType.DONT_TOUCH)
            } else {
                handleDontTouchClick()
            }
        }

        binding.pocketMode.setOnClickListener {
            disableMultipleClicking(it, 1000)
            MyApplication.mInstance.byCreateAudioService = false

            if (isAppPremium()) {
                selectTopService(TopServiceType.POCKET_MODE)
                Constants.SERVICE_TYPE = Constants.POCKET_MODE
            } else {
                handlePockeMode()
            }
        }

        binding.byWhistle.setOnClickListener {
            disableMultipleClicking(it, 1000)
            selectTopService(TopServiceType.BY_WHISTLE)
            MyApplication.mInstance.byCreateAudioService = false
            Constants.SERVICE_TYPE = Constants.BY_WHISTLE
//            (activity as MainActivity).openServiceActivationScreen(
//                R.string.whistlemode
//            )
        }


        binding.createSoundBtn.setOnClickListener {
            disableMultipleClicking(it, 1000)
            AnalyticsManager.logEvent("FA_add_sound")
            audioPermissionUtil.checkAndRequest {
                MyApplication.mInstance.byCreateAudioService = true
                startActivity(Intent(requireActivity(), RecordSoundActivity::class.java))
            }
        }

        binding.importSoundsBtn.setOnClickListener {
            disableMultipleClicking(it, 1000)
            AnalyticsManager.logEvent("FA_import_sound")
            storagePermissionUtil.checkAndRequestPermissions {
                MyApplication.mInstance.byCreateAudioService = true
                startActivity(Intent(requireActivity(), ImportSoundActivity::class.java))
            }
        }

        setDetectorUiState(isDetectorActive, persist = false)
//        binding.apply {
//            if (isDetectorActive) {
//                binding.enableView.visibility = View.VISIBLE
//                binding.enableViewHand.visibility = View.VISIBLE
//                binding.disableView.visibility = View.INVISIBLE
//            } else {
//                binding.enableView.visibility = View.INVISIBLE
//                binding.enableViewHand.visibility = View.INVISIBLE
//                binding.disableView.visibility = View.VISIBLE
//            }
//        }

        binding.enableView.setOnClickListener {
            disableMultipleClicking(it, 1000)
            stopDetectionService()
            setDetectorUiState(false, persist = false) // stopDetectionService already persists the pref
            selectTopService(TopServiceType.FIND_PHONE)
        }

        binding.disableView.setOnClickListener {
            disableMultipleClicking(it, 1000)
            AnalyticsManager.logEvent("FA_service_start")

            audioPermissionUtil.checkAndRequest {
                notificationPermissionUtil.checkAndRequest {
                    if (isAppPremium()) {
                        handleApplySoundLogic()
                    } else {
                        //  Permission granted → Go Next
                        showServiceStartCountDialog()
                    }
                }
            }
        }

    }

    fun showServiceStartCountDialog() {
        val dialogBinding = DialogueServiceStartTimerBinding.inflate(
            LayoutInflater.from(requireActivity())
        )

        val alertDialog = AlertDialog.Builder(requireActivity())
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()

        val countDownTimer = object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt() + 1
                dialogBinding.tvTimer.text = secondsLeft.toString()
            }

            override fun onFinish() {
                if (_binding == null || !isAdded) return
                if (alertDialog.isShowing) {
                    alertDialog.dismiss()

                    if (!RemoteConfigAds.shouldShowAd(RemoteConfigAds.SERVICE_START)) {
                        if (Constants.SERVICE_TYPE == Constants.POCKET_MODE) {
                            prefs.edit { putInt("pocket_mode_attempts", 0) }
                        }
                        if (Constants.SERVICE_TYPE == Constants.DONT_TOUCH) {
                            prefs.edit { putInt("dont_touch_attempts", 0) }
                        }
                        binding.tvTapToActivate.text = getString(R.string.tap_to_deactivate)
                        handleApplySoundLogic()

                        return
                    }

                    MyApplication.mInstance.adsManager.loadInterstitialAd(
                        requireActivity(),
                        adId = getString(R.string.ADMOB_INTERSTITIAL_V2_SERVICE)
                    ) {
                        if (_binding == null || !isAdded) return@loadInterstitialAd
                        if (Constants.SERVICE_TYPE == Constants.POCKET_MODE) {
                            prefs.edit { putInt("pocket_mode_attempts", 0) }
                        }
                        if (Constants.SERVICE_TYPE == Constants.DONT_TOUCH) {
                            prefs.edit { putInt("dont_touch_attempts", 0) }
                        }
                        binding.tvTapToActivate.text = getString(R.string.tap_to_deactivate)
                        handleApplySoundLogic()
                    }
                }
            }
        }

        dialogBinding.tvTimer.text = "5"
        countDownTimer.start()

        fun closeDialog() {
            countDownTimer.cancel()
            if (alertDialog.isShowing) {
                alertDialog.dismiss()
            }
        }

        dialogBinding.btnCancel.setOnClickListener {
            closeDialog()
        }

        dialogBinding.cross.setOnClickListener {
            closeDialog()
        }

        alertDialog.setOnDismissListener {
            countDownTimer.cancel()
        }
    }

    private fun initAdapter() {

        soundsArrayList.clear()

        val savedUri = MyApplication.mInstance.preferenceManager
            .getString(PreferenceManager.Key.appliedSoundUri, null)

        // ---- ADD LOCAL SOUNDS ---- //
        soundsArrayList.add(
            SoundsDataClass(
                getString(R.string.cat_meow),
                R.drawable.ic_cat_meo,
                true,
                "android.resource://${requireActivity().packageName}/${R.raw.cat_sound}".toUri()
                    .toString()
            )
        )

        soundsArrayList.add(
            SoundsDataClass(
                getString(R.string.dog_barking),
                R.drawable.ic_dog_bark,
                true,
                "android.resource://${requireActivity().packageName}/${R.raw.dog_sound}".toUri()
                    .toString()
            )
        )

        soundsArrayList.add(
            SoundsDataClass(
                getString(R.string.say_he),
                R.drawable.ic_say_hey,
                false,
                "android.resource://${requireActivity().packageName}/${R.raw.hey_sound}".toUri()
                    .toString()
            )
        )

        soundsArrayList.add(
            SoundsDataClass(
                getString(R.string.whistlee),
                R.drawable.ic_whistle2,
                false,
                "android.resource://${requireActivity().packageName}/${R.raw.whistle_sound}".toUri()
                    .toString()
            )
        )

        soundsArrayList.add(
            SoundsDataClass(
                getString(R.string.door_bell),
                R.drawable.ic_door_bell,
                false,
                "android.resource://${requireActivity().packageName}/${R.raw.doorbell_sound}".toUri()
                    .toString()
            )
        )

        soundsArrayList.add(
            SoundsDataClass(
                getString(R.string.car_horn),
                R.drawable.ic_car_horn,
                false,
                "android.resource://${requireActivity().packageName}/${R.raw.car_horn_sound}".toUri()
                    .toString()
            )
        )

        soundsArrayList.add(
            SoundsDataClass(
                getString(R.string.hello),
                R.drawable.ic_robot,
                false,
                "android.resource://${requireActivity().packageName}/${R.raw.hello_sound}".toUri()
                    .toString()
            )
        )

        soundsArrayList.add(
            SoundsDataClass(
                getString(R.string.party_horn),
                R.drawable.ic_party_horn,
                false,
                "android.resource://${requireActivity().packageName}/${R.raw.party_sound}".toUri()
                    .toString()
            )
        )

        soundsArrayList.add(
            SoundsDataClass(
                getString(R.string.police_horn),
                R.drawable.ic_police_whistle,
                false,
                "android.resource://${requireActivity().packageName}/${R.raw.police_sound}".toUri()
                    .toString()
            )
        )

        // ---- FIND SAVED POSITION ---- //
        var selectedPosition = soundsArrayList.indexOfFirst {
            it.audioUri == savedUri
        }

        if (selectedPosition == -1) {
            selectedPosition = 0
        }

        // ---- SET SELECTED URI ---- //
        uri = soundsArrayList[selectedPosition].audioUri.toUri()

        binding.soundsRv.apply {
            setHasFixedSize(true)
//            setItemViewCacheSize(4)
            layoutManager = GridLayoutManager(requireActivity(), 3)

            soundsAdapter = SoundsAdapter(
                requireActivity(),
                soundsArrayList,
                selectedPosition,
                this@HomeFragment
            )

            adapter = soundsAdapter
        }
    }

    override fun clickOnSoundListener(
        position: Int,
        premium: Boolean,
        img: Int,
        title: String,
        audioUri: String
    ) {

        // UPDATE SELECTED URI
        uri = Uri.parse(audioUri)

        val safeTitle = sanitizeTitleForEvent(title)
        AnalyticsManager.logEvent("FA_${safeTitle}_sound")

        if (audioUri.isNullOrEmpty()) {
            return
        }
        if (audioUri.isNullOrEmpty()) {
            return
        }
        val intent = Intent(requireActivity(), SoundPreviewActivity::class.java)
        intent.putExtra("img", img)
        intent.putExtra("position", position)
        intent.putExtra("title", title)
        intent.putExtra("uri", audioUri.toString())
        Log.d(ContentValues.TAG, "uriStringCheck set: $audioUri")
        startActivity(intent)
        return
    }

    fun sanitizeTitleForEvent(title: String): String {
        val replaced = title.replace(" ", "_")
        return replaced.replace(Regex("[^A-Za-z0-9_]"), "")
    }

    override fun onDeleteSound(position: Int, sound: SoundsDataClass) {
        TODO("Not yet implemented")
    }

    private fun handleApplySoundLogic() {

        // Save preferences based on toggle state
        val isLoop = MyApplication.mInstance.preferenceManager
            .getBoolean(PreferenceManager.Key.checkLoop, true)

        if (isLoop) {
            MyApplication.mInstance.preferenceManager.put(
                PreferenceManager.Key.soundPlayTime,
                -1
            )
        } else {
            val selectedTime = MyApplication.mInstance.preferenceManager
                .getInt(PreferenceManager.Key.soundPlayTime, 45000)

            MyApplication.mInstance.preferenceManager.put(
                PreferenceManager.Key.soundPlayTime,
                selectedTime
            )
        }

        // Proceed with activation and ad flow
        MyApplication.isFromMain = false

        if (MyApplication.mInstance.byCreateAudioService) {
            Log.d("MainActivityTest", "Sound Preview byCreate Audio")
            Constants.SERVICE_TYPE = Constants.BY_CLAP

            MyApplication.mInstance.byCreateAudioServiceActivated = true
            MyApplication.mInstance.byCreateAudioService = false

            MyApplication.mInstance.preferenceManager.put(
                PreferenceManager.Key.appliedSoundUri,
                uri.toString()
            )
            setDetectorUiState(true)
            startActivity(Intent(requireActivity(), SuccessfullyActivatedActivity::class.java))
            Toast.makeText(requireActivity(), "Service Activated", Toast.LENGTH_SHORT).show()

            restartService()
        } else {
            MyApplication.mInstance.preferenceManager.put(
                PreferenceManager.Key.appliedSoundUri,
                uri.toString()
            )
            setDetectorUiState(true)
            startActivity(Intent(requireActivity(), SuccessfullyActivatedActivity::class.java))
            Toast.makeText(requireActivity(), "Service Activated", Toast.LENGTH_SHORT).show()
            restartService()
        }
    }

    private fun setDetectorUiState(active: Boolean, persist: Boolean = true) {
        isDetectorActive = active
        if (persist) {
            MyApplication.mInstance.preferenceManager.put(
                PreferenceManager.Key.isDetectorActive,
                active
            )
        }
        binding.enableView.visibility = if (active) View.VISIBLE else View.INVISIBLE
        binding.enableViewHand.visibility = if (active) View.VISIBLE else View.INVISIBLE
        binding.disableView.visibility = if (active) View.INVISIBLE else View.VISIBLE
        binding.tvTapToActivate.text =
            getString(if (active) R.string.tap_to_deactivate else R.string.tap_to_activate)
    }

    private fun restartService() {
        val isDetectorActive = MyApplication.mInstance.preferenceManager.getBoolean(
            PreferenceManager.Key.isDetectorActive,
            false
        )
        if (isDetectorActive) {
            startDetectionService()
        } else {
            MyApplication.mInstance.preferenceManager.put(
                PreferenceManager.Key.isDetectorActive,
                true
            )
            startDetectionService()
        }
    }


    override fun onResume() {
        super.onResume()
        syncDetectorUi()
        updateFreeTrialIconsDontTouch(prefs.getInt("dont_touch_attempts", 1))
        updateFreeTrialIconsPocket(prefs.getInt("pocket_mode_attempts", 1))

        viewLifecycleOwner.lifecycleScope.launch {
            delay(100)

            val prefs = MyApplication.mInstance.preferenceManager
            val skipPremium = prefs.getBoolean(Key.SKIP_PREMIUM, false)

            // If user is already premium → no need to show anything
            if (isAppPremium()) {
                return@launch
            }

            // If skipPremium flag is TRUE (after language change)
            if (skipPremium) {
                Log.d("MainActivity", "Skipping premium screen after language change")

                MyApplication.isFirstPremium = false

                // Clear skip flag for next time
                prefs.put(Key.SKIP_PREMIUM, false)

                return@launch
            }

            // Normal premium showing logic
            if (AdsCounter.showPremiumScreen()) {
                AdsCounter.proCounter = 0
                Log.d("MainActivity", "Showing premium screen normally")
                startActivity(Intent(requireActivity(), PremiumScreenActivity::class.java))
            }
        }

        // Rating logic (unchanged)
        if (AdsCounter.isShowRatting()) {
            if (!(MyApplication.mInstance?.preferenceManager?.getBoolean(
                    PreferenceManager.Key.SHOW_RATING_DIALOG
                ) ?: false)
            ) {
                showRatingDialogue()
            }
        }

        // ---- SELECT FIRST SOUND ---- //
        if (soundsArrayList.isNotEmpty()) {

            val firstSound = soundsArrayList[0]

            uri = Uri.parse(firstSound.audioUri)

            binding.root.clearFocus()
            requireActivity().currentFocus?.clearFocus()
            binding.soundsRv.post {
                try {
                    soundsAdapter?.updateSelection(0)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    }

    override fun onPause() {
        binding.root.clearFocus()
        binding.soundsRv.clearFocus()
        requireActivity().currentFocus?.clearFocus()
        super.onPause()
    }

    private fun syncDetectorUi(resetSelectionWhenInactive: Boolean = false) {
        val prefActive = MyApplication.mInstance.preferenceManager.getBoolean(
            PreferenceManager.Key.isDetectorActive,
            false
        )
        val actuallyRunning = DetectorWorkerStarter.isDetectorRunning()
        val active = prefActive && actuallyRunning

        setDetectorUiState(active, persist = (prefActive != active))

        if (!active && resetSelectionWhenInactive) {
            selectTopService(TopServiceType.FIND_PHONE)
        }
    }

    private fun startDetectionService() {
        DetectorWorkerStarter.requestStart(
            requireContext(),
            "HomeFragment.startDetectionService"
        )
    }

    private fun stopDetectionService() {
        MyApplication.mInstance.preferenceManager.put(
            PreferenceManager.Key.isDetectorActive,
            false
        )
        DetectorWorkerStarter.requestStop(
            requireContext(),
            "HomeFragment.stopDetectionService",
            sendStopBroadcast = true
        )
    }

    private fun handleDontTouchClick() {
        val attempts = prefs.getInt("dont_touch_attempts", 1)

        if (attempts > 0) {
            // Free trial available
            Constants.SERVICE_TYPE = Constants.DONT_TOUCH
            selectTopService(TopServiceType.DONT_TOUCH)

            // Save that user used the trial
         //   prefs.edit().putInt("dont_touch_attempts", 0).apply()

            // Update icons after trial used
         //   updateFreeTrialIconsDontTouch(0)
        } else {
            // No trial left → Show Premium Dialog
            showPremiumDialog()
        }
    }

    private fun handlePockeMode() {
        val attempts = prefs.getInt("pocket_mode_attempts", 1)

        if (attempts > 0) {

            selectTopService(TopServiceType.POCKET_MODE)
            Constants.SERVICE_TYPE = Constants.POCKET_MODE

            // Save attempt used
          //  prefs.edit().putInt("pocket_mode_attempts", 0).apply()
          //  updateFreeTrialIconsPocket(0)
        } else {
            showPremiumDialog()
        }
    }

    private fun updateFreeTrialIconsDontTouch(attempts: Int) {
        Log.d("FreeTrailTest", "Dont Touch Mode attempts: $attempts")

        if (isAppPremium()) {
            binding.clDontTouchProCard.visibility = View.GONE
        } else {
            binding.clDontTouchProCard.visibility = View.VISIBLE
            binding.tvDontTouchAttempts.text = "$attempts ${getString(R.string.free_trail)}"
        }
    }

    private fun updateFreeTrialIconsPocket(attempt: Int) {
        Log.d("FreeTrailTest", "Pocket Mode attempts: $attempt")
        if (isAppPremium()) {
            binding.clPocketModeProCard.visibility = View.GONE
        } else {
            binding.clPocketModeProCard.visibility = View.VISIBLE
            binding.tvPocketModeAttempts.text = "$attempt ${getString(R.string.free_trail)}"
        }
    }


    private fun selectTopService(type: TopServiceType) {

        // Reset All First (Unselect Everything)
        resetAllTopServices()

        when (type) {

            TopServiceType.FIND_PHONE -> {
                binding.bgFindPhone.setBackgroundResource(R.drawable.ic_selected_card)
                binding.tvFindPhone.setTextColor(requireContext().getColor(R.color.white))
            }

            TopServiceType.DONT_TOUCH -> {
                binding.bgDontTouch.setBackgroundResource(R.drawable.ic_selected_card)
                binding.tvDontTouch.setTextColor(requireContext().getColor(R.color.white))
            }

            TopServiceType.POCKET_MODE -> {
                binding.bgPocketMode.setBackgroundResource(R.drawable.ic_selected_card)
                binding.tvPocketMode.setTextColor(requireContext().getColor(R.color.white))
            }

            TopServiceType.BY_WHISTLE -> {
                binding.bgByWhistle.setBackgroundResource(R.drawable.ic_selected_card)
                binding.tvByWhistle.setTextColor(requireContext().getColor(R.color.white))
            }
        }
    }

    private fun resolveTopServiceType(serviceType: String): TopServiceType {
        return when (serviceType) {
            Constants.DONT_TOUCH -> TopServiceType.DONT_TOUCH
            Constants.POCKET_MODE -> TopServiceType.POCKET_MODE
            Constants.BY_WHISTLE -> TopServiceType.BY_WHISTLE
            else -> TopServiceType.FIND_PHONE
        }
    }

    private fun resetAllTopServices() {

        // Reset Backgrounds
        binding.bgFindPhone.setBackgroundResource(R.drawable.ic_unselected_card)
        binding.bgDontTouch.setBackgroundResource(R.drawable.ic_unselected_card)
        binding.bgPocketMode.setBackgroundResource(R.drawable.ic_unselected_card)
        binding.bgByWhistle.setBackgroundResource(R.drawable.ic_unselected_card)

        // Reset Text Colors
        val grayColor = requireContext().getColor(R.color.gray_text)

        binding.tvFindPhone.setTextColor(grayColor)
        binding.tvDontTouch.setTextColor(grayColor)
        binding.tvPocketMode.setTextColor(grayColor)
        binding.tvByWhistle.setTextColor(grayColor)

    }

    private fun showPremiumDialog() {
        val dialogBinding = DialogPremiumUpgradeBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireActivity())
            .setView(dialogBinding.root)
            .create()

        // Make background transparent
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Optional: dim background behind dialog
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog.window?.setDimAmount(0.8f) // 0.0 = no dim, 1.0 = fully dark

        dialogBinding.btnUpgrade.setOnClickListener {
            disableMultipleClicking(it, 1000)
            dialog.dismiss()
            val intent = Intent(requireActivity(), PremiumScreenActivity::class.java)
            startActivity(intent)
        }

        dialogBinding.btnLater.setOnClickListener {
            disableMultipleClicking(it, 1000)
            dialog.dismiss()
        }

        dialog.show()

        // Set width to 90% of screen
        val window = dialog.window
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(window?.attributes)
        val displayMetrics = resources.displayMetrics
        layoutParams.width = (displayMetrics.widthPixels * 0.9).toInt()
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        window?.attributes = layoutParams
    }


    private fun showRatingDialogue() {
        if (!isAdded || context == null) return  // Fragment not attached, abort

        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.ratting_dialogue)
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        dialog.findViewById<View>(R.id.cancel).setOnClickListener {
            disableMultipleClicking(it, 1000)
            dialog.dismiss()
        }
        dialog.findViewById<View>(R.id.submit).setOnClickListener {
            disableMultipleClicking(it, 1000)
            if (isAdded) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.thanks_message),
                    Toast.LENGTH_SHORT
                ).show()
            }
            dialog.dismiss()
        }

        val simpleRatingBar: RatingBar = dialog.findViewById(R.id.rattingBar)
        simpleRatingBar.onRatingBarChangeListener =
            RatingBar.OnRatingBarChangeListener { _, rating, _ ->
                if (rating >= 4 && isAdded) {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=com.phonefinder.findmyphone.clapflash")
                    )
                    MyApplication.mInstance.preferenceManager.put(
                        PreferenceManager.Key.SHOW_RATING_DIALOG,
                        true
                    )
                    startActivity(intent)
                    dialog.dismiss()
                }
            }

        try {
            if (isAdded) dialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        binding.root.clearFocus()
        binding.soundsRv.clearFocus()
        requireActivity().currentFocus?.clearFocus()
        soundsAdapter = null
        _binding = null
        super.onDestroyView()
    }

}


enum class TopServiceType {
    FIND_PHONE,
    DONT_TOUCH,
    POCKET_MODE,
    BY_WHISTLE
}
