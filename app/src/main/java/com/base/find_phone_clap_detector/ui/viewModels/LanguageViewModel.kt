package com.base.find_phone_clap_detector.ui.viewModels

import androidx.lifecycle.ViewModel
import com.base.find_phone_clap_detector.ui.repository.LanguageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val repository: LanguageRepository
) : ViewModel() {

    val itemList = repository.fetchItems()

    fun updateLanguage(language: String, isChecked: Boolean) {
        repository.updateItemStatusByName(language, isChecked)
    }
}