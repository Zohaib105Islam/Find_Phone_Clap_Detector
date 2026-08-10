package com.base.find_phone_clap_detector.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.base.find_phone_clap_detector.managers.PreferenceManager.Key
import com.base.find_phone_clap_detector.myApplication.MyApplication
import com.base.find_phone_clap_detector.service.PhoneDetectorWorker

object DetectorWorkerStarter {
    private const val TAG = "DetectorWorkerStarter"
    private const val START_DEBOUNCE_MS = 1500L
    private const val STOP_SOUND_ACTION = "com.base.find_phone_clap_detector.STOP_SOUND_ACTION"

    @Volatile
    private var lastStartElapsedAt = 0L

    @Volatile
    private var startInProgress = false

    @Volatile
    private var startRequested = false

    private val mainHandler = Handler(Looper.getMainLooper())

    @Synchronized
    fun requestStart(context: Context, reason: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (startInProgress || now - lastStartElapsedAt < START_DEBOUNCE_MS) {
            Log.d(TAG, "requestStart: skipped, reason=$reason")
            return false
        }

        if (!AppStateTracker.isInForeground) {
            MyApplication.shouldRestartDetectorFromStopSound = true
            Log.d(TAG, "requestStart: deferred until foreground, reason=$reason")
            return false
        }
        if (requiresMicrophoneFgs() && !hasRecordAudioPermission(context)) {
            MyApplication.shouldRestartDetectorFromStopSound = true
            Log.w(TAG, "requestStart: blocked (missing RECORD_AUDIO), reason=$reason")
            return false
        }

        return try {
            startInProgress = true
            val workRequest = OneTimeWorkRequestBuilder<PhoneDetectorWorker>().build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                PhoneDetectorWorker.UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
            lastStartElapsedAt = now
            startRequested = true
            MyApplication.mInstance.preferenceManager.put(
                Key.isDetectorActive,
                true
            )
            Log.d(TAG, "requestStart: enqueued worker, reason=$reason")
            true
        } catch (e: Exception) {
            Log.e(TAG, "requestStart: failed, reason=$reason", e)
            MyApplication.shouldRestartDetectorFromStopSound = true
            false
        } finally {
            mainHandler.postDelayed({ startInProgress = false }, 700L)
        }
    }

    fun requestStop(
        context: Context,
        reason: String,
        sendStopBroadcast: Boolean = false
    ): Boolean {
        return try {
            MyApplication.shouldRestartDetectorFromStopSound = false
            if (sendStopBroadcast) {
                context.sendBroadcast(Intent(STOP_SOUND_ACTION))
            }
            WorkManager.getInstance(context.applicationContext)
                .cancelUniqueWork(PhoneDetectorWorker.UNIQUE_WORK_NAME)
            startRequested = false
            Log.d(TAG, "requestStop: cancel requested, reason=$reason")
            true
        } catch (e: Exception) {
            Log.e(TAG, "requestStop: failed, reason=$reason", e)
            false
        }
    }

    fun isDetectorRunning(): Boolean {
        return PhoneDetectorWorker.isRunning || startRequested || startInProgress
    }

    private fun requiresMicrophoneFgs(): Boolean {
        return when (Constants.SERVICE_TYPE) {
            Constants.BY_CLAP, Constants.BY_WHISTLE -> true
            else -> false
        }
    }

    private fun hasRecordAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
}
