package com.base.find_phone_clap_detector.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.base.find_phone_clap_detector.databinding.ActivityStopSoundBinding
import com.base.find_phone_clap_detector.managers.PreferenceManager
import com.base.find_phone_clap_detector.myApplication.MyApplication
import com.base.find_phone_clap_detector.utils.DetectorWorkerStarter
import com.base.find_phone_clap_detector.utils.LocaleHelper
import com.base.find_phone_clap_detector.utils.disableMultipleClicking
import com.horse.identification.extensions.setSafeOnClickListener
import com.ncorti.slidetoact.SlideToActView

class StopSoundActivity : AppCompatActivity(), SlideToActView.OnSlideCompleteListener {
    private val tag = "StopSoundActivity"

    private lateinit var binding: ActivityStopSoundBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply current app locale before view inflation
        LocaleHelper.setAppLocale(this)
        super.onCreate(savedInstanceState)
        binding = ActivityStopSoundBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.sliderBtn.onSlideCompleteListener = this

        initViews()
    }

    private fun initViews() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        binding.stopServiceBtn.setSafeOnClickListener {
            disableMultipleClicking(it, 1000)
            // Stop the service
            DetectorWorkerStarter.requestStop(
                this,
                "StopSoundActivity.stopServiceBtn",
                sendStopBroadcast = true
            )

            MyApplication.mInstance.preferenceManager.put(
                PreferenceManager.Key.isDetectorActive,
                false
            )
            MyApplication.mInstance.preferenceManager.put(
                PreferenceManager.Key.isClapDetected,
                false
            )
            startActivity(Intent(this@StopSoundActivity, MainActivity::class.java))
            finish()
        }
    }

    override fun onSlideComplete(view: SlideToActView) {
        DetectorWorkerStarter.requestStop(
            this,
            "StopSoundActivity.onSlideComplete",
            sendStopBroadcast = true
        )

        MyApplication.mInstance.preferenceManager.put(PreferenceManager.Key.isClapDetected, false)
        MyApplication.mInstance.preferenceManager.put(PreferenceManager.Key.isDetectorActive, true)
        MyApplication.shouldRestartDetectorFromStopSound = true

        startActivity(Intent(this@StopSoundActivity, MainActivity::class.java))
        finish()
    }

    override fun onStart() {
        super.onStart()
        MyApplication.isFromStopClap = true
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.d(tag, "Stop Sound Activity is destroyed.")
    }
}
