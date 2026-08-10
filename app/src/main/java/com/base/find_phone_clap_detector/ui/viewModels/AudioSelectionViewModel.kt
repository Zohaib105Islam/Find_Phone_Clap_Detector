package com.base.find_phone_clap_detector.ui.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.base.find_phone_clap_detector.ui.dataClasses.AudioModel
import com.base.find_phone_clap_detector.ui.repository.AudioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioSelectionViewModel @Inject constructor(
    private val audioRepository: AudioRepository
) : ViewModel() {

    private val _audioModel = MutableLiveData<List<AudioModel>>()
    val audioModel: LiveData<List<AudioModel>> get() = _audioModel

    fun setAudioListIfNotLoaded(list: List<AudioModel>) {
        if (_audioModel.value.isNullOrEmpty()) {
            _audioModel.postValue(list)
        }
    }

    fun fetchAudioFiles() {
        viewModelScope.launch {
            delay(2000)
            val audioList = audioRepository.getAllAudioFiles()
            _audioModel.postValue(audioList)
        }
    }
}