package com.base.find_phone_clap_detector.ui.dataClasses

import androidx.annotation.DrawableRes
import androidx.annotation.Keep

@Keep
data class SoundsDataClass(
    var title: String,
    @DrawableRes
    val img: Int,
    val isPremium: Boolean,
    val audioUri: String
)