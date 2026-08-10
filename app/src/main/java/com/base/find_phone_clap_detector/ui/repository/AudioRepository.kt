package com.base.find_phone_clap_detector.ui.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.base.find_phone_clap_detector.ui.dataClasses.AudioModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import java.util.concurrent.TimeUnit
import java.util.Locale

class AudioRepository @Inject constructor(private val context: Context) {

    suspend fun getAllAudioFiles(): List<AudioModel> = withContext(Dispatchers.IO) {
        val audioList = mutableListOf<AudioModel>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION
        )

        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            MediaStore.Audio.Media.DATE_ADDED + " DESC"
        )

        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val dataCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val title = it.getString(titleCol)
                val data = it.getString(dataCol)
                val durationMs = it.getLong(durationCol)
                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                val file = File(data)
                if (file.exists() && file.length() > 0) {
                    audioList.add(
                        AudioModel(
                            id = id,
                            title = title,
                            artist = "artist",
                            data = data,
                            audioUri = uri,
                            duration = formatDuration(durationMs),
                            fileSize = formatFileSizeInMB(file)
                        )
                    )
                }
            }
        }

        audioList
    }

    private fun formatDuration(durationMs: Long): String {
        val safeDurationMs = durationMs.coerceAtLeast(0L)
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(safeDurationMs)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    private fun formatFileSizeInMB(file: File): String {
        val fileSizeInBytes = file.length()
        val fileSizeInMB = fileSizeInBytes.toDouble() / (1024 * 1024)
        return String.format(Locale.US, "%.2f", fileSizeInMB)
    }
}
