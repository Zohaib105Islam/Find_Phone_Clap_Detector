package com.base.find_phone_clap_detector.ui.viewModels

import android.app.Application
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {
    private lateinit var recorder: MediaRecorder
    private lateinit var mediaPlayer: MediaPlayer
    private var outputFilePath: String? = null
    private val context: Application = getApplication()
    val handler = Handler(Looper.getMainLooper())
    var filePath: MutableLiveData<String> = MutableLiveData()
    var isRecording = false
    fun getFilePath(): String {
        return filePath.value ?: "null"
    }

    private val _tickDuration = MutableLiveData<Int>()
    val tickDuration: LiveData<Int>
        get() = _tickDuration

    private val _maxAmplitude = MutableLiveData<Int>()
    val maxAmplitude: LiveData<Int>
        get() = _maxAmplitude
    private val _currentTime = MutableLiveData<Long>()
    val currentTime: LiveData<Long>
        get() = _currentTime

    init {

        _tickDuration.value = (BUFFER_SIZE.toDouble() * 500 / BYTE_RATE).toInt()
        _maxAmplitude.value = 0
    }

    private var isTimerRunning = false
    private var pausedTime: Long = 0
    private var seconds = 0L

    fun startOrResumeTimer() {
        if (!isTimerRunning) {
            isTimerRunning = true

            handler.post(object : Runnable {
                override fun run() {
                    _currentTime.value = seconds++
                    handler.postDelayed(this, 1000) // Update every second
                }
            })
        }
    }

    // Call this function to pause the timer
    fun pauseTimer() {
        if (isTimerRunning) {
            isTimerRunning = false
            pausedTime = seconds
            handler.removeCallbacksAndMessages(null)
        }
    }

    // Call this function to stop and reset the timer
    fun stopTimer() {
        isTimerRunning = false
        seconds = 0
        _currentTime.value = 0
        handler.removeCallbacksAndMessages(null)
    }

    fun startRecording() {
        recorder = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
        try {
            recorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder?.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            outputFilePath = "${context.externalCacheDir?.absolutePath}/DemoAudioRecording.3gp"
            Log.d("outPutFilePath", "startRecording:${outputFilePath} ")
            filePath.value = outputFilePath!!
            recorder?.setOutputFile(outputFilePath)
            recorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            // Prepare and start recording
            recorder?.prepare()
            recorder?.start()
            isRecording = true
            startOrResumeTimer()
//            Toast.makeText(context.applicationContext, "Recording started", Toast.LENGTH_SHORT)
//                .show()
            handler.post(object : Runnable {
                override fun run() {
                    _maxAmplitude.value = try {
                        if (isRecording) recorder?.maxAmplitude else 0
                    } catch (e: Exception) {
                        0
                    }
                    _tickDuration.value = (BUFFER_SIZE.toDouble() * 500 / BYTE_RATE).toInt()
                    handler.postDelayed(this, 300) // Update every second
                }
            })
        } catch (e: IOException) {
            Log.d("TAG", "startRecording: ")
        } catch (e: IllegalStateException) {
            Log.d("TAG", "startRecording: ")
        }
    }

    fun stopRecording() {
        try {
            // Stop and release the MediaRecorder
            recorder.stop()
            isRecording = false
            recorder.release()
            stopTimer()

//            Toast.makeText(context.applicationContext, "Recording stopped", Toast.LENGTH_SHORT)
//                .show()
            stopTimer()
            // TODO: Handle the recorded audio file at outputFilePath
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pauseRecording() {
        try {
            // Stop and release the MediaRecorder
            recorder.pause()
//            Toast.makeText(context.applicationContext, "Recording paused", Toast.LENGTH_SHORT)
//                .show()
            pauseTimer()
            // TODO: Handle the recorded audio file at outputFilePath
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resumeRecording() {
        try {
            // Stop and release the MediaRecorder
            recorder.resume()
            handler.post(object : Runnable {
                override fun run() {
                    _maxAmplitude.value = if (recorder != null) {
                        recorder?.maxAmplitude
                    } else 0
                    _tickDuration.value = (BUFFER_SIZE.toDouble() * 500 / BYTE_RATE).toInt()
                    handler.postDelayed(this, 300) // Update every second
                }
            })
            startOrResumeTimer()
//            Toast.makeText(context.applicationContext, "Recording resumed", Toast.LENGTH_SHORT)
//                .show()
            // TODO: Handle the recorded audio file at outputFilePath
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playRecording() {
        try {
            mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(outputFilePath)
            mediaPlayer.prepare()
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener {
                // Playback completed
//                Toast.makeText(context.applicationContext, "Playback completed", Toast.LENGTH_SHORT)
//                    .show()
            }
//            Toast.makeText(context.applicationContext, "Playback started", Toast.LENGTH_SHORT)
//                .show()

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun reset() {
        recorder.release()
        mediaPlayer.release()
    }

    companion object {
        private const val SAMPLING_RATE_IN_HZ = 8000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val CHANNEL_COUNT = 1
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BIT_PER_SAMPLE = 16
        private const val BYTE_RATE = (BIT_PER_SAMPLE * SAMPLING_RATE_IN_HZ * CHANNEL_COUNT / 8).toLong()
        private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE_IN_HZ, CHANNEL_CONFIG, AUDIO_FORMAT)
    }
}