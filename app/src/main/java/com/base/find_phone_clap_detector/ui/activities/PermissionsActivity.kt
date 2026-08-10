package com.base.find_phone_clap_detector.ui.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.databinding.ActivityPermissionsBinding
import com.base.find_phone_clap_detector.managers.AdsManager
import com.base.find_phone_clap_detector.managers.PreferenceManager
import com.base.find_phone_clap_detector.myApplication.MyApplication
import com.base.find_phone_clap_detector.utils.Language
import com.base.find_phone_clap_detector.utils.LocaleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PermissionsActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityPermissionsBinding.inflate(layoutInflater)
    }
    private var isDarkTheme = MyApplication.mInstance.preferenceManager.getBoolean(PreferenceManager.Key.isDarkTheme , false)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply current app locale before view inflation
        LocaleHelper.setAppLocale(this)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(binding.main.id)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        if (!MyApplication.mInstance.preferenceManager.getBoolean(PreferenceManager.Key.isDarkTheme , false)){
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        initViews()
    }

    private fun initViews() {
        if (isDarkTheme){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        binding.apply {}
    }

}