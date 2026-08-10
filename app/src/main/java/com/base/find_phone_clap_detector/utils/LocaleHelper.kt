package com.base.find_phone_clap_detector.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.base.find_phone_clap_detector.managers.PreferenceManager
import com.base.find_phone_clap_detector.myApplication.MyApplication

object LocaleHelper {

    fun setAppLocale(context:Context) {
        val languageName = MyApplication.mInstance.preferenceManager
            .getString(PreferenceManager.Key.APP_LANGUAGE, "English")

        val langCode = when (languageName) {
            "English" -> "en"
            "Arabic (العربية)" -> "ar"
            "Hindi (हिंदी)" -> "hi"
            "Portuguese (Português)" -> "pt"
            "Russian (Русский)" -> "ru"
            "Turkish (Türkçe)" -> "tr"
            "Spanish" -> "es"
            "German" -> "de"
            "French" -> "fr"
            "Italian" -> "it"
            else -> "en"
        }


        // Use AppCompatDelegate to set app-wide locales
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(langCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }
}