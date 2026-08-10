package com.base.find_phone_clap_detector.ui.activities

import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.horse.identification.extensions.setSafeOnClickListener
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.databinding.ActivityMainBinding
import com.base.find_phone_clap_detector.managers.AnalyticsManager
import com.base.find_phone_clap_detector.managers.GoogleMobileAdsConsentManager
import com.base.find_phone_clap_detector.managers.PreferenceManager
import com.base.find_phone_clap_detector.managers.PreferenceManager.Key
import com.base.find_phone_clap_detector.myApplication.MyApplication
import com.base.find_phone_clap_detector.ui.adapters.HomeViewPagerAdapter
import com.base.find_phone_clap_detector.utils.CallUITrigger
import com.base.find_phone_clap_detector.utils.DetectorWorkerStarter
import com.base.find_phone_clap_detector.utils.LocaleHelper
import com.base.find_phone_clap_detector.utils.Utils
import com.base.find_phone_clap_detector.utils.disableMultipleClicking
import com.base.find_phone_clap_detector.utils.gone
import com.base.find_phone_clap_detector.utils.visible
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: HomeViewPagerAdapter
    private val TAG = "MainActivity"
    private var isDarkTheme = MyApplication.mInstance.preferenceManager.getBoolean(
        PreferenceManager.Key.isDarkTheme,
        false
    )


    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    lateinit var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager

    private var isClapDialogShowing = false


    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply current app locale before view inflation
        LocaleHelper.setAppLocale(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        CallUITrigger.shouldDialog.value = false
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(binding.main.id)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (!MyApplication.mInstance.preferenceManager.getBoolean(
                PreferenceManager.Key.isDarkTheme,
                false
            )
        ) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        initViews()
        showConsent()
        navigationBar()
    }

    private fun initViews() {
        if (isDarkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        val shakeAnimator = ObjectAnimator.ofFloat(
            binding.claimNowBtn,
            "translationX",
            0f,
            25f,
            -25f,
            20f,
            -20f,
            15f,
            -15f,
            10f,
            -10f,
            5f,
            -5f,
            0f
        )
        shakeAnimator.duration = 1500
        shakeAnimator.repeatCount = ObjectAnimator.INFINITE
        shakeAnimator.repeatMode = ObjectAnimator.RESTART
        shakeAnimator.start()

        binding.claimNowBtn.setOnClickListener {
            startActivity(Intent(this@MainActivity, PremiumFreeTrailActivity::class.java))
        }

        CallUITrigger.shouldDialog.observe(this) { shouldShow ->
            if (shouldShow == true && !isClapDialogShowing) {
                if (MyApplication.isFromMain) {
                    showDilogClapDetected()
                } else {
                    MyApplication.isFromMain = true
                    Log.d(TAG, "initViews: Nothing Happens1")
                }
            } else {
                Log.d(TAG, "initViews: Nothing Happens2 or Dialog Already Showing")
            }
        }

        binding.settingsIcon.setOnClickListener {
            disableMultipleClicking(it, 1000)
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
        }

        binding.themeIcon.setOnClickListener {
            disableMultipleClicking(it, 1000)
            if (MyApplication.mInstance.preferenceManager.getBoolean(PreferenceManager.Key.IS_APP_PREMIUM)) {
                val currentNightMode =
                    resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                val isDarkMode = if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                    MyApplication.mInstance.preferenceManager.put(
                        PreferenceManager.Key.isDarkTheme,
                        false
                    )
                    false
                } else {
                    MyApplication.mInstance.preferenceManager.put(
                        PreferenceManager.Key.isDarkTheme,
                        true
                    )
                    true
                }
                val newNightMode = if (isDarkMode) {
                    AppCompatDelegate.MODE_NIGHT_YES
                } else {
                    AppCompatDelegate.MODE_NIGHT_NO
                }

                AppCompatDelegate.setDefaultNightMode(newNightMode)
                recreate()
            } else {
                Utils.watchAdOrBuyPremium(
                    this@MainActivity,
                    onBuyPremium = {
                        startActivity(Intent(this@MainActivity, PremiumScreenActivity::class.java))
                    }) {
                    val currentNightMode =
                        resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                    val isDarkMode = if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                        MyApplication.mInstance.preferenceManager.put(
                            PreferenceManager.Key.isDarkTheme,
                            false
                        )
                        false
                    } else {
                        MyApplication.mInstance.preferenceManager.put(
                            PreferenceManager.Key.isDarkTheme,
                            true
                        )
                        true
                    }
                    val newNightMode = if (isDarkMode) {
                        AppCompatDelegate.MODE_NIGHT_YES
                    } else {
                        AppCompatDelegate.MODE_NIGHT_NO
                    }

                    AppCompatDelegate.setDefaultNightMode(newNightMode)
                    recreate()
                }
            }

        }

        if (MyApplication.mInstance.preferenceManager.getBoolean(PreferenceManager.Key.IS_APP_ADS_FREE, false)) {
            binding.premiumIcon.visibility = View.GONE
        }else {
            binding.premiumIcon.visibility = View.VISIBLE
        }
        binding.premiumIcon.setOnClickListener {
            disableMultipleClicking(it, 1000)
            AnalyticsManager.logEvent("FA_home_premium_icon")
            startActivity(Intent(this@MainActivity, PremiumScreenActivity::class.java))
        }
    }

    private fun showDilogClapDetected() {
        if (isClapDialogShowing) return
        isClapDialogShowing = true

        val dialog = Dialog(this@MainActivity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.detection_dialogue)

        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

        dialog.findViewById<TextView>(R.id.yes).setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<ImageView>(R.id.closeBtn).setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            isClapDialogShowing = false
        }

        try {
            dialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
            isClapDialogShowing = false
        }
    }

    private fun navigationBar() {
        adapter = HomeViewPagerAdapter(this@MainActivity)
        binding.apply {
            viewpager.adapter = adapter
            viewpager.isUserInputEnabled = false
            viewpager.currentItem = 0
            bottomNavigation.setItemSelected(R.id.home, true)
            bottomNavigation.setOnItemSelectedListener {
                when (it) {
                    R.id.home -> {
                        binding.root.clearFocus()
                        currentFocus?.clearFocus()
                        binding.viewpager.clearFocus()
                        viewpager.currentItem = 0
                        binding.title.text = getString(R.string.find_my)
                    }

                    R.id.Sounds -> {
                        binding.root.clearFocus()
                        currentFocus?.clearFocus()
                        binding.viewpager.clearFocus()
                        viewpager.currentItem = 1
                        binding.title.text = getString(R.string.sounds_programatically)
                    }

                    R.id.add_sounds -> {
                        binding.root.clearFocus()
                        currentFocus?.clearFocus()
                        binding.viewpager.clearFocus()
                        viewpager.currentItem = 2
                        binding.title.text = getString(R.string.add_sound_programatically)
                    }
                }
            }
            viewpager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    when (position) {
                        0 -> {
                            title.text = getString(R.string.find_my)
                            title2.text = getString(R.string.phone)
                            titleB.visible()
                            backBtn.gone()
                            titleB.text = getString(R.string.select_a_feature_to_protect_your_device)
                        }

                        1 -> {
                            title.text = getString(R.string.find_my)
                            title2.text = getString(R.string.phone)
                            titleB.gone()
                            backBtn.visible()
                        }
                    }
                }
            })

            backBtn.setSafeOnClickListener {
                viewpager.currentItem = 0
            }
        }
    }

    private fun showConsent() {
        googleMobileAdsConsentManager =
            GoogleMobileAdsConsentManager.getInstance(applicationContext)
        googleMobileAdsConsentManager.gatherConsent(
            this
        ) { consentError ->
            if (consentError != null) {
                Log.w(
                    TAG,
                    String.format(
                        "%s: %s",
                        consentError.getErrorCode(),
                        consentError.getMessage()
                    )
                )
            }
            if (googleMobileAdsConsentManager.canRequestAds()) {
                initializeMobileAdsSdk()
            }
            if (googleMobileAdsConsentManager.isPrivacyOptionsRequired) {
                invalidateOptionsMenu()
            }
        }

        if (googleMobileAdsConsentManager.canRequestAds()) {
            initializeMobileAdsSdk()
        }
    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }
        MyApplication.mInstance.adsManager.initSDK(
            this
        ) {
          //  initAds()
        }
    }

    override fun onResume() {
        super.onResume()

        if (MyApplication.shouldRestartDetectorFromStopSound) {
            MyApplication.shouldRestartDetectorFromStopSound = false
            DetectorWorkerStarter.requestStart(
                this@MainActivity,
                "MainActivity.onResume"
            )
        }

        if (MyApplication.mInstance.preferenceManager.getBoolean(
                Key.IS_APP_PREMIUM, false
            )
        ) {
            binding.trailCard.visibility = View.GONE
            return
        }
    }

}
