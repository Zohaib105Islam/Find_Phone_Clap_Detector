package com.base.find_phone_clap_detector.ui.interfaces

import com.base.find_phone_clap_detector.ui.adapters.AudiosAdapter
import com.base.find_phone_clap_detector.ui.dataClasses.AudioModel
import com.base.find_phone_clap_detector.ui.dataClasses.SoundsDataClass

interface CallBackAudioSelection {
    fun getItem(
        position: Int,
        holder: AudiosAdapter.MyViewHolder,
        albumPhoto: AudioModel?
    )
}