package com.base.find_phone_clap_detector.service.helpers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.base.find_phone_clap_detector.service.DetectorLog

class StopSoundReceiver(
    private val onStopRequested: () -> Unit
) : BroadcastReceiver() {

    private companion object {
        private const val TAG = "StopSoundReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        DetectorLog.d(TAG, "onReceive: received stop broadcast")
        onStopRequested()
    }
}
