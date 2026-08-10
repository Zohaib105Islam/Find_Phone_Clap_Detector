package com.base.find_phone_clap_detector.service.helpers

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.net.toUri
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.managers.PreferenceManager
import com.base.find_phone_clap_detector.myApplication.MyApplication
import com.base.find_phone_clap_detector.service.DetectorLog
import com.base.find_phone_clap_detector.ui.activities.StopSoundAvtivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AlertController(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val mainHandler: Handler
) {
    private companion object {
        private const val TAG = "AlertController"
        private const val FLASH_TAG = "FlashFix"
        private const val TORCH_RETRY_DELAY_MS = 150L
    }

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var isServiceActive = true
    private var uri: Uri = "null".toUri()
    private var isFlashDefault = true
    private var isFlashDisco = false
    private var isFlashSos = false
    private var isVibrationDefault = true
    private var isVibrationStrong = false
    private var isVibrationHeart = false
    private var isVibrationActive = true
    private var isFlashActive = true
    private var timerSound = 45000
    private var checkLoop = true
    private var isFlashlightOn = false
    private var isDiscoModeOn = false
    private var cameraId = ""
    private var flashlightJob: Job? = null
    private var flashlightRetryJob: Job? = null
    private var vibrationJob: Job? = null
    private var stopSoundJob: Job? = null
    private val vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    private var cameraManager: CameraManager? = null

    fun loadRuntimePrefs() {
        val preferenceManager = MyApplication.mInstance.preferenceManager
        uri = preferenceManager.getString(PreferenceManager.Key.appliedSoundUri, "null").toUri()
        isFlashDefault = preferenceManager.getBoolean(PreferenceManager.Key.isFlashDefault, true)
        isFlashDisco = preferenceManager.getBoolean(PreferenceManager.Key.isFlashDisco, false)
        isFlashSos = preferenceManager.getBoolean(PreferenceManager.Key.isFlashSos, false)
        isVibrationDefault =
            preferenceManager.getBoolean(PreferenceManager.Key.isVibrationDefault, true)
        isVibrationStrong =
            preferenceManager.getBoolean(PreferenceManager.Key.isVibrationStrong, false)
        isVibrationHeart =
            preferenceManager.getBoolean(PreferenceManager.Key.isVibrationHeart, false)
        isVibrationActive =
            preferenceManager.getBoolean(PreferenceManager.Key.isVibrationActive, true)
        isFlashActive = preferenceManager.getBoolean(PreferenceManager.Key.isFlashActive, true)
        timerSound = preferenceManager.getInt(PreferenceManager.Key.soundPlayTime, 45000)
        checkLoop = preferenceManager.getBoolean(PreferenceManager.Key.checkLoop, true)
    }

    fun initialize() {
        if (cameraId.isBlank()) {
            cameraId = getCameraIdWithFlash() ?: ""
            DetectorLog.d(TAG, "initialize: Camera ID with flash: $cameraId")
        }
        initializeMediaPlayer()
    }

    fun setServiceActive(isActive: Boolean) {
        isServiceActive = isActive
    }

    fun startAlertActions(reason: String) {
        DetectorLog.d(TAG, "startAlertActions: reason=$reason")
        playAudio()
        startFlashVibration()
        startTimer()
    }

    fun stopAllAlertOutputs(reason: String) {
        DetectorLog.d(TAG, "stopAllAlertOutputs: reason=$reason")
        stopSoundJob?.cancel()
        stopAudio()
        stopFlashMode()
        stopVibration()
    }

    fun stopAudioOnly() {
        stopAudio()
    }

    fun launchStopSoundActivity(clearTop: Boolean) {
        val intent = Intent(context, StopSoundAvtivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (clearTop) {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
        context.startActivity(intent)
    }

    fun release() {
        stopSoundJob?.cancel()
        flashlightJob?.cancel()
        flashlightRetryJob?.cancel()
        vibrationJob?.cancel()
        stopFlashMode()
        stopVibration()
        releaseMediaPlayer()
    }

    private fun initializeMediaPlayer() {
        releaseMediaPlayer()
        try {
            mediaPlayer = if (uri.toString() == "null") {
                DetectorLog.d(TAG, "initializeMediaPlayer: loading default sound")
                MediaPlayer.create(context.applicationContext, R.raw.doorbell_sound)
            } else {
                DetectorLog.d(TAG, "initializeMediaPlayer: loading custom sound")
                MediaPlayer().apply {
                    setDataSource(context.applicationContext, uri)
                    prepare()
                }
            }?.apply {
                isLooping = checkLoop
                setOnCompletionListener {
                    if (checkLoop) {
                        DetectorLog.d(TAG, "MediaPlayer: Loop ON -> restarting sound automatically.")
                        start()
                    } else {
                        DetectorLog.d(TAG, "MediaPlayer: Loop OFF -> continuing until timer completes.")
                        if (stopSoundJob?.isActive == true) {
                            seekTo(0)
                            start()
                        } else {
                            stopAudio()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            DetectorLog.e(TAG, "initializeMediaPlayer: failed to prepare player", e)
            mediaPlayer = null
        }
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
            } catch (e: Exception) {
                DetectorLog.w(TAG, "releaseMediaPlayer: stop failed: ${e.message}")
            }

            try {
                player.reset()
            } catch (e: Exception) {
                DetectorLog.w(TAG, "releaseMediaPlayer: reset failed: ${e.message}")
            }

            try {
                player.release()
            } catch (e: Exception) {
                DetectorLog.w(TAG, "releaseMediaPlayer: release failed: ${e.message}")
            }
        }
        mediaPlayer = null
        isPlaying = false
    }

    private fun startFlashVibration() {
        DetectorLog.d(TAG, "startFlashVibration: flashActive=$isFlashActive, vibrationActive=$isVibrationActive")
        if (isFlashActive) {
            if (isFlashDefault) {
                startDefFlashMode()
            } else if (isFlashDisco) {
                startDiscoFlashMode()
            } else if (isFlashSos) {
                startSosFlashMode()
            }
        }

        if (isVibrationActive) {
            if (isVibrationDefault) {
                startDefaultVibration()
            } else if (isVibrationStrong) {
                startStrongVibration()
            } else if (isVibrationHeart) {
                startHeartbeatVibration()
            }
        }
    }

    private fun stopAudio() {
        DetectorLog.d(TAG, "stopAudio: Stopping playback")
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    player.seekTo(0)
                    stopVibration()
                }
                isPlaying = false
                DetectorLog.d(TAG, "stopAudio: MediaPlayer playback stopped safely")
            }
        } catch (e: Exception) {
            DetectorLog.d(TAG, "stopAudio: Exception caught: ${e.message}")
        }
    }

    private fun playAudio() {
        if (!isPlaying) {
            try {
                if (mediaPlayer == null) {
                    DetectorLog.d(TAG, "playAudio: media player missing, preparing it now")
                    initializeMediaPlayer()
                }
                mediaPlayer?.isLooping = checkLoop
                mediaPlayer?.start()
                isPlaying = true
                DetectorLog.d(TAG, "playAudio: playback started, loop=$checkLoop")
            } catch (e: Exception) {
                DetectorLog.e(TAG, "playAudio: failed", e)
            }
        }
    }

    private fun startDefFlashMode() {
        DetectorLog.d(TAG, "startDefFlashMode: starting default flashlight pattern")
        stopFlashMode()
        flashlightJob = coroutineScope.launch {
            isDiscoModeOn = true
            while (isDiscoModeOn) {
                toggleFlashlight()
                delay(800.milliseconds)
            }
        }
    }

    private fun startDiscoFlashMode() {
        DetectorLog.d(TAG, "startDiscoFlashMode: starting disco flashlight pattern")
        stopFlashMode()
        flashlightJob = coroutineScope.launch {
            isDiscoModeOn = true
            while (isDiscoModeOn) {
                toggleFlashlight()
                delay(100.milliseconds)
            }
        }
    }

    private fun startSosFlashMode() {
        DetectorLog.d(TAG, "startSosFlashMode: starting SOS flashlight pattern")
        stopFlashMode()
        flashlightJob = coroutineScope.launch {
            isDiscoModeOn = true
            while (isDiscoModeOn) {
                toggleFlashlight()
                delay(400.milliseconds)
            }
        }
    }

    private fun stopFlashMode() {
        try {
            DetectorLog.d(TAG, "stopFlashMode: stopping flashlight pattern")
            isDiscoModeOn = false
            isFlashlightOn = false
            flashlightRetryJob?.cancel()
            flashlightJob?.cancel()
            turnOffFlashlight()
        } catch (e: Exception) {
            DetectorLog.e(TAG,e.message?:"Exception occurred while stoping flash")
        }
    }

    private fun toggleFlashlight() {
        try {
            if (isFlashlightOn) {
                turnOffFlashlight()
            } else {
                turnOnFlashlight()
            }
            isFlashlightOn = !isFlashlightOn
        } catch (e: Exception) {
            DetectorLog.e(TAG, "toggleFlashlight: failed", e)
        }
    }

    private fun turnOnFlashlight() {
        try {
            if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                DetectorLog.e(FLASH_TAG, "turnOnFlashlight: device does not support flashlight")
                return
            }

            cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val validCameraId = getCameraIdWithFlash() ?: run {
                DetectorLog.e(FLASH_TAG, "turnOnFlashlight: no valid camera with flash found")
                return
            }
            setTorchModeWithRetry(validCameraId, enabled = true)
        } catch (e: Exception) {
            DetectorLog.e(FLASH_TAG, "turnOnFlashlight: exception ${e.message}")
        }
    }

    private fun turnOffFlashlight() {
        try {
            cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val validCameraId = getCameraIdWithFlash() ?: run {
                DetectorLog.e(FLASH_TAG, "turnOffFlashlight: no valid camera with flash found")
                return
            }
            setTorchModeWithRetry(validCameraId, enabled = false)
        } catch (e: Exception) {
            DetectorLog.e(FLASH_TAG, "turnOffFlashlight: exception ${e.message}")
        }
    }

    private fun setTorchModeWithRetry(validCameraId: String, enabled: Boolean, attempt: Int = 0) {
        try {
            cameraManager?.setTorchMode(validCameraId, enabled)
            DetectorLog.d(
                FLASH_TAG,
                "setTorchModeWithRetry: flashlight ${if (enabled) "ON" else "OFF"} (cameraId=$validCameraId, attempt=${attempt + 1})"
            )
        } catch (e: Exception) {
            if (attempt >= 2 || !isServiceActive) {
                DetectorLog.e(
                    FLASH_TAG,
                    "setTorchModeWithRetry: failed to turn ${if (enabled) "ON" else "OFF"} after ${attempt + 1} attempts: ${e.message}"
                )
            } else {
                DetectorLog.e(
                    FLASH_TAG,
                    "setTorchModeWithRetry: retry ${attempt + 1} failed while turning ${if (enabled) "ON" else "OFF"}: ${e.message}"
                )
                flashlightRetryJob?.cancel()
                flashlightRetryJob = coroutineScope.launch {
                    delay(TORCH_RETRY_DELAY_MS.milliseconds)
                    setTorchModeWithRetry(validCameraId, enabled, attempt + 1)
                }
            }
        }
    }

    private fun getCameraIdWithFlash(): String? {
        return try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            for (id in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(id)
                val hasFlash =
                    characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    DetectorLog.d(FLASH_TAG, "getCameraIdWithFlash: found back camera with flash, id=$id")
                    return id
                }
            }
            DetectorLog.w(FLASH_TAG, "getCameraIdWithFlash: no back camera with flash found")
            null
        } catch (e: Exception) {
            DetectorLog.e(FLASH_TAG, "getCameraIdWithFlash: exception ${e.message}")
            null
        }
    }

    private fun startDefaultVibration() {
        vibrationJob?.cancel()
        vibrationJob = coroutineScope.launch(Dispatchers.Main) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    1000,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
            delay(1.seconds)
            if (isActive) startDefaultVibration()
        }
    }

    private fun startStrongVibration() {
        vibrationJob?.cancel()
        val pattern = longArrayOf(0, 500, 500, 500, 500, 500)
        vibrationJob = coroutineScope.launch(Dispatchers.Main) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            delay(1.seconds)
            if (isActive) startStrongVibration()
        }
    }

    private fun startHeartbeatVibration() {
        vibrationJob?.cancel()
        val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
        vibrationJob = coroutineScope.launch(Dispatchers.Main) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            delay(1.seconds)
            if (isActive) startHeartbeatVibration()
        }
    }

    private fun stopVibration() {
        try {
            vibrator.cancel()
        } catch (e: Exception) {
            DetectorLog.w(TAG, "stopVibration: cancel() threw: ${e.message}")
        }
        vibrationJob?.cancel()
        vibrationJob = null
    }

    private fun startTimer() {
        stopSoundJob?.cancel()
        stopSoundJob = coroutineScope.launch {
            if (checkLoop) {
                DetectorLog.d(TAG, "startTimer: loop mode enabled, timer will not stop playback")
                return@launch
            }

            DetectorLog.d(TAG, "startTimer: timer mode active, stopping playback after $timerSound ms")
            delay(timerSound.toLong().milliseconds)
            stopAllAlertOutputs("timer_completed")
        }
    }
}
