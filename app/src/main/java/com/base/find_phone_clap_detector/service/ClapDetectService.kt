package com.base.find_phone_clap_detector.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.annotation.Keep
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.managers.PreferenceManager
import com.base.find_phone_clap_detector.managers.PreferenceManager.Key
import com.base.find_phone_clap_detector.myApplication.MyApplication
import com.base.find_phone_clap_detector.ui.activities.StopSoundAvtivity
import com.base.find_phone_clap_detector.utils.AppStateTracker
import com.base.find_phone_clap_detector.utils.CallUITrigger
import com.base.find_phone_clap_detector.utils.Constants
import com.base.find_phone_clap_detector.utils.SensorBasedPhoneInHandDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sqrt

@Keep
class ClapDetectService : Service(), SensorEventListener {

    private companion object {
        private const val TAG = "CLAP_DETECTOR_SERVICE"
        private const val IN_HAND_TAG = "InHandCheck"
        private const val FLASH_TAG = "FlashFix"
        private const val STOP_SOUND_ACTION = "com.base.find_phone_clap_detector.STOP_SOUND_ACTION"
        private const val NOTIFICATION_CHANNEL_ID = "ClapDetectionChannel"
        private const val NOTIFICATION_ID = 1
        private const val CLAP_PEAK_THRESHOLD = 16000
        private const val FRAME_DELAY_MS = 100L
        private const val CLAP_RESET_WINDOW_MS = 2000L
        private const val CLAP_PAIR_WINDOW_MS = 800L
        private const val WHISTLE_PAIR_WINDOW_MS = 1000L
        private const val WHISTLE_RESTART_DELAY_MS = 1000L
        private const val DETECTION_BOOT_DELAY_MS = 3000L
        private const val SENSOR_ACTIVATION_DELAY_MS = 5000L
        private const val SPEECH_RESTART_DELAY_MS = 600L
        private const val RECOGNIZER_BUSY_RESTART_DELAY_MS = 1000L
        private const val TORCH_RETRY_DELAY_MS = 150L
    }

    private var sensorHandler: Handler? = null
    private var sensorRunnable: Runnable? = null
    private var isServiceActive = true

    private var isTouchDetectionReady = false
    private val audioSource = MediaRecorder.AudioSource.MIC
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private var bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private val detectorThreshold = 23500
    private var isClapDetected = false

    private var audioRecorder: AudioRecord? = null
    private val audioRecorderLock = Any()

    private var clapCounter = 0
    private var lastClapTime = System.currentTimeMillis()
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var uri = "null".toUri()

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var clapJob: Job? = null

    // flash management
    private var isFlashlightOn = false
    private var isDiscoModeOn = false
    private lateinit var cameraManager: CameraManager
    private var cameraId: String = ""
    private lateinit var discoThread: Thread
    private var flashlightJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var flashlightRetryJob: Job? = null
    private var sensorEventJob: Job? = null

    private val vibrator by lazy {
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private var vibrationJob: Job? = null

    // flash modes prefs
    private var isFlashDefault = true
    private var isFlashDisco = false
    private var isFlashSos = false

    private var isDisableWhenUsingPhone = false

    // vibration modes prefs
    private var isVibrationDefault = true
    private var isVibrationStrong = false
    private var isVibrationHeart = false

    // vibration & flash active for service
    private var isVibrationActive = true
    private var isFlashActive = true

    // play Time of Service
    private var timerSound = 45000

    private var checkLoop = true
    private var stopSoundJob: Job? = null

    //Passcode
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null

    // Don't touch
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var isInPocket = false

    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var isFirstRead = true
    private var isCoreSetupDone = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Service created")

        try {
            Log.d(TAG, "onCreate: Creating foreground notification")
            val notification = createNotification()
            Log.d(TAG, "onCreate: Starting foreground service with fallback strategy")
            val foregroundStarted = startForegroundSafely(notification)
            if (!foregroundStarted) {
                Log.e(TAG, "onCreate: Foreground startup failed. Stopping service safely.")
                stopSelf()
                return
            }
            Log.d(TAG, "onCreate: Foreground service started safely")
        } catch (e: Exception) {
            Log.e(TAG, "onCreate: Failed to enter foreground", e)
            stopSelf()
            return
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: Service started with intent: $intent")

        // Stop request comes first
        if (intent?.getBooleanExtra("stop", false) == true) {
            Log.d(TAG, "onStartCommand: Stop request received. Stopping service...")
            cleanupAndStop()
            return START_NOT_STICKY
        }

        try {
            ensureCoreSetup()
            Constants.ACTIVE_SERVICE_TYPE = Constants.SERVICE_TYPE
            Log.d(TAG, "onStartCommand: Active mode is ${getServiceModeLabel(Constants.SERVICE_TYPE)}")
            coroutineScope.launch {
                delay(DETECTION_BOOT_DELAY_MS)
                startActiveDetectionMode()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }

        // Never restart automatically
        return START_NOT_STICKY
    }

    private fun ensureCoreSetup() {
        if (isCoreSetupDone) return

        loadRuntimePrefs()
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        if (cameraId.isBlank()) {
            cameraId = getCameraIdWithFlash() ?: ""
            Log.d(TAG, "Camera ID with flash: $cameraId")
        }

        initializeMediaPlayer()
        logRuntimePreferences("onStartCommand")

        val filter = IntentFilter(STOP_SOUND_ACTION)
        ContextCompat.registerReceiver(
            this,
            stopSoundReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        Log.d(TAG, "ensureCoreSetup: Stop sound receiver registered")
        isCoreSetupDone = true
    }

    private fun loadRuntimePrefs() {
        val preferenceManager = MyApplication.mInstance.preferenceManager
        uri = preferenceManager.getString(PreferenceManager.Key.appliedSoundUri, "null").toUri()
        isFlashDefault = preferenceManager.getBoolean(PreferenceManager.Key.isFlashDefault, true)
        isFlashDisco = preferenceManager.getBoolean(PreferenceManager.Key.isFlashDisco, false)
        isFlashSos = preferenceManager.getBoolean(PreferenceManager.Key.isFlashSos, false)
        isDisableWhenUsingPhone =
            preferenceManager.getBoolean(PreferenceManager.Key.disableWhenUsingPhone, false)
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
        checkLoop = preferenceManager.getBoolean(Key.checkLoop, true)
    }


    override fun onDestroy() {
        Log.d(TAG, "onDestroy: Cleaning up service")

        try {
            coroutineScope.cancel()
            clapJob?.cancel()
            stopSoundJob?.cancel()
            flashlightJob?.cancel()
            flashlightRetryJob?.cancel()
            vibrationJob?.cancel()
            sensorEventJob?.cancel()
            sensorHandler?.removeCallbacksAndMessages(null)
            mainHandler.removeCallbacksAndMessages(null)

            speechRecognizer?.destroy()
            stopDontTouchDetection()
            stopClapDetection()
            stopFlashMode()
            stopVibration()
            releaseMediaPlayer()

            unregisterReceiverSafe(stopSoundReceiver)

            isServiceActive = false
            isPlaying = false

            Log.d(TAG, "onDestroy: Service stopped successfully")

        } catch (e: Exception) {
            Log.e(TAG, "onDestroy error", e)
        }

        super.onDestroy()
    }


    private fun cleanupAndStop() {
        try {
            coroutineScope.cancel()
            clapJob?.cancel()
            stopSoundJob?.cancel()
            flashlightJob?.cancel()
            flashlightRetryJob?.cancel()
            vibrationJob?.cancel()
            sensorEventJob?.cancel()

            sensorHandler?.removeCallbacksAndMessages(null)
            mainHandler.removeCallbacksAndMessages(null)
            stopClapDetection()
            stopFlashMode()
            stopVibration()

            // stop audio
            stopAudio()

            unregisterReceiverSafe(stopSoundReceiver)
            releaseMediaPlayer()

            isPlaying = false
            isServiceActive = false

            // Clear prefs to prevent re-trigger
            MyApplication.mInstance.preferenceManager.put(Key.isClapDetected, false)
            MyApplication.mInstance.preferenceManager.put(Key.isDetectorActive, false)

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            Log.d(TAG, "cleanupAndStop: Service fully stopped and cleaned")
        } catch (e: Exception) {
            Log.e(TAG, "cleanupAndStop error", e)
        }
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startForegroundSafely(notification: Notification): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val preferredType = getForegroundServiceTypeForMode()
                try {
                    startForeground(NOTIFICATION_ID, notification, preferredType)
                    Log.d(TAG, "startForegroundSafely: started with preferred type=$preferredType")
                    true
                } catch (security: SecurityException) {
                    Log.w(
                        TAG,
                        "startForegroundSafely: preferred type failed, trying connectedDevice fallback",
                        security
                    )
                    try {
                        startForeground(
                            NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                        )
                        Log.d(TAG, "startForegroundSafely: started with connectedDevice fallback")
                        true
                    } catch (fallbackError: Exception) {
                        Log.w(
                            TAG,
                            "startForegroundSafely: typed fallback failed, trying untyped fallback",
                            fallbackError
                        )
                        startForeground(NOTIFICATION_ID, notification)
                        Log.d(TAG, "startForegroundSafely: started with untyped fallback")
                        true
                    }
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
                Log.d(TAG, "startForegroundSafely: started for pre-Q device")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "startForegroundSafely: all startup strategies failed", e)
            false
        }
    }

    private fun getForegroundServiceTypeForMode(): Int {
        return when (Constants.SERVICE_TYPE) {
            Constants.BY_CLAP, Constants.BY_WHISTLE, Constants.BY_PASSCODE ->
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE

            else -> ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        }
    }

    private fun startActiveDetectionMode() {
        Log.d(TAG, "startActiveDetectionMode: preparing ${getServiceModeLabel(Constants.SERVICE_TYPE)}")
        when (Constants.SERVICE_TYPE) {
            Constants.BY_CLAP -> startClapDetection("clap")
            Constants.BY_WHISTLE -> startWhistleDetection()
            Constants.BY_PASSCODE -> startPasscodeDetection()
            Constants.DONT_TOUCH, Constants.POCKET_MODE -> dontTouchPhone()
        }
    }

    private fun logRuntimePreferences(source: String) {
        Log.d(
            TAG,
            "$source: mode=${getServiceModeLabel(Constants.SERVICE_TYPE)}, flashActive=$isFlashActive, vibrationActive=$isVibrationActive, loop=$checkLoop, timerSound=$timerSound, disableWhenUsingPhone=$isDisableWhenUsingPhone"
        )
    }

    private fun getServiceModeLabel(mode: String): String {
        return when (mode) {
            Constants.BY_CLAP -> "By Clap"
            Constants.BY_WHISTLE -> "By Whistle"
            Constants.BY_PASSCODE -> "By Passcode"
            Constants.DONT_TOUCH -> "Don't Touch"
            Constants.POCKET_MODE -> "Pocket Mode"
            else -> "Unknown($mode)"
        }
    }

    private fun initializeMediaPlayer() {
        releaseMediaPlayer()
        try {
            mediaPlayer = if (uri.toString() == "null") {
                Log.d(TAG, "initializeMediaPlayer: loading default sound")
                MediaPlayer.create(applicationContext, R.raw.doorbell_sound)
            } else {
                Log.d(TAG, "initializeMediaPlayer: loading custom sound")
                MediaPlayer().apply {
                    setDataSource(applicationContext, uri)
                    prepare()
                }
            }?.apply {
                isLooping = checkLoop
                setOnCompletionListener {
                    if (checkLoop) {
                        Log.d(TAG, "MediaPlayer: Loop ON → restarting sound automatically.")
                        start()
                    } else {
                        Log.d(TAG, "MediaPlayer: Loop OFF → continuing until timer completes.")
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
            Log.e(TAG, "initializeMediaPlayer: failed to prepare player", e)
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
                Log.w(TAG, "releaseMediaPlayer: stop failed: ${e.message}")
            }

            try {
                player.reset()
            } catch (e: Exception) {
                Log.w(TAG, "releaseMediaPlayer: reset failed: ${e.message}")
            }

            try {
                player.release()
            } catch (e: Exception) {
                Log.w(TAG, "releaseMediaPlayer: release failed: ${e.message}")
            }
        }
        mediaPlayer = null
        isPlaying = false
    }

    private fun createNotification(): Notification {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Clap Detection Service",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(notificationChannel)

        val notificationIntent = Intent(this, StopSoundAvtivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Clap Detection Service")
            .setContentText("Running")
            .setSmallIcon(R.drawable.icon_save_apply)
            .setContentIntent(pendingIntent)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun startClapDetection(clap: String) {
        Log.d(TAG, "startClapDetection: initializing for trigger=$clap")
        try {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e(TAG, "No RECORD_AUDIO permission")
                return
            }

            bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            if (bufferSize <= 0) {
                Log.e(TAG, "Invalid buffer size: $bufferSize")
                return
            }

            val readBufferSize = maxOf(bufferSize, 2048)
            audioRecorder =
                AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, readBufferSize)
            if (audioRecorder?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord not initialized")
                return
            }

            audioRecorder?.startRecording()
            Log.d(TAG, "startClapDetection: recorder started with buffer=$readBufferSize")

            clapJob?.cancel()
            clapJob = coroutineScope.launch(Dispatchers.Default) {
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

                        // Use peak detection (clap is a spike)
                        if (peak >= CLAP_PEAK_THRESHOLD) {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastClapTime <= CLAP_PAIR_WINDOW_MS) {
                                clapCounter++
                                if (clapCounter >= 2 && !isClapDetected) {
                                    isClapDetected = true
                                    Log.d(TAG, "startClapDetection: confirmed double trigger for $clap")
                                    if (clap == "clap") {
                                        handleAppState()
                                    } else {
                                        triggerFindPhoneAction()
                                    }
                                }
                            } else {
                                clapCounter = 1
                            }
                            lastClapTime = currentTime
                        } else {
                            if (System.currentTimeMillis() - lastClapTime > CLAP_RESET_WINDOW_MS) {
                                clapCounter = 0
                                isClapDetected = false
                            }
                        }
                    }
                    delay(FRAME_DELAY_MS)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun handleAppState() {
        Log.d(IN_HAND_TAG, "handleAppState: checking app foreground state")

        if (MyApplication.isAppInitialized()) {
            val isForeground = AppStateTracker.isInForeground
            Log.d(IN_HAND_TAG, "handleAppState: app initialized, isForeground=$isForeground")

            if (!isForeground) {
                Log.d(IN_HAND_TAG, "handleAppState: app in background, continuing with background flow")
                CallUITrigger.shouldDialog.postValue(false)
                handleClap(isForeground = false)
            } else {
                Log.d(IN_HAND_TAG, "handleAppState: app in foreground, continuing with in-app flow")
                handleClap(isForeground = true)
            }
        } else {
            Log.d(
                IN_HAND_TAG,
                "handleAppState: app not initialized yet, treating as background"
            )
            CallUITrigger.shouldDialog.postValue(false)
            handleClap(isForeground = false)
        }
    }

    private suspend fun handleClap(isForeground: Boolean) {
        Log.d(IN_HAND_TAG, "handleClap: evaluating trigger, isForeground=$isForeground")
        val manager = getSystemService(AUDIO_SERVICE) as AudioManager

        if (isDisableWhenUsingPhone) {
            if (isScreenOn()) {
                Log.d(IN_HAND_TAG, "handleClap: ignored because screen is on and disableWhenUsingPhone is enabled")
                return
            } else {
                Log.d(IN_HAND_TAG, "handleClap: screen is off, trigger is allowed")
            }
        }

        if (!manager.isMusicActive) {
            Log.d(IN_HAND_TAG, "handleClap: no music active, checking whether phone is already in hand")

            val phoneInHandDetector = SensorBasedPhoneInHandDetector(this)
            val isInHand = phoneInHandDetector.isPhoneInHand()
            Log.d(
                IN_HAND_TAG,
                "handleClap: sensor result isInHand=$isInHand, isForeground=$isForeground"
            )

            if (isInHand) {
                if (isForeground) {
                    Log.i(IN_HAND_TAG, "handleClap: phone is in hand while app is foreground, showing in-app dialog")
                    withContext(Dispatchers.Main) {
                        CallUITrigger.shouldDialog.postValue(true)
                    }
                } else {
                    Log.i(IN_HAND_TAG, "handleClap: phone is in hand while app is background, launching alert UI")
                    startAlertActions("clap_in_hand_background")
                    withContext(Dispatchers.Main) {
                        launchStopSoundActivity(clearTop = true)
                        CallUITrigger.shouldDialog.postValue(true)
                    }
                }
                return
            }

            Log.i(IN_HAND_TAG, "handleClap: phone not in hand, starting normal alert flow")
            startAlertActions("clap_not_in_hand")

            withContext(Dispatchers.Main) {
                MyApplication.mInstance.preferenceManager.put(
                    PreferenceManager.Key.isClapDetected,
                    true
                )
                launchStopSoundActivity(clearTop = false)
            }
        } else {
            Log.w(IN_HAND_TAG, "handleClap: music is active, skipping trigger")
        }
    }

    private fun isScreenOn(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return powerManager.isInteractive
    }


    private fun startFlashVibration() {
        Log.d(TAG, "startFlashVibration: flashActive=$isFlashActive, vibrationActive=$isVibrationActive")
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

    private fun stopClapDetection() {
        try {
            synchronized(audioRecorderLock) {
                audioRecorder?.stop()
                audioRecorder?.release()
                audioRecorder = null
            }
            stopAudio()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun stopAudio() {
        Log.d(TAG, "stopAudio: Stopping playback")
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    player.seekTo(0)
                    stopVibration()
                }
                isPlaying = false
                Log.d(TAG, "stopAudio: MediaPlayer playback stopped safely")
            }
        } catch (e: Exception) {
            Log.d(TAG, "stopAudio: Exception caught: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun playAudio() {
        if (!isPlaying) {
            try {
                if (mediaPlayer == null) {
                    Log.d(TAG, "playAudio: media player missing, preparing it now")
                    initializeMediaPlayer()
                }
                mediaPlayer?.isLooping = checkLoop
                mediaPlayer?.start()
                isPlaying = true
                Log.d(TAG, "playAudio: playback started, loop=$checkLoop")
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }


    private fun startDefFlashMode() {
        Log.d(TAG, "startDefFlashMode: starting default flashlight pattern")
        stopFlashMode()
        flashlightJob = CoroutineScope(Dispatchers.Default).launch {
            isDiscoModeOn = true
            while (isDiscoModeOn) {
                toggleFlashlight()
                delay(800)
            }
        }
    }

    private fun startDiscoFlashMode() {
        Log.d(TAG, "startDiscoFlashMode: starting disco flashlight pattern")
        stopFlashMode()
        flashlightJob = CoroutineScope(Dispatchers.Default).launch {
            isDiscoModeOn = true
            while (isDiscoModeOn) {
                toggleFlashlight()
                delay(100)
            }
        }
    }

    private fun startSosFlashMode() {
        Log.d(TAG, "startSosFlashMode: starting SOS flashlight pattern")
        stopFlashMode()
        flashlightJob = CoroutineScope(Dispatchers.Default).launch {
            isDiscoModeOn = true
            while (isDiscoModeOn) {
                toggleFlashlight()
                delay(400)
            }
        }
    }

    private fun stopFlashMode() {
        try {
            Log.d(TAG, "stopFlashMode: stopping flashlight pattern")
            isDiscoModeOn = false
            isFlashlightOn = false
            flashlightRetryJob?.cancel()

            if (::discoThread.isInitialized && discoThread.isAlive) {
                try {
                    discoThread.interrupt()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }

            turnOffFlashlight()

        } catch (e: Exception) {
            e.printStackTrace()
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
            e.printStackTrace()
        }

    }

    @Suppress("MissingPermission")
    private fun turnOnFlashlight() {
        try {
            // 🧩 Step 1: Verify hardware
            if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                Log.e(FLASH_TAG, "turnOnFlashlight: device does not support flashlight")
                return
            }

            // 🧩 Step 2: Reinitialize CameraManager fresh each time
            cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

            val validCameraId = getCameraIdWithFlash() ?: run {
                Log.e(FLASH_TAG, "turnOnFlashlight: no valid camera with flash found")
                return
            }

            setTorchModeWithRetry(validCameraId, enabled = true)

        } catch (e: Exception) {
            Log.e(FLASH_TAG, "turnOnFlashlight: exception ${e.message}")
        }
    }

    @Suppress("MissingPermission")
    private fun turnOffFlashlight() {
        try {
            // 🧩 Step 1: Always refresh CameraManager
            cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

            val validCameraId = getCameraIdWithFlash() ?: run {
                Log.e(FLASH_TAG, "turnOffFlashlight: no valid camera with flash found")
                return
            }

            setTorchModeWithRetry(validCameraId, enabled = false)

        } catch (e: Exception) {
            Log.e(FLASH_TAG, "turnOffFlashlight: exception ${e.message}")
        }
    }

    private fun setTorchModeWithRetry(validCameraId: String, enabled: Boolean, attempt: Int = 0) {
        mainHandler.post {
            try {
                cameraManager.setTorchMode(validCameraId, enabled)
                Log.d(
                    FLASH_TAG,
                    "setTorchModeWithRetry: flashlight ${if (enabled) "ON" else "OFF"} (cameraId=$validCameraId, attempt=${attempt + 1})"
                )
            } catch (e: Exception) {
                if (attempt >= 2 || !isServiceActive) {
                    Log.e(
                        FLASH_TAG,
                        "setTorchModeWithRetry: failed to turn ${if (enabled) "ON" else "OFF"} after ${attempt + 1} attempts: ${e.message}"
                    )
                } else {
                    Log.e(
                        FLASH_TAG,
                        "setTorchModeWithRetry: retry ${attempt + 1} failed while turning ${if (enabled) "ON" else "OFF"}: ${e.message}"
                    )
                    flashlightRetryJob?.cancel()
                    flashlightRetryJob = coroutineScope.launch {
                        delay(TORCH_RETRY_DELAY_MS)
                        setTorchModeWithRetry(validCameraId, enabled, attempt + 1)
                    }
                }
            }
        }
    }

    private fun getCameraIdWithFlash(): String? {
        return try {
            val manager = getSystemService(CAMERA_SERVICE) as CameraManager
            for (id in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(id)
                val hasFlash =
                    characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    Log.d(FLASH_TAG, "getCameraIdWithFlash: found back camera with flash, id=$id")
                    return id
                }
            }
            Log.w(FLASH_TAG, "getCameraIdWithFlash: no back camera with flash found")
            null
        } catch (e: Exception) {
            Log.e(FLASH_TAG, "getCameraIdWithFlash: exception ${e.message}")
            null
        }
    }


    private fun startDefaultVibration() {
        vibrationJob?.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrationJob = CoroutineScope(Dispatchers.Main).launch {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        1000,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
                delay(1000)
                // loop by re-calling (only if still active)
                if (isActive) startDefaultVibration()
            }
        } else {
            vibrator.vibrate(1000)
        }
    }

    private fun unregisterReceiverSafe(receiver: BroadcastReceiver) {
        try {
            unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Receiver not registered: ${e.message}")
        }
    }

    private fun startStrongVibration() {
        vibrationJob?.cancel()
        val pattern = longArrayOf(0, 500, 500, 500, 500, 500)
        vibrationJob = CoroutineScope(Dispatchers.Main).launch {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            delay(1000)
            if (isActive) startStrongVibration()
        }
    }

    private fun startHeartbeatVibration() {
        vibrationJob?.cancel()
        val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
        vibrationJob = CoroutineScope(Dispatchers.Main).launch {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            delay(1000)
            if (isActive) startHeartbeatVibration()
        }
    }


    private fun stopVibration() {
        try {
            vibrator.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "stopVibration: cancel() threw: ${e.message}")
        }
        vibrationJob?.cancel()
        vibrationJob = null
    }


    private fun startTimer() {
        stopSoundJob?.cancel()

        stopSoundJob = coroutineScope.launch {
            if (checkLoop) {
                Log.d(TAG, "startTimer: loop mode enabled, timer will not stop playback")
                return@launch
            }

            Log.d(TAG, "startTimer: timer mode active, stopping playback after $timerSound ms")
            delay(timerSound.toLong())

            stopAllAlertOutputs("timer_completed")
        }
    }

    private fun startAlertActions(reason: String) {
        Log.d(TAG, "startAlertActions: reason=$reason")
        playAudio()
        startFlashVibration()
        startTimer()
    }

    private fun stopAllAlertOutputs(reason: String) {
        Log.d(TAG, "stopAllAlertOutputs: reason=$reason")
        stopAudio()
        stopFlashMode()
        stopVibration()
    }

    private fun launchStopSoundActivity(clearTop: Boolean) {
        val intent = Intent(baseContext, StopSoundAvtivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (clearTop) {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
        startActivity(intent)
    }



    private val stopSoundReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "stopSoundReceiver: received stop broadcast")
            stopAllAlertOutputs("stop_broadcast")
            stopDontTouchDetection()
            this@ClapDetectService.stopForeground(STOP_FOREGROUND_REMOVE)
            this@ClapDetectService.stopSelf()
            Log.d(TAG, "stopSoundReceiver: Service stopping itself via broadcast")
        }
    }

    /**
     * Find by whistle
     */

    @SuppressLint("MissingPermission")
    private fun startWhistleDetection() {
        Log.d(TAG, "startWhistleDetection: initializing")
        try {
            cleanupAudioResources()

            bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            if (bufferSize <= 0) {
                Log.e(TAG, "Invalid buffer size: $bufferSize")
                return
            }

            val actualBufferSize = maxOf(bufferSize, 4096)

            audioRecorder =
                AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, actualBufferSize)

            if (audioRecorder?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord not initialized - state: ${audioRecorder?.state}")
                cleanupAudioResources()
                return
            }

            audioRecorder?.startRecording()
            Log.d(TAG, "startWhistleDetection: recorder started successfully with buffer=$actualBufferSize")

            clapJob?.cancel()
            clapJob = coroutineScope.launch(Dispatchers.IO) {
                delay(DETECTION_BOOT_DELAY_MS)
                Log.d(TAG, "startWhistleDetection: active after stabilization delay")

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
                                Log.d(TAG, "startWhistleDetection: whistle candidate detected")
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastClapTime <= WHISTLE_PAIR_WINDOW_MS) {
                                    clapCounter++
                                    if (clapCounter >= 2 && !isClapDetected) {
                                        isClapDetected = true
                                        Log.d(TAG, "startWhistleDetection: confirmed double whistle, triggering alert")
                                        triggerFindPhoneAction()
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
                            Log.e(TAG, "AudioRecord invalid operation - restarting detection")
                            detectionActive = false
                            withContext(Dispatchers.Main) {
                                restartWhistleDetection()
                            }
                            break
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in whistle detection loop: ${e.message}")
                        detectionActive = false
                        break
                    }

                    delay(100)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting whistle detection: ${e.message}")
            e.printStackTrace()
            cleanupAudioResources()
        }
    }

    private fun cleanupAudioResources() {
        try {
            clapJob?.cancel()
            clapJob = null

            synchronized(audioRecorderLock) {
                audioRecorder?.let { recorder ->
                    if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        recorder.stop()
                    }
                    recorder.release()
                }
                audioRecorder = null
            }

            Log.d(TAG, "Audio resources cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up audio resources: ${e.message}")
        }
    }

    private fun restartWhistleDetection() {
        Log.d(TAG, "restartWhistleDetection: scheduling restart")
        cleanupAudioResources()

        Handler(Looper.getMainLooper()).postDelayed({
            startWhistleDetection()
        }, WHISTLE_RESTART_DELAY_MS)
    }

    private fun detectWhistle(buffer: ShortArray): Boolean {
        try {
            val fftSize = nextPowerOfTwo(buffer.size)
            val real = DoubleArray(fftSize)
            val imag = DoubleArray(fftSize)

            // Apply Hanning window
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

            if (maxIndex == -1 || maxAmp < 30000) {
                return false
            }

            var peakWidth = 0
            for (i in maxIndex - 5..maxIndex + 5) {
                if (i == maxIndex || i !in magnitude.indices) continue
                if (magnitude[i] > maxAmp * 0.4) peakWidth++
            }
            if (peakWidth > 8) return false

            val freq = maxIndex * sampleRate / fftSize

            if (freq in 1800..2800) {
                Log.d(TAG, "Valid whistle detected: $freq Hz, amplitude: $maxAmp")
                return true
            }

            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error in detectWhistle: ${e.message}")
            return false
        }
    }


    private fun nextPowerOfTwo(n: Int): Int {
        var power = 1
        while (power < n) power *= 2
        return power
    }

    private fun fft(real: DoubleArray, imag: DoubleArray) {
        val n = real.size
        if (n == 0) return
        if (n and (n - 1) != 0) throw IllegalArgumentException("Array length is not a power of 2")

        val levels = 31 - Integer.numberOfLeadingZeros(n)

        val cosTable = DoubleArray(n / 2)
        val sinTable = DoubleArray(n / 2)
        for (i in 0 until n / 2) {
            cosTable[i] = kotlin.math.cos(2 * Math.PI * i / n)
            sinTable[i] = kotlin.math.sin(2 * Math.PI * i / n)
        }

        // Bit-reversed addressing permutation
        for (i in 0 until n) {
            val j = Integer.reverse(i).ushr(32 - levels)
            if (j > i) {
                val tempReal = real[i]
                val tempImag = imag[i]
                real[i] = real[j]
                imag[i] = imag[j]
                real[j] = tempReal
                imag[j] = tempImag
            }
        }

        // Cooley-Tukey decimation-in-time radix-2 FFT
        var size = 2
        while (size <= n) {
            val halfSize = size / 2
            val tableStep = n / size
            for (i in 0 until n step size) {
                var k = 0
                for (j in i until i + halfSize) {
                    val l = j + halfSize
                    val tReal = real[l] * cosTable[k] + imag[l] * sinTable[k]
                    val tImag = -real[l] * sinTable[k] + imag[l] * cosTable[k]
                    real[l] = real[j] - tReal
                    imag[l] = imag[j] - tImag
                    real[j] += tReal
                    imag[j] += tImag
                    k += tableStep
                }
            }
            size *= 2
        }
    }


    private fun dontTouchPhone() {
        Log.d(TAG, "dontTouchPhone: Initializing sensor detection with delay")
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = when (Constants.SERVICE_TYPE) {
            Constants.DONT_TOUCH -> sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            Constants.POCKET_MODE -> sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
            else -> sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }

        isTouchDetectionReady = false
        isFirstRead = true

        sensorHandler?.removeCallbacks(sensorRunnable ?: Runnable { })

        sensorHandler = Handler(Looper.getMainLooper())
        sensorRunnable = Runnable {
            accelerometer?.also {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
            isTouchDetectionReady = true
            Log.d(TAG, "dontTouchPhone: ${getServiceModeLabel(Constants.SERVICE_TYPE)} detection is now active")
        }

        sensorHandler?.postDelayed(sensorRunnable!!, SENSOR_ACTIVATION_DELAY_MS)
    }

    private fun stopDontTouchDetection() {
        sensorHandler?.removeCallbacks(sensorRunnable ?: Runnable { })
        sensorRunnable = null
        sensorHandler = null

        stopAudio()
        stopVibration()

        sensorManager?.unregisterListener(this)
        isTouchDetectionReady = false
        Log.d(TAG, "stopDontTouchDetection: sensor listener removed")
    }


    override fun onSensorChanged(event: SensorEvent?) {
        if (!isTouchDetectionReady) return
        if (sensorEventJob?.isActive == true) return

        val currentEvent = event ?: return
        val values = currentEvent.values.clone()
        sensorEventJob = coroutineScope.launch(Dispatchers.IO) {

            when (Constants.SERVICE_TYPE) {
                Constants.DONT_TOUCH -> {
                    if (values.size < 3) return@launch
                    val x = currentEvent.values[0]
                    val y = currentEvent.values[1]
                    val z = currentEvent.values[2]

                    if (isFirstRead) {
                        lastX = x
                        lastY = y
                        lastZ = z
                        isFirstRead = false
                    }

                    val deltaX = abs(lastX - x)
                    val deltaY = abs(lastY - y)
                    val deltaZ = abs(lastZ - z)

                    if (deltaX > 1.5f || deltaY > 1.5f || deltaZ > 1.5f) {
                        Log.d(TAG, "onSensorChanged: motion threshold crossed, triggering alert")
                        triggerFindPhoneAction()
                    }

                    lastX = x
                    lastY = y
                    lastZ = z
                }

                Constants.POCKET_MODE -> {
                    if (values.isEmpty()) return@launch
                    val distance = currentEvent.values[0]
                    val maxRange = accelerometer?.maximumRange ?: 0f

                    if (distance < maxRange) {
                        isInPocket = true
                    } else {
                        if (isInPocket) {
                            isInPocket = false
                            Log.d(TAG, "onSensorChanged: pocket mode exit detected, triggering alert")
                            triggerFindPhoneAction()
                        }
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}


    private suspend fun triggerFindPhoneAction() {
        Log.d(IN_HAND_TAG, "triggerFindPhoneAction: evaluating direct alert trigger")

        if (MyApplication.isAppInitialized()) {
            val manager = getSystemService(AUDIO_SERVICE) as AudioManager

            if (isDisableWhenUsingPhone) {
                if (isScreenOn()) {
                    Log.d(IN_HAND_TAG, "triggerFindPhoneAction: ignored because screen is on and disableWhenUsingPhone is enabled")
                    return
                } else {
                    Log.d(IN_HAND_TAG, "triggerFindPhoneAction: screen is off, trigger is allowed")
                }
            }


            if (!manager.isMusicActive) {
                Log.d(
                    IN_HAND_TAG,
                    "triggerFindPhoneAction: no music active, starting alert actions"
                )

                startAlertActions("direct_trigger")

                withContext(Dispatchers.Main) {
                    MyApplication.mInstance.preferenceManager.put(
                        PreferenceManager.Key.isClapDetected,
                        true
                    )
                    launchStopSoundActivity(clearTop = false)
                }
            } else {
                Log.w(
                    IN_HAND_TAG,
                    "triggerFindPhoneAction: music is active, skipping trigger"
                )
            }
        } else {
            Log.d(
                IN_HAND_TAG,
                "triggerFindPhoneAction: app not initialized yet, falling back to clap handler"
            )
            CallUITrigger.shouldDialog.postValue(false)
            handleClap(isForeground = false)
        }
    }


    /**
     * Find by Passcode
     */

    @SuppressLint("MissingPermission")
    private fun startPasscodeDetection() {
        Log.d(TAG, "startPasscodeDetection: Initializing speech recognizer")
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "Speech recognition not available")
            return
        }

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)

                putExtra("android.speech.extra.DICTATION_MODE", true)
                putExtra("android.speech.extra.PROMPT", "")
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(TAG, "Ready for speech")
                }

                override fun onBeginningOfSpeech() {
                    isListening = true
                }

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    isListening = false
                    restartListeningWithDelay()
                }

                override fun onError(error: Int) {
                    isListening = false
                    Log.d(TAG, "Speech error: $error")
                    if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                        stopAndRestartRecognizer()
                    } else {
                        restartListeningWithDelay()
                    }
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val passcode = MyApplication.mInstance.preferenceManager.getString(
                        Key.PASSCODE_STRING,
                        "hello app"
                    )

                    matches?.forEach { result ->
                        Log.d(TAG, "Heard: $result")
                        if (result.contains(passcode, ignoreCase = true)) {
                            Log.d(TAG, "Passcode matched!")
                            clapJob = coroutineScope.launch(Dispatchers.IO) {
                                handleAppState()
                            }
                            return
                        }
                    }

                    restartListeningWithDelay()
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        startListeningSafe()
    }

    private var isListening = false

    private fun startListeningSafe() {
        try {
            if (!isListening) {
                speechRecognizer?.startListening(recognizerIntent)
                isListening = true
            } else {
                Log.d(TAG, "Already listening, skip starting again")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAndRestartRecognizer() {
        try {
            speechRecognizer?.cancel()
            isListening = false
            Handler(Looper.getMainLooper()).postDelayed({
                startListeningSafe()
            }, RECOGNIZER_BUSY_RESTART_DELAY_MS)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun restartListeningWithDelay() {
        try {
            Handler(Looper.getMainLooper()).postDelayed({
                startListeningSafe()
            }, SPEECH_RESTART_DELAY_MS)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}
