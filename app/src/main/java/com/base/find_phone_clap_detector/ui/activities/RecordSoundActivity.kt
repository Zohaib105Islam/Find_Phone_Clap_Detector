package com.base.find_phone_clap_detector.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.base.find_phone_clap_detector.myApplication.MyApplication
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.databinding.ActivityRecordSoundBinding
import com.base.find_phone_clap_detector.managers.AdsManager
import com.base.find_phone_clap_detector.ui.viewModels.RecordingViewModel
import com.base.find_phone_clap_detector.utils.Language
import com.base.find_phone_clap_detector.utils.LocaleHelper

class RecordSoundActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecordSoundBinding
    private lateinit var viewModel: RecordingViewModel

    var filePath = ""
    private val recordAudioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { isGranted ->
            if (isGranted.containsValue(false)) {
                showSettingsDialog(this@RecordSoundActivity)
            } else {
                binding.apply {
                    startBtn.visibility = View.INVISIBLE
                    pauseBtn.visibility = View.VISIBLE
                    playBtn.visibility = View.INVISIBLE
                    wavesImg.visibility = View.INVISIBLE
                    waveAnim.visibility = View.VISIBLE
                    deleteBtn.visibility = View.VISIBLE
                    stopBtn.visibility = View.VISIBLE
                    textStatus.text = getString(R.string.recording_sound_programatically)
                    textRecordingStatus.text = getString(R.string.tap_to_stop_recording_programatically)
                }
                try {
                    viewModel.startRecording()
                }catch (e:Exception){
                    Log.d(TAG, "EXCEPTION: ${e.localizedMessage}")
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply current app locale before view inflation
        LocaleHelper.setAppLocale(this)
        super.onCreate(savedInstanceState)
        binding = ActivityRecordSoundBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(binding.main.id)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initViews()
        initAds()
    }

    private fun initAds() {
        // loadNativeAd
        MyApplication.mInstance.adsManager.loadNativeAd(
            this,
            binding.adFrame,
            AdsManager.NativeAdType.NOMEDIA_MEDIUM,
            this.getString(R.string.ADMOB_NATIVE_WITHOUT_MEDIA_V2),
            binding.shimmerLayout
        )
    }

    private fun initViews() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.appClr)

        viewModel = ViewModelProvider(this)[RecordingViewModel::class.java]
        viewModel.filePath.observe(this){
            filePath = it
        }

        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.startBtn.setOnClickListener {
            launchPermission()
        }

        binding.pauseBtn.setOnClickListener {
            binding.pauseBtn.visibility = View.INVISIBLE
            binding.playBtn.visibility = View.VISIBLE

            try {
                viewModel.pauseRecording()
            }catch (e:Exception){
                Log.d(TAG, "EXCEPTION: ${e.localizedMessage}")
            }
        }

        binding.playBtn.setOnClickListener {
            binding.pauseBtn.visibility = View.VISIBLE
            binding.playBtn.visibility = View.INVISIBLE

            try {
                viewModel.resumeRecording()
            }catch (e:Exception){
                Log.d(TAG, "EXCEPTION: ${e.localizedMessage}")
            }
        }

        binding.stopBtn.setOnClickListener {
            binding.startBtn.visibility = View.VISIBLE

            binding.pauseBtn.visibility = View.INVISIBLE
            binding.playBtn.visibility = View.INVISIBLE

            binding.wavesImg.visibility = View.VISIBLE
            binding.waveAnim.visibility = View.INVISIBLE

            binding.deleteBtn.visibility = View.INVISIBLE
            binding.stopBtn.visibility = View.INVISIBLE

            binding.textStatus.text = getString(R.string.recording_sound_programatically)
            binding.textRecordingStatus.text = getString(R.string.tap_to_stop_recording_programatically)

            try {
                viewModel.stopRecording()
                startActivity(Intent(this@RecordSoundActivity , SaveRecordingActivity::class.java).putExtra("filepath" , filePath))
                finish()
            }catch (e:Exception){
                Log.d(TAG, "EXCEPTION: ${e.localizedMessage}")
            }
        }

        binding.deleteBtn.setOnClickListener {
            binding.startBtn.visibility = View.VISIBLE

            binding.pauseBtn.visibility = View.INVISIBLE
            binding.playBtn.visibility = View.INVISIBLE

            binding.wavesImg.visibility = View.VISIBLE
            binding.waveAnim.visibility = View.INVISIBLE

            binding.deleteBtn.visibility = View.INVISIBLE
            binding.stopBtn.visibility = View.INVISIBLE

            binding.textStatus.text = getString(R.string.record_sound)
            binding.textRecordingStatus.text = getString(R.string.tap_to_start_recording)

            try {
                viewModel.stopRecording()
            }catch (e:Exception){
                Log.d(TAG, "EXCEPTION: ${e.localizedMessage}")
            }
        }

        viewModel.currentTime.observe(this@RecordSoundActivity) {
            binding.timeText.text = formatTime(it)
        }
    }

    private fun formatTime(timeInSeconds: Long): String {
        val hours = timeInSeconds / 3600
        val minutes = (timeInSeconds % 3600) / 60
        val seconds = timeInSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun launchPermission() {
        recordAudioPermissionLauncher.launch(getRequiredPermissions())
    }

    private fun getRequiredPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.RECORD_AUDIO
        )
    }

    private fun showSettingsDialog(requireActivity: FragmentActivity) {
        val dialog = Dialog(requireActivity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.setting_dialogue)

        val width = (requireActivity.resources.displayMetrics.widthPixels * 0.90).toInt()
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
}