package com.base.find_phone_clap_detector.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.horse.identification.extensions.setSafeOnClickListener
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.databinding.ActivityStopSoundAvtivityBinding
import com.base.find_phone_clap_detector.managers.AdsManager
import com.base.find_phone_clap_detector.managers.PreferenceManager
import com.base.find_phone_clap_detector.myApplication.MyApplication
import com.base.find_phone_clap_detector.utils.DetectorWorkerStarter
import com.base.find_phone_clap_detector.utils.LocaleHelper
import com.base.find_phone_clap_detector.utils.RemoteConfigAds
import com.base.find_phone_clap_detector.utils.disableMultipleClicking
import com.ncorti.slidetoact.SlideToActView

class StopSoundAvtivity : AppCompatActivity(), SlideToActView.OnSlideCompleteListener {
    private val TAG = "StopSoundAvtivity"

    private lateinit var binding: ActivityStopSoundAvtivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply current app locale before view inflation
        LocaleHelper.setAppLocale(this)
        super.onCreate(savedInstanceState)
        binding = ActivityStopSoundAvtivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(binding.main.id)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.sliderBtn.onSlideCompleteListener = this

        initAds()
        initViews()
    }

    private fun initViews() {
        if (!MyApplication.mInstance.preferenceManager.getBoolean(
                PreferenceManager.Key.isDarkTheme,
                false
            )
        ) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        binding.stopServiceBtn.setSafeOnClickListener {
            disableMultipleClicking(it,1000)
            // Stop the service
            DetectorWorkerStarter.requestStop(
                this,
                "StopSoundAvtivity.stopServiceBtn",
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
            startActivity(Intent(this@StopSoundAvtivity, MainActivity::class.java))
            finish()
//            MyApplication.mInstance.adsManager.loadInterstitialAd(this@StopSoundAvtivity) {
//                startActivity(Intent(this@StopSoundAvtivity, MainActivity::class.java))
//                finish()
//            }
        }
    }

    override fun onSlideComplete(view: SlideToActView) {
        DetectorWorkerStarter.requestStop(
            this,
            "StopSoundAvtivity.onSlideComplete",
            sendStopBroadcast = true
        )

        MyApplication.mInstance.preferenceManager.put(PreferenceManager.Key.isClapDetected, false)



        if (!RemoteConfigAds.shouldShowAd(RemoteConfigAds.SERVICE_STOP)) {
            MyApplication.mInstance.preferenceManager.put(PreferenceManager.Key.isDetectorActive, true)
            MyApplication.shouldRestartDetectorFromStopSound = true

            startActivity(Intent(this@StopSoundAvtivity, MainActivity::class.java))
            finish()

            return
        }

        MyApplication.mInstance.adsManager.loadInterstitialAd(this@StopSoundAvtivity) {

            MyApplication.mInstance.preferenceManager.put(PreferenceManager.Key.isDetectorActive, true)
            MyApplication.shouldRestartDetectorFromStopSound = true

            startActivity(Intent(this@StopSoundAvtivity, MainActivity::class.java))
            finish()
        }
    }

    private fun initAds() {
        // loadNativeAd
        MyApplication.mInstance.adsManager.loadNativeAd(
            this,
            binding.adFrame,
            AdsManager.NativeAdType.MEDIUM_TYPE,
            this.getString(R.string.ADMOB_NATIVE_WITH_MEDIA_V2),
            binding.shimmerLayout
        )
    }

    override fun onStart() {
        super.onStart()
        MyApplication.isFromStopClap = true
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.d(TAG, "Stop Sound Activity is destroyed.")
    }

}
