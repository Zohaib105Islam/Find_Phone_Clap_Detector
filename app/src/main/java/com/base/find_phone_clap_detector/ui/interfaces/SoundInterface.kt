package com.base.find_phone_clap_detector.ui.interfaces

import com.base.find_phone_clap_detector.ui.dataClasses.SoundsDataClass

interface SoundInterface {
    fun clickOnSoundListener(
        position: Int,
        premium: Boolean,
        img: Int,
        title: String,
        audioUri: String
    )

    fun onDeleteSound(position: Int, sound: SoundsDataClass)
}