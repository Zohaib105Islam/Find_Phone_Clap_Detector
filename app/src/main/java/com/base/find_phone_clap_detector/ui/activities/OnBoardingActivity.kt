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
import androidx.viewpager2.widget.ViewPager2
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.databinding.ActivityOnBoardingBinding
import com.base.find_phone_clap_detector.managers.AnalyticsManager
import com.base.find_phone_clap_detector.managers.PreferenceManager
import com.base.find_phone_clap_detector.myApplication.MyApplication
import com.base.find_phone_clap_detector.ui.adapters.ScreenSlidePagerAdapter
import com.base.find_phone_clap_detector.utils.Constants.NUMBER_OF_ON_BOARDING_SLIDER
import com.base.find_phone_clap_detector.utils.LocaleHelper

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

        addDots()
        initClicks()

        // Select first dot initially
        selectDot(0)
        updateActionButton(0)
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupTheme() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }

    private fun initClicks() {
        binding.btnNext.setOnClickListener {
            val nextItem = binding.onBoardingViewPager.currentItem + 1
            if (nextItem < NUMBER_OF_ON_BOARDING_SLIDER) {
                binding.onBoardingViewPager.currentItem = nextItem
            } else {
                AnalyticsManager.logEvent("FA_onboarding_get_started")
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
        val dotSpacing = (4 * resources.displayMetrics.density).toInt()
        dotsLayout.removeAllViews()
        for (i in 0 until NUMBER_OF_ON_BOARDING_SLIDER) {
            val dot = ImageView(this)
            dot.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.onboarding_indicator_inactive))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.marginEnd = dotSpacing
            dotsLayout.addView(dot, params)
            dots.add(dot)
        }

        binding.onBoardingViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                selectDot(position)
                updateActionButton(position)
            }
        })
    }

    private fun selectDot(idx: Int) {
        for (i in 0 until NUMBER_OF_ON_BOARDING_SLIDER) {
            val drawableId = if (i == idx) {
                R.drawable.onboarding_indicator_active
            } else {
                R.drawable.onboarding_indicator_inactive
            }
            dots[i].setImageDrawable(ContextCompat.getDrawable(this, drawableId))
        }
    }

    private fun updateActionButton(position: Int) {
        binding.tvNext.setText(
            if (position == NUMBER_OF_ON_BOARDING_SLIDER - 1) {
                R.string.get_started
            } else {
                R.string.next
            }
        )
    }

}
