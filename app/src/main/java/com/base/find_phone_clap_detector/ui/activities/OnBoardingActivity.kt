package com.base.find_phone_clap_detector.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.ads.AdSize
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.databinding.ActivityOnBoardingBinding
import com.base.find_phone_clap_detector.managers.AnalyticsManager
import com.base.find_phone_clap_detector.managers.PreferenceManager
import com.base.find_phone_clap_detector.myApplication.MyApplication
import com.base.find_phone_clap_detector.ui.adapters.ScreenSlidePagerAdapter
import com.base.find_phone_clap_detector.utils.Constants.NUMBER_OF_ON_BOARDING_SLIDER
import com.base.find_phone_clap_detector.utils.LocaleHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class OnBoardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnBoardingBinding
    private lateinit var screenSlidePagerAdapter: ScreenSlidePagerAdapter
    private lateinit var dots: ArrayList<ImageView>

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        LocaleHelper.setAppLocale(this)
        super.onCreate(savedInstanceState)
        binding = ActivityOnBoardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAffinity()
            }
        })

        setupInsets()
        setupTheme()

        screenSlidePagerAdapter = ScreenSlidePagerAdapter(this)
        binding.onBoardingViewPager.adapter = screenSlidePagerAdapter

        lifecycleScope.launch {
            delay(3.seconds)
            binding.pbNext.isVisible = false
            binding.ivNext.isVisible = true
        }

        addDots()
        addTopDots()
        loadAds()
        initClicks()

        // Select first dot initially
        selectDot(0)
        selectTopDot(0)
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupTheme() {
        if (!MyApplication.mInstance.preferenceManager.getBoolean(
                PreferenceManager.Key.isDarkTheme,
                false
            )
        ) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    private fun initClicks() {
        binding.getStarted.setOnClickListener {
            AnalyticsManager.logEvent("FA_onboarding_get_started")
            startMainActivity()
        }

        binding.btnNext.setOnClickListener {
            if (binding.pbNext.isVisible) return@setOnClickListener
            val nextItem = binding.onBoardingViewPager.currentItem + 1
            if (nextItem < NUMBER_OF_ON_BOARDING_SLIDER) {
                binding.onBoardingViewPager.currentItem = nextItem
            } else {
                AnalyticsManager.logEvent("FA_onboarding_last_get_started")
                startMainActivity()
            }
        }
    }

    private fun startMainActivity() {
        MyApplication.mInstance.preferenceManager.put(
            PreferenceManager.Key.IS_APP_FIRST_TIME,
            false
        )
        intent.putExtra("fromSplash", true)
        MyApplication.isFirstPremium = true
        val intent1 = Intent(this, MainActivity::class.java)
        startActivity(intent1)
        finish()
    }

    private fun addDots() {
        dots = ArrayList()
        val dotsLayout = binding.dots
        dotsLayout.removeAllViews()
        for (i in 0 until NUMBER_OF_ON_BOARDING_SLIDER) {
            val dot = ImageView(this)
            dot.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.tab_indicator_default))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 15, 0)
            dotsLayout.addView(dot, params)
            dots.add(dot)
        }

        binding.onBoardingViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                selectDot(position)
                selectTopDot(position)

                if (position == NUMBER_OF_ON_BOARDING_SLIDER - 1) {
                    // Last page → show Get Started and top dots
                    binding.getStarted.visibility = View.VISIBLE
                    binding.dotsTop.visibility = View.VISIBLE

                    // Hide Next button and bottom dots
                    binding.btnNext.visibility = View.GONE
                    binding.dots.visibility = View.GONE
                } else {
                    // Not last page → show Next and bottom dots
                    binding.getStarted.visibility = View.GONE
                    binding.dotsTop.visibility = View.GONE

                    binding.btnNext.visibility = View.VISIBLE
                    binding.dots.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun addTopDots() {
        binding.dotsTop.removeAllViews()
        for (i in 0 until NUMBER_OF_ON_BOARDING_SLIDER) {
            val dot = ImageView(this)
            dot.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.tab_indicator_default))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 15, 0)
            binding.dotsTop.addView(dot, params)
        }
    }

    private fun selectDot(idx: Int) {
        for (i in 0 until NUMBER_OF_ON_BOARDING_SLIDER) {
            val drawableId = if (i == idx) R.drawable.tab_indicator_selected else R.drawable.tab_indicator_default
            dots[i].setImageDrawable(ContextCompat.getDrawable(this, drawableId))
        }
    }

    private fun selectTopDot(idx: Int) {
        for (i in 0 until NUMBER_OF_ON_BOARDING_SLIDER) {
            val drawableId = if (i == idx) R.drawable.tab_indicator_selected else R.drawable.tab_indicator_default
            (binding.dotsTop.getChildAt(i) as ImageView).setImageDrawable(
                ContextCompat.getDrawable(this, drawableId)
            )
        }
    }

    private fun loadAds() {
        MyApplication.mInstance.adsManager.showBanner(
            this,
            AdSize.MEDIUM_RECTANGLE,
            binding.adFrame,
            this.getString(R.string.ADMOB_BANNER_MEDIUM_RECTANGLE_V2),
            binding.shimmerLayout
        )
    }
}