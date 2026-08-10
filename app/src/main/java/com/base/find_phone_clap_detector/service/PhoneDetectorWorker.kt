package com.base.find_phone_clap_detector.service

import android.Manifest
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.base.find_phone_clap_detector.managers.PreferenceManager.Key
import com.base.find_phone_clap_detector.myApplication.MyApplication
import com.base.find_phone_clap_detector.service.helpers.AlertController
import com.base.find_phone_clap_detector.service.helpers.AudioDetector
import com.base.find_phone_clap_detector.service.helpers.ForegroundNotificationFactory
import com.base.find_phone_clap_detector.service.helpers.MotionDetector
import com.base.find_phone_clap_detector.service.helpers.PasscodeDetector
import com.base.find_phone_clap_detector.service.helpers.StopSoundReceiver
import com.base.find_phone_clap_detector.utils.AppStateTracker
import com.base.find_phone_clap_detector.utils.CallUITrigger
import com.base.find_phone_clap_detector.utils.Constants
import com.base.find_phone_clap_detector.utils.SensorBasedPhoneInHandDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

@Keep
class PhoneDetectorWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {

    companion object {
        const val UNIQUE_WORK_NAME = "phone_detector_worker"

        private const val TAG = "PHONE_DETECTOR_WORKER"
        private const val IN_HAND_TAG = "InHandCheck"
        private const val STOP_SOUND_ACTION = "com.base.find_phone_clap_detector.STOP_SOUND_ACTION"
        private const val DETECTION_BOOT_DELAY_MS = 3000L
        private const val KEEP_ALIVE_DELAY_MS = 1000L

        @Volatile
        var isRunning: Boolean = false
            private set
    }

    private val workerJob = SupervisorJob()
    private val coroutineScope = CoroutineScope(workerJob + Dispatchers.Default)
    private var clapJob: Job? = null
    private var detectionBootJob: Job? = null
    private var isStopReceiverRegistered = false
    private var isWorkerActive = true
    private var isDisableWhenUsingPhone = false
    private var isCoreSetupDone = false
    private var stopRequested = false
    private var cleanupStarted = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val audioDetector by lazy {
        AudioDetector(
            context = applicationContext,
            coroutineScope = coroutineScope,
            mainHandler = mainHandler
        )
    }

    private val passcodeDetector by lazy {
        PasscodeDetector(
            context = applicationContext,
            packageName = applicationContext.packageName
        ) {
            clapJob?.cancel()
            clapJob = coroutineScope.launch(Dispatchers.IO) {
                try {
                    handleAppState()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    DetectorLog.e(TAG, "passcodeDetector: failed", e)
                }
            }
        }
    }

    private val motionDetector by lazy {
        MotionDetector(
            context = applicationContext,
            coroutineScope = coroutineScope
        ) {
            try {
                triggerFindPhoneAction()
            } catch (e: Exception) {
                DetectorLog.e(TAG, "motionDetector: triggerFindPhoneAction failed", e)
            }
        }
    }

    private val alertController by lazy {
        AlertController(
            context = applicationContext,
            coroutineScope = coroutineScope,
            mainHandler = mainHandler
        )
    }

    private val foregroundNotificationFactory by lazy {
        ForegroundNotificationFactory(applicationContext)
    }

    private val stopSoundReceiver by lazy {
        StopSoundReceiver {
            try {
                cleanupAndStop()
            } catch (e: Exception) {
                DetectorLog.e(TAG, "stopSoundReceiver: cleanup failed", e)
                stopRequested = true
            }
        }
    }

