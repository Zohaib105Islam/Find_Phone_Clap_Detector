package com.base.find_phone_clap_detector.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.databinding.ActivityPasscodeBinding
import com.base.find_phone_clap_detector.managers.AdsManager
import com.base.find_phone_clap_detector.managers.PreferenceManager
import com.base.find_phone_clap_detector.managers.PreferenceManager.Key
import com.base.find_phone_clap_detector.myApplication.MyApplication
import com.base.find_phone_clap_detector.utils.AdsCounter
import com.base.find_phone_clap_detector.utils.Constants
import com.base.find_phone_clap_detector.utils.disableMultipleClicking

class PasscodeActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityPasscodeBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initView()
        initListener()
        initAds()
    }

    private fun initView() {
        if (intent.getBooleanExtra(Constants.FROM_CHANGE_PASSCODE, false)){
            binding.textLay.visibility = View.VISIBLE
            binding.recordedText.text = MyApplication.mInstance.preferenceManager.getString(Key.PASSCODE_STRING)
        }else binding.textLay.visibility = View.GONE
    }

    private fun initListener() {

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.recordVoice.setOnClickListener {
            startActivity(Intent(this, CreatePasscodeVoiceActivity::class.java))
            finish()
        }

        binding.voiceToText.setOnClickListener {
            startActivity(Intent(this, CreatePasscodeTextActivity::class.java))
            finish()
        }

        binding.settingsIcon.setOnClickListener {
            disableMultipleClicking(it, 1000)
//            if (AdsCounter.showSettingsAd()) {
//                MyApplication.mInstance.adsManager.loadInterstitialAd(this, false) {
//                    startActivity(Intent(this, SettingsActivity::class.java))
//                }
//            } else {
                startActivity(Intent(this, SettingsActivity::class.java))
//            }
        }

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

}
