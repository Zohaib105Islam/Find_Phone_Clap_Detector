package com.base.find_phone_clap_detector.ui.dataClasses

import androidx.annotation.Keep

@Keep
data class ModelLanguage (
    val image: Int,
    val language : String,
    var isChecked: Boolean
)