package com.base.find_phone_clap_detector.ui.activities

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.databinding.ActivitySplashBinding
import com.base.find_phone_clap_detector.managers.PreferenceManager
import com.base.find_phone_clap_detector.myApplication.MyApplication
import com.base.find_phone_clap_detector.utils.LocaleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val binding: ActivitySplashBinding by lazy {
        ActivitySplashBinding.inflate(layoutInflater)
    }

    val isAppFirstTime =
        MyApplication.mInstance.preferenceManager.getBoolean(
            PreferenceManager.Key.IS_APP_FIRST_TIME,
            true
        )

    private val totalTime = 6000L
    private val interval = 100L
    private var isNavigating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        LocaleHelper.setAppLocale(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainSplash)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        playEntranceAnimations()
        startProgress()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                android.webkit.WebSettings.getDefaultUserAgent(applicationContext)
            } catch (e: Throwable) {
                // Ignore any WebView errors
            }
        }
    }

    private fun initViews() {
        try {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } catch (e: Exception) {
            e.printStackTrace()
        }

        binding.apply {
            loadingContainer.visibility = View.VISIBLE
            loadingProgress.progress = 0
        }
    }

    private fun playEntranceAnimations() {
        binding.apply {
            animateIn(tvTitle, startDelay = 100L)
            animateIn(tvSubtitle, startDelay = 250L)
            animateIn(loadingContainer, startDelay = 450L)
        }
    }

    private fun animateIn(view: View, startDelay: Long) {
        view.translationY = 24f
        val fade = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f)
        val rise = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 24f, 0f)

        AnimatorSet().apply {
            playTogether(fade, rise)
            duration = 500L
            this.startDelay = startDelay
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    private fun startProgress() {
        lifecycleScope.launch(Dispatchers.IO) {
            val steps = totalTime / interval
            for (i in 1..steps) {
                delay(interval.milliseconds)
                val progress = (i * 100 / steps).toInt()
                withContext(Dispatchers.Main) {
                    animateProgressTo(progress)
                }
            }
            withContext(Dispatchers.Main) {
                openNextScreen()
            }
        }
    }

    private fun animateProgressTo(progress: Int) {
        ObjectAnimator.ofInt(
            binding.loadingProgress,
            "progress",
            binding.loadingProgress.progress,
            progress
        )
            .apply {
                duration = interval
                interpolator = DecelerateInterpolator()
                start()
            }
    }

    private fun openNextScreen() {
        if (isNavigating) return
        isNavigating = true

        val fadeOut = ObjectAnimator.ofFloat(binding.root, View.ALPHA, 1f, 0f).apply {
            duration = 300L
        }
        fadeOut.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                navigateNext()
            }
        })
        fadeOut.start()
    }

    private fun navigateNext() {
        if (isAppFirstTime) {
            startActivity(Intent(this, LanguageActivity::class.java))
            finish()
        } else {
            val isClapDetected =
                MyApplication.mInstance.preferenceManager.getBoolean(
                    PreferenceManager.Key.isClapDetected,
                    false
                )
            if (isClapDetected) {
                startActivity(Intent(this, StopSoundActivity::class.java))
                finish()
            } else {
                MyApplication.isFirstPremium = true
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onStop() {
        super.onStop()
        try {
            com.base.find_phone_clap_detector.utils.QueuedWorkFix.clearQueuedWork()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