    override suspend fun doWork(): Result {
        DetectorLog.d(TAG, "doWork: worker starting")

        return try {
            if (!canStartForCurrentMode()) {
                DetectorLog.w(TAG, "doWork: missing requirements for mode=${Constants.SERVICE_TYPE}")
                Result.failure()
            } else {
                setForeground(getForegroundInfo())
                isRunning = true
                stopRequested = false
                isWorkerActive = true
                cleanupStarted = false
                ensureCoreSetup()
                Constants.ACTIVE_SERVICE_TYPE = Constants.SERVICE_TYPE
                DetectorLog.d(TAG, "doWork: active mode is ${getServiceModeLabel(Constants.SERVICE_TYPE)}")
                scheduleDetectionStart()
                keepWorkerAlive()
                Result.success()
            }
        } catch (e: CancellationException) {
            DetectorLog.d(TAG, "doWork: worker cancelled")
            throw e
        } catch (e: Exception) {
            DetectorLog.e(TAG, "doWork: failed", e)
            Result.failure()
        } finally {
            cleanupResources()
            workerJob.cancel()
            isRunning = false
            DetectorLog.d(TAG, "doWork: worker stopped")
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo()
    }

    private suspend fun keepWorkerAlive() {
        while (currentCoroutineContext().isActive && !isStopped && !stopRequested) {
            delay(KEEP_ALIVE_DELAY_MS.milliseconds)
        }
    }

    private fun ensureCoreSetup() {
        if (isCoreSetupDone) return

        loadRuntimePrefs()
        alertController.loadRuntimePrefs()
        alertController.initialize()
        logRuntimePreferences()
        registerStopSoundReceiver()
        isCoreSetupDone = true
    }

    private fun loadRuntimePrefs() {
        val preferenceManager = MyApplication.mInstance.preferenceManager
        isDisableWhenUsingPhone =
            preferenceManager.getBoolean(Key.disableWhenUsingPhone, false)
    }

    private fun logRuntimePreferences() {
        DetectorLog.d(
            TAG,
            "Runtime prefs: mode=${getServiceModeLabel(Constants.SERVICE_TYPE)}, disableWhenUsingPhone=$isDisableWhenUsingPhone"
        )
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notification = foregroundNotificationFactory.create()

        return ForegroundInfo(
            ForegroundNotificationFactory.NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        )
    }

    private fun scheduleDetectionStart() {
        detectionBootJob?.cancel()

        detectionBootJob = coroutineScope.launch {
            try {
                delay(DETECTION_BOOT_DELAY_MS)

                if (!isWorkerActive) {
                    DetectorLog.w(TAG, "scheduleDetectionStart: worker is not active, skipping detection start")
                    return@launch
                }

                startActiveDetectionMode()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DetectorLog.e(TAG, "scheduleDetectionStart: failed", e)
            }
        }
    }

    private fun startActiveDetectionMode() {
        DetectorLog.d(TAG, "startActiveDetectionMode: preparing ${getServiceModeLabel(Constants.SERVICE_TYPE)}")

        try {
            when (Constants.SERVICE_TYPE) {
                Constants.BY_CLAP -> startClapDetection()
                Constants.BY_WHISTLE -> startWhistleDetection()
                Constants.BY_PASSCODE -> startPasscodeDetection()
                Constants.DONT_TOUCH, Constants.POCKET_MODE -> startMotionDetection()
                else -> {
                    DetectorLog.w(TAG, "startActiveDetectionMode: unknown service type=${Constants.SERVICE_TYPE}")
                }
            }
        } catch (e: Exception) {
            DetectorLog.e(TAG, "startActiveDetectionMode: failed", e)
            cleanupAndStop()
        }
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

    private fun startClapDetection() {
        audioDetector.startClapDetection {
            try {
                handleAppState()
            } catch (e: Exception) {
                DetectorLog.e(TAG, "startClapDetection: trigger callback failed", e)
            }
        }
    }

    private suspend fun handleAppState() {
        DetectorLog.d(IN_HAND_TAG, "handleAppState: checking app foreground state")

        if (MyApplication.isAppInitialized()) {
            val isForeground = AppStateTracker.isInForeground
            DetectorLog.d(IN_HAND_TAG, "handleAppState: app initialized, isForeground=$isForeground")

            if (!isForeground) {
                DetectorLog.d(IN_HAND_TAG, "handleAppState: app in background, continuing with background flow")
                CallUITrigger.shouldDialog.postValue(false)
                handleClap(isForeground = false)
            } else {
                DetectorLog.d(IN_HAND_TAG, "handleAppState: app in foreground, continuing with in-app flow")
                handleClap(isForeground = true)
            }
        } else {
            DetectorLog.d(
                IN_HAND_TAG,
                "handleAppState: app not initialized yet, treating as background"
            )
            CallUITrigger.shouldDialog.postValue(false)
            handleClap(isForeground = false)
        }
    }

    private suspend fun handleClap(isForeground: Boolean) {
        DetectorLog.d(IN_HAND_TAG, "handleClap: evaluating trigger, isForeground=$isForeground")
        val manager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (manager == null) {
            DetectorLog.e(IN_HAND_TAG, "handleClap: AudioManager is null")
            return
        }
        if (isDisableWhenUsingPhone) {
            if (isScreenOn()) {
                DetectorLog.d(IN_HAND_TAG, "handleClap: ignored because screen is on and disableWhenUsingPhone is enabled")
                return
            } else {
                DetectorLog.d(IN_HAND_TAG, "handleClap: screen is off, trigger is allowed")
            }
        }

        if (!manager.isMusicActive) {
            DetectorLog.d(IN_HAND_TAG, "handleClap: no music active, checking whether phone is already in hand")

            val phoneInHandDetector = SensorBasedPhoneInHandDetector(applicationContext)
            val isInHand = phoneInHandDetector.isPhoneInHand()
            DetectorLog.d(
                IN_HAND_TAG,
                "handleClap: sensor result isInHand=$isInHand, isForeground=$isForeground"
            )

            if (isInHand) {
                if (isForeground) {
                    DetectorLog.i(IN_HAND_TAG, "handleClap: phone is in hand while app is foreground, showing in-app dialog")
                    withContext(Dispatchers.Main) {
                        CallUITrigger.shouldDialog.postValue(true)
                    }
                } else {
                    DetectorLog.i(IN_HAND_TAG, "handleClap: phone is in hand while app is background, launching alert UI")
                    alertController.startAlertActions("clap_in_hand_background")
                    withContext(Dispatchers.Main) {
                        alertController.launchStopSoundActivity(clearTop = true)
                        CallUITrigger.shouldDialog.postValue(true)
                    }
                }
                return
            }

            DetectorLog.i(IN_HAND_TAG, "handleClap: phone not in hand, starting normal alert flow")
            alertController.startAlertActions("clap_not_in_hand")

            withContext(Dispatchers.Main) {
                MyApplication.mInstance.preferenceManager.put(
                    Key.isClapDetected,
                    true
                )
                alertController.launchStopSoundActivity(clearTop = false)
            }
        } else {
            DetectorLog.w(IN_HAND_TAG, "handleClap: music is active, skipping trigger")
        }
    }

    private fun startWhistleDetection() {
        audioDetector.startWhistleDetection {
            try {
                triggerFindPhoneAction()
            } catch (e: Exception) {
                DetectorLog.e(TAG, "startWhistleDetection: trigger callback failed", e)
            }
        }
    }

    private fun startPasscodeDetection() {
        passcodeDetector.start()
    }

    private fun startMotionDetection() {
        motionDetector.start(Constants.SERVICE_TYPE, getServiceModeLabel(Constants.SERVICE_TYPE))
    }

    private suspend fun triggerFindPhoneAction() {
        DetectorLog.d(IN_HAND_TAG, "triggerFindPhoneAction: evaluating direct alert trigger")

        if (MyApplication.isAppInitialized()) {
            val manager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (manager == null) {
                DetectorLog.e(IN_HAND_TAG, "triggerFindPhoneAction: AudioManager is null")
                return
            }
            if (isDisableWhenUsingPhone) {
                if (isScreenOn()) {
                    DetectorLog.d(IN_HAND_TAG, "triggerFindPhoneAction: ignored because screen is on and disableWhenUsingPhone is enabled")
                    return
                } else {
                    DetectorLog.d(IN_HAND_TAG, "triggerFindPhoneAction: screen is off, trigger is allowed")
                }
            }

            if (!manager.isMusicActive) {
                DetectorLog.d(
                    IN_HAND_TAG,
                    "triggerFindPhoneAction: no music active, starting alert actions"
                )

                alertController.startAlertActions("direct_trigger")

                withContext(Dispatchers.Main) {
                    MyApplication.mInstance.preferenceManager.put(
                        Key.isClapDetected,
                        true
                    )
                    alertController.launchStopSoundActivity(clearTop = false)
                }
            } else {
                DetectorLog.w(
                    IN_HAND_TAG,
                    "triggerFindPhoneAction: music is active, skipping trigger"
                )
            }
        } else {
            DetectorLog.d(
                IN_HAND_TAG,
                "triggerFindPhoneAction: app not initialized yet, falling back to clap handler"
            )
            CallUITrigger.shouldDialog.postValue(false)
            handleClap(isForeground = false)
        }
    }

    private fun isScreenOn(): Boolean {
        return try {
            val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isInteractive == true
        } catch (e: Exception) {
            DetectorLog.e(TAG, "isScreenOn: failed", e)
            false
        }
    }

    private fun registerStopSoundReceiver() {
        if (isStopReceiverRegistered) {
            DetectorLog.d(TAG, "registerStopSoundReceiver: already registered")
            return
        }

        try {
            val filter = IntentFilter(STOP_SOUND_ACTION)

            ContextCompat.registerReceiver(
                applicationContext,
                stopSoundReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )

            isStopReceiverRegistered = true
            DetectorLog.d(TAG, "registerStopSoundReceiver: registered")
        } catch (e: Exception) {
            DetectorLog.e(TAG, "registerStopSoundReceiver: failed", e)
        }
    }

    private fun unregisterStopSoundReceiver() {
        if (!isStopReceiverRegistered) {
            DetectorLog.d(TAG, "unregisterStopSoundReceiver: already unregistered")
            return
        }

        try {
            applicationContext.unregisterReceiver(stopSoundReceiver)
        } catch (e: Exception) {
            DetectorLog.w(TAG, "unregisterStopSoundReceiver: failed or already unregistered", e)
        } finally {
            isStopReceiverRegistered = false
        }
    }

    private fun cleanupAndStop() {
        try {
            MyApplication.mInstance.preferenceManager.put(Key.isClapDetected, false)
            MyApplication.mInstance.preferenceManager.put(Key.isDetectorActive, false)
            stopRequested = true
            cleanupResources()
            DetectorLog.d(TAG, "cleanupAndStop: worker stop requested and cleaned")
        } catch (e: Exception) {
            DetectorLog.e(TAG, "cleanupAndStop error", e)
            stopRequested = true
        }
    }

    private fun cleanupResources() {
        if (cleanupStarted) return
        cleanupStarted = true

        DetectorLog.d(TAG, "cleanupResources")
        isWorkerActive = false

        detectionBootJob?.cancel()
        detectionBootJob = null

        clapJob?.cancel()
        clapJob = null

        runSafely("audioDetector.stop") {
            audioDetector.stop()
            alertController.stopAudioOnly()
        }

        runSafely("motionDetector.stop") {
            motionDetector.stop()
        }

        runSafely("passcodeDetector.destroy") {
            passcodeDetector.destroy()
        }

        runSafely("alertController.stopAllAlertOutputs") {
            alertController.stopAllAlertOutputs("cleanup_resources")
        }

        runSafely("alertController.release") {
            alertController.release()
        }

        runSafely("mainHandler.removeCallbacks") {
            mainHandler.removeCallbacksAndMessages(null)
        }

        unregisterStopSoundReceiver()

        runSafely("alertController.setServiceActive") {
            alertController.setServiceActive(false)
        }
    }

    private fun canStartForCurrentMode(): Boolean {
        return when (Constants.SERVICE_TYPE) {
            Constants.BY_CLAP, Constants.BY_WHISTLE -> {
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            }

            else -> true
        }
    }

    private inline fun runSafely(actionName: String, action: () -> Unit) {
        try {
            action()
        } catch (e: Exception) {
            DetectorLog.e(TAG, "runSafely: $actionName failed", e)
        }
    }
}
