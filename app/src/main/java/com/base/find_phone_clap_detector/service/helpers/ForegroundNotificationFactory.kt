package com.base.find_phone_clap_detector.service.helpers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.service.DetectorLog
import com.base.find_phone_clap_detector.ui.activities.StopSoundActivity

class ForegroundNotificationFactory(
    private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "ClapDetectionChannel"
        const val NOTIFICATION_ID = 1

        private const val TAG = "ForegroundNotification"
        private const val CHANNEL_NAME = "Clap Detection Service"
    }

    fun create(): Notification {
        ensureChannel()

        val notificationIntent = Intent(context, StopSoundActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(CHANNEL_NAME)
            .setContentText("Running")
            .setSmallIcon(R.drawable.icon_save_apply)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun ensureChannel() {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(notificationChannel)
        DetectorLog.d(TAG, "ensureChannel: notification channel ready")
    }
}
