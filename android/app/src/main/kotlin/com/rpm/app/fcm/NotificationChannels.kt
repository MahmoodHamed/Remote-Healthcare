package com.rpm.app.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build

object NotificationChannels {
    const val ALERTS = "rpm_alerts"
    const val CHAT = "rpm_chat"

    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        manager.createNotificationChannel(
            NotificationChannel(ALERTS, "Health Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Critical vitals and health alerts"
                enableVibration(true)
                setSound(sound, audio)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHAT, "Messages", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Doctor and patient chat messages"
                enableVibration(true)
                setSound(sound, audio)
            }
        )
    }
}
