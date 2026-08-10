package com.base.find_phone_clap_detector.utils

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.Keep
import java.util.Locale

@Keep
class Language {
    fun setLanguage(context: Context) {
        val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val selectedLanguage = sharedPreferences.getString("SELECTED_LANGUAGE", "English") ?: "English"

        val languageMap = mapOf(
            "English" to "en",
            "Arabic (العربية)" to "ar",
            "Hindi (हिंदी)" to "hi",
            "Portuguese (Português)" to "pt",
            "Russian (Русский)" to "ru",
            "Turkish (Türkçe)" to "tr",
            "Spanish" to "es",
            "German" to "de",
            "French" to "fr",
            "Italian" to "it"
        )

        val langCode = languageMap[selectedLanguage] ?: "en"
        val locale = Locale(langCode)
        Locale.setDefault(locale)

        val config = Configuration()
        config.setLocale(locale)

        // Force LTR direction regardless of language
        config.setLayoutDirection(Locale("en"))

        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}