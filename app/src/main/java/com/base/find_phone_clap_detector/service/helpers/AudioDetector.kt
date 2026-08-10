package com.base.find_phone_clap_detector.service.helpers

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.base.find_phone_clap_detector.service.DetectorLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds

class AudioDetector(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
) {
    private companion object {
        private const val TAG = "AudioDetector"
        private const val CLAP_PEAK_THRESHOLD = 16000
        private const val FRAME_DELAY_MS = 100L
        private const val CLAP_RESET_WINDOW_MS = 2000L
        private const val CLAP_PAIR_WINDOW_MS = 800L
        private const val WHISTLE_PAIR_WINDOW_MS = 1000L
        private const val WHISTLE_RESTART_DELAY_MS = 1000L
        private const val DETECTION_BOOT_DELAY_MS = 3000L
    }

    private val audioSource = MediaRecorder.AudioSource.MIC
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private var bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    private var audioRecorder: AudioRecord? = null
    private val audioRecorderLock = Any()
    private var detectionJob: Job? = null
    private var clapCounter = 0
    private var lastClapTime = System.currentTimeMillis()
    private var isClapDetected = false

    @SuppressLint("MissingPermission")
    fun startClapDetection(onDetected: suspend () -> Unit) {
        DetectorLog.d(TAG, "startClapDetection: initializing")
        try {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                DetectorLog.e(TAG, "startClapDetection: RECORD_AUDIO permission missing")
                return
            }

            bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            if (bufferSize <= 0) {
                DetectorLog.e(TAG, "startClapDetection: invalid buffer size $bufferSize")
                return
            }

            val readBufferSize = maxOf(bufferSize, 2048)
            audioRecorder =
                AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, readBufferSize)
            if (audioRecorder?.state != AudioRecord.STATE_INITIALIZED) {
                DetectorLog.e(TAG, "startClapDetection: AudioRecord not initialized")
                return
            }

            audioRecorder?.startRecording()
            detectionJob?.cancel()
            detectionJob = coroutineScope.launch(Dispatchers.Default) {
                val buffer = ShortArray(readBufferSize)
                while (isActive) {
                    val readResult = synchronized(audioRecorderLock) {
                        val recorder = audioRecorder
                        if (recorder == null || recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                            AudioRecord.ERROR_INVALID_OPERATION
                        } else {
                            recorder.read(buffer, 0, buffer.size)
                        }
                    }
                    if (readResult > 0) {
                        var peak = 0
                        for (index in 0 until readResult) {
                            peak = maxOf(peak, abs(buffer[index].toInt()))
                        }

                        if (peak >= CLAP_PEAK_THRESHOLD) {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastClapTime <= CLAP_PAIR_WINDOW_MS) {
                                clapCounter++
                                if (clapCounter >= 2 && !isClapDetected) {
                                    isClapDetected = true
                                    onDetected()
                                }
                            } else {
                                clapCounter = 1
                            }
                            lastClapTime = currentTime
                        } else if (System.currentTimeMillis() - lastClapTime > CLAP_RESET_WINDOW_MS) {
                            clapCounter = 0
                            isClapDetected = false
                        }
                    }
                    delay(FRAME_DELAY_MS.milliseconds)
                }
            }
        } catch (e: Exception) {
            DetectorLog.e(TAG, "startClapDetection: ${e.message}", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun startWhistleDetection(onDetected: suspend () -> Unit) {
        DetectorLog.d(TAG, "startWhistleDetection: initializing")
        try {
            cleanupAudioResources()

            bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            if (bufferSize <= 0) {
                DetectorLog.e(TAG, "startWhistleDetection: invalid buffer size $bufferSize")
                return
            }

            val actualBufferSize = maxOf(bufferSize, 4096)
            audioRecorder =
                AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, actualBufferSize)

            if (audioRecorder?.state != AudioRecord.STATE_INITIALIZED) {
                DetectorLog.e(TAG, "startWhistleDetection: AudioRecord not initialized")
                cleanupAudioResources()
                return
            }

            audioRecorder?.startRecording()
            detectionJob?.cancel()
            detectionJob = coroutineScope.launch(Dispatchers.IO) {
                delay(DETECTION_BOOT_DELAY_MS.milliseconds)
                var detectionActive = true

                while (isActive && detectionActive) {
                    try {
                        val buffer = ShortArray(actualBufferSize)
                        val readResult = synchronized(audioRecorderLock) {
                            val recorder = audioRecorder
                            if (recorder == null || recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                                AudioRecord.ERROR_INVALID_OPERATION
                            } else {
                                recorder.read(buffer, 0, buffer.size)
                            }
                        }

                        if (readResult > 0) {
                            if (detectWhistle(buffer)) {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastClapTime <= WHISTLE_PAIR_WINDOW_MS) {
                                    clapCounter++
                                    if (clapCounter >= 2 && !isClapDetected) {
                                        isClapDetected = true
                                        onDetected()
                                    }
                                } else {
                                    clapCounter = 1
                                }
                                lastClapTime = currentTime
                            } else {
                                isClapDetected = false
                                if (System.currentTimeMillis() - lastClapTime > CLAP_RESET_WINDOW_MS) {
                                    clapCounter = 0
                                }
                            }
                        } else if (readResult == AudioRecord.ERROR_INVALID_OPERATION) {
                            detectionActive = false
                            restartWhistleDetection(onDetected)
//                            withContext(Dispatchers.Main) {
//                                restartWhistleDetection(onDetected)
//                            }
                        }
                    } catch (e: Exception) {
                        DetectorLog.e(TAG, "startWhistleDetection loop: ${e.message}", e)
                        detectionActive = false
                    }

                    delay(100.milliseconds)
                }
            }
        } catch (e: Exception) {
            DetectorLog.e(TAG, "startWhistleDetection: ${e.message}", e)
            cleanupAudioResources()
        }
    }

    fun stop() {
        cleanupAudioResources()
    }

    private fun cleanupAudioResources() {
        try {
            detectionJob?.cancel()
            detectionJob = null

            synchronized(audioRecorderLock) {
                audioRecorder?.let { recorder ->
                    if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        recorder.stop()
                    }
                    recorder.release()
                }
                audioRecorder = null
            }
        } catch (e: Exception) {
            DetectorLog.e(TAG, "cleanupAudioResources: ${e.message}", e)
        }
    }

    private fun restartWhistleDetection(onDetected: suspend () -> Unit) {
        cleanupAudioResources()
        coroutineScope.launch(Dispatchers.IO) {
            delay(WHISTLE_RESTART_DELAY_MS.milliseconds)
            startWhistleDetection(onDetected)
        }
//        mainHandler.postDelayed(
//            { startWhistleDetection(onDetected) },
//            WHISTLE_RESTART_DELAY_MS
//        )
    }

    private fun detectWhistle(buffer: ShortArray): Boolean {
        return try {
            val fftSize = nextPowerOfTwo(buffer.size)
            val real = DoubleArray(fftSize)
            val imag = DoubleArray(fftSize)

            for (i in buffer.indices) {
                real[i] = buffer[i] * (0.5 - 0.5 * cos(2 * Math.PI * i / buffer.size))
            }

            fft(real, imag)

            val magnitude = DoubleArray(fftSize / 2)
            for (i in magnitude.indices) {
                magnitude[i] = sqrt(real[i].pow(2) + imag[i].pow(2))
            }

            val startFreqIndex = (1800 * fftSize) / sampleRate
            val endFreqIndex = (2800 * fftSize) / sampleRate

            var maxAmp = 0.0
            var maxIndex = -1

            for (i in startFreqIndex..endFreqIndex) {
                if (i < magnitude.size && magnitude[i] > maxAmp) {
                    maxAmp = magnitude[i]
                    maxIndex = i
                }
            }

            if (maxIndex == -1 || maxAmp < 30000) return false

            var peakWidth = 0
            for (i in maxIndex - 5..maxIndex + 5) {
                if (i == maxIndex || i !in magnitude.indices) continue
                if (magnitude[i] > maxAmp * 0.4) peakWidth++
            }
            if (peakWidth > 8) return false

            val freq = maxIndex * sampleRate / fftSize
            freq in 1800..2800
        } catch (e: Exception) {
            DetectorLog.e(TAG, "detectWhistle: ${e.message}", e)
            false
        }
    }

    private fun nextPowerOfTwo(n: Int): Int {
        var power = 1
        while (power < n) power *= 2
        return power
    }

    private fun fft(real: DoubleArray, imag: DoubleArray) {
        val n = real.size
        if (n <= 1) return

        val evenReal = DoubleArray(n / 2)
        val evenImag = DoubleArray(n / 2)
        val oddReal = DoubleArray(n / 2)
        val oddImag = DoubleArray(n / 2)

        for (i in 0 until n / 2) {
            evenReal[i] = real[2 * i]
            evenImag[i] = imag[2 * i]
            oddReal[i] = real[2 * i + 1]
            oddImag[i] = imag[2 * i + 1]
        }

        fft(evenReal, evenImag)
        fft(oddReal, oddImag)

        for (k in 0 until n / 2) {
            val angle = -2.0 * Math.PI * k / n
            val cos = cos(angle)
            val sin = sin(angle)

            val tReal = cos * oddReal[k] - sin * oddImag[k]
            val tImag = sin * oddReal[k] + cos * oddImag[k]

            real[k] = evenReal[k] + tReal
            imag[k] = evenImag[k] + tImag
            real[k + n / 2] = evenReal[k] - tReal
            imag[k + n / 2] = evenImag[k] - tImag
        }
    }
}
