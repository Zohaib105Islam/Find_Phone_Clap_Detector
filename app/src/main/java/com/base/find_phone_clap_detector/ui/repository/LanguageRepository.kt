package com.base.find_phone_clap_detector.ui.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.Keep
import androidx.lifecycle.MutableLiveData
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.ui.dataClasses.ModelLanguage
import javax.inject.Inject

@Keep
class LanguageRepository @Inject constructor(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

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

    private val itemListLiveData = MutableLiveData<MutableList<ModelLanguage>>()

    init {
        val selectedLanguage = sharedPreferences.getString("SELECTED_LANGUAGE", "English")
        defaultItemList.forEach {
            it.isChecked = it.language == selectedLanguage
        }
        itemListLiveData.value = defaultItemList
    }

    fun fetchItems(): MutableLiveData<MutableList<ModelLanguage>> {
        return itemListLiveData
    }

    fun updateItemStatusByName(language: String, newStatus: Boolean) {
        val itemList = itemListLiveData.value ?: return

        itemList.forEach { item ->
            item.isChecked = item.language == language && newStatus
        }

        // Save selected language in SharedPreferences
        sharedPreferences.edit().putString("SELECTED_LANGUAGE", language).apply()

        itemListLiveData.value = itemList
    }
}