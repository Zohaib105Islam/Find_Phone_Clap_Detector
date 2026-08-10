package com.base.find_phone_clap_detector.ui.dataClasses

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import androidx.annotation.Keep

@Keep
@Parcelize
data class AudioModel(
    val id: Long,
    val title: String,
    val artist: String?,
    val data: String,
    val audioUri: Uri,
    val duration: String = "00:00",
    val fileSize: String = "0.00"
) : Parcelable
