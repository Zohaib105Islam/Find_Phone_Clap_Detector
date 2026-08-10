package com.base.find_phone_clap_detector.ui.activities

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdSize
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.databinding.ActivityLanguageBinding
import com.base.find_phone_clap_detector.managers.AdsManager
import com.base.find_phone_clap_detector.managers.AnalyticsManager
import com.base.find_phone_clap_detector.managers.PreferenceManager
import com.base.find_phone_clap_detector.myApplication.MyApplication
import com.base.find_phone_clap_detector.ui.adapters.LanguagesAdapter
import com.base.find_phone_clap_detector.ui.dataClasses.ModelLanguage
import com.base.find_phone_clap_detector.utils.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class LanguageActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityLanguageBinding.inflate(layoutInflater)
    }
    val isAppFirstTime = MyApplication.mInstance.preferenceManager.getBoolean(
        PreferenceManager.Key.IS_APP_FIRST_TIME,
        true
    )
    private var isDarkTheme = MyApplication.mInstance.preferenceManager.getBoolean(
        PreferenceManager.Key.isDarkTheme,
        false
    )
    private lateinit var adapter: LanguagesAdapter

    private var selectedLanguage: String? = null

    private val doneTranslations = mapOf(
        "English" to "Save",
        "Arabic (العربية)" to "حفظ",
        "Hindi (हिंदी)" to "सहेजें",
        "Portuguese (Português)" to "Salvar",
        "Russian (Русский)" to "Сохранить",
        "Turkish (Türkçe)" to "Kaydet",
        "Spanish" to "Guardar",
        "German" to "Speichern",
        "French" to "Enregistrer",
        "Italian" to "Salva"
    )

    private val handler = Handler(Looper.getMainLooper())
    private var applyingLanguageDialog: AlertDialog? = null
    private var progressAnimator: ValueAnimator? = null

    private val TAG = "LanguageActivity"


    override fun onCreate(savedInstanceState: Bundle?) {
        LocaleHelper.setAppLocale(this)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(binding.main.id)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isAppFirstTime) {
                    Log.d("TAG", "onBackPressed: Nothing Happens")
                } else {
                    finish()
                }
            }
        })

        if (!MyApplication.mInstance.preferenceManager.getBoolean(
                PreferenceManager.Key.isDarkTheme,
                false
            )
        ) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        initViews()
        initViewModel()
        if (isAppFirstTime) {
            binding.backBtn.visibility = View.GONE
            binding.doneBtn.alpha = 1f
            binding.doneBtn.isEnabled = true
        } else {
            binding.backBtn.visibility = View.VISIBLE
            binding.doneBtn.alpha = 0.4f
            binding.doneBtn.isEnabled = false
        }
    }

    private fun initViews() {
        if (isDarkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.doneBtn.setOnClickListener {

            if (selectedLanguage == null) {
                Toast.makeText(this, "Please select a language", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showApplyingLanguageDialog {
                try {
                    // Save language
                    MyApplication.mInstance.preferenceManager.put(
                        PreferenceManager.Key.APP_LANGUAGE,
                        selectedLanguage
                    )

                    Log.d("LanguageActivityCheck", " try Language is: ${selectedLanguage}")

                    if (isAppFirstTime) {
                        AnalyticsManager.logEvent("FA_onboarding_language_save")
                        startActivity(Intent(this@LanguageActivity, OnBoardingActivity::class.java))
                        finish()
                    } else {
                        MyApplication.mInstance.preferenceManager.put(
                            PreferenceManager.Key.SKIP_PREMIUM,
                            true
                        )
                        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(intent)
                        finishAffinity()

                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        this,
                        "Failed to apply language. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        }

    }

    private fun initViewModel() {
        binding.recyclerView.layoutManager =
            LinearLayoutManager(this@LanguageActivity, RecyclerView.VERTICAL, false)

        val savedLanguageName = MyApplication.mInstance.preferenceManager
            .getString(PreferenceManager.Key.APP_LANGUAGE, "English") ?: "English"

        Log.d("LanguageActivityCheck", "Language is: $savedLanguageName")

        val defaultItemList = mutableListOf(
            ModelLanguage(R.drawable.flag_english, "English", false),
            ModelLanguage(R.drawable.flag_arab, "Arabic (العربية)", false),
            ModelLanguage(R.drawable.flag_india, "Hindi (हिंदी)", false),
            ModelLanguage(R.drawable.flag_portugal, "Portuguese (Português)", false),
            ModelLanguage(R.drawable.flag_russian, "Russian (Русский)", false),
            ModelLanguage(R.drawable.flag_turkey, "Turkish (Türkçe)", false),
            ModelLanguage(R.drawable.flag_spanish, "Spanish", false),
            ModelLanguage(R.drawable.flag_german, "German", false),
            ModelLanguage(R.drawable.flag_french, "French", false),
            ModelLanguage(R.drawable.flag_italian, "Italian", false)
        )

        // Mark only saved language as selected
        defaultItemList.forEach {
            it.isChecked = it.language.trim() == savedLanguageName.trim()
        }
        Log.d("LanguageActivityCheck", " Before LanguagesAdapter List is: ${defaultItemList}")

        selectedLanguage = savedLanguageName

        Log.d("LanguageActivityCheck", " Before LanguagesAdapter is: ${selectedLanguage}")

        lifecycleScope.launch {
            delay(1.seconds)
            binding.pb.visibility = View.GONE
            binding.doneBtn.visibility = View.VISIBLE

            adapter = LanguagesAdapter(this@LanguageActivity, defaultItemList) { selected ->
                enableDoneButton()
                selectedLanguage = selected.language
                Log.d("LanguageActivityCheck", "LanguagesAdapter is: ${selected.language}")
                // Change button text according to selected language
                binding.doneBtn.text = doneTranslations[selected.language] ?: "Save"
            }
            binding.recyclerView.adapter = adapter

            val animation = AnimationUtils.loadLayoutAnimation(
                this@LanguageActivity,
                R.anim.layout_animation_fall_down
            )

            binding.recyclerView.layoutAnimation = animation
            binding.recyclerView.scheduleLayoutAnimation()
        }

    }

    private fun enableDoneButton() {
        binding.doneBtn.alpha = 1f
        binding.doneBtn.isEnabled = true
    }

    private fun showApplyingLanguageDialog(onComplete: () -> Unit) {
        dismissApplyingLanguageDialog()

        val dialogView = LayoutInflater.from(this).inflate(R.layout.layout_applying_loading, null)

        val progressBar = dialogView.findViewById<ProgressBar>(R.id.pb_ad_loading)

        applyingLanguageDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        applyingLanguageDialog?.setCancelable(false)
        applyingLanguageDialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        applyingLanguageDialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        applyingLanguageDialog?.window?.setDimAmount(0f)

        if (!isFinishing && !isDestroyed) {
            applyingLanguageDialog?.show()
            startProgressBarAnimation(progressBar){
                onComplete()
            }

        }
    }

    private fun startProgressBarAnimation(progressBar: ProgressBar,onComplete: () -> Unit) {
        progressAnimator?.cancel()

        progressAnimator = ValueAnimator.ofInt(0, 100).apply {
            duration = 4000
            interpolator = LinearInterpolator()

            addUpdateListener { animator ->
                val progress = animator.animatedValue as Int
                progressBar.progress = progress

                if (progress == 100) {
                    dismissApplyingLanguageDialog()
                    onComplete()
                }
            }
            start()
        }
    }


    private fun dismissApplyingLanguageDialog() {
        progressAnimator?.cancel()
        if (applyingLanguageDialog?.isShowing == true) {
            applyingLanguageDialog?.dismiss()
        }
        applyingLanguageDialog = null
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        dismissApplyingLanguageDialog()
        super.onDestroy()
    }

}