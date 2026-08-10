package com.base.find_phone_clap_detector.ui.dataClasses

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.Keep
import com.google.gson.Gson
import com.base.find_phone_clap_detector.R
import javax.inject.Inject

@Keep
class LanguageSharedPreferences @Inject constructor(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val defaultItemList = mutableListOf(
        ModelLanguage(R.drawable.flag_english, "English", true),
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

    init {
        // Load the initial list from SharedPreferences or use the default if it doesn't exist
        val savedList = getLanguageList()
        if (savedList.isEmpty()) {
            saveLanguageList(defaultItemList)
        }
    }

    fun saveLanguageList(list: List<ModelLanguage>) {
        val json = gson.toJson(list)
        sharedPreferences.edit().putString("language_list", json).apply()
    }

    fun getLanguageList(): List<ModelLanguage> {
        val json = sharedPreferences.getString("language_list", "")
        return if (json.isNullOrEmpty()) {
            emptyList()
        } else {
            gson.fromJson(json, ModelLanguageListType().type)
        }
    }
}