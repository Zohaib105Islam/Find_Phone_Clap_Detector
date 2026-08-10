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
import com.base.find_phone_clap_detector.myApplication.MyApplication
import com.base.find_phone_clap_detector.service.PhoneDetectorService

object DetectorServiceStarter {
    private const val TAG = "DetectorServiceStarter"
    private const val START_DEBOUNCE_MS = 1500L
    private const val STOP_SOUND_ACTION = "com.base.find_phone_clap_detector.STOP_SOUND_ACTION"

    @Volatile
    private var lastStartElapsedAt = 0L

    @Volatile
    private var startInProgress = false

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
            // ContextCompat.startForegroundService(context, Intent(context, ClapDetectService::class.java))
            ContextCompat.startForegroundService(context, Intent(context, PhoneDetectorService::class.java))
            lastStartElapsedAt = now
            Log.d(TAG, "requestStart: started, reason=$reason")
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
            if (sendStopBroadcast) {
                context.sendBroadcast(Intent(STOP_SOUND_ACTION))
            }
            // context.stopService(Intent(context, ClapDetectService::class.java).putExtra("stop", true))
            val stopped = context.stopService(
                Intent(context, PhoneDetectorService::class.java).putExtra("stop", true)
            )
            Log.d(TAG, "requestStop: stopped=$stopped, reason=$reason")
            stopped
        } catch (e: Exception) {
            Log.e(TAG, "requestStop: failed, reason=$reason", e)
            false
        }
    }

    fun isDetectorRunning(): Boolean {
        return PhoneDetectorService.isRunning
    }

    private fun requiresMicrophoneFgs(): Boolean {
        return when (Constants.SERVICE_TYPE) {
            Constants.BY_CLAP, Constants.BY_WHISTLE, Constants.BY_PASSCODE -> true
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
