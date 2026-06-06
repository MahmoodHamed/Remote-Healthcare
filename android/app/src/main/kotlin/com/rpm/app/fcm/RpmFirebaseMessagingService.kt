package com.rpm.app.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.rpm.app.MainActivity
import com.rpm.app.data.fcm.FcmTokenRegistrar
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RpmFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val type = data["type"] ?: "alert"
        val title = data["title"] ?: message.notification?.title ?: "RPM"
        val body = data["body"] ?: message.notification?.body ?: ""

        when (type) {
            "chat" -> showChatNotification(
                title = title,
                body = body,
                conversationId = data["conversationId"],
            )
            else -> showAlertNotification(title, body)
        }
    }

    override fun onNewToken(token: String) {
        serviceScope.launch {
            val registrar = EntryPointAccessors.fromApplication(
                applicationContext,
                FcmServiceEntryPoint::class.java,
            ).fcmTokenRegistrar()
            registrar.registerToken(token)
        }
    }

    private fun showChatNotification(title: String, body: String, conversationId: String?) {
        showNotification(
            channelId = CHANNEL_MESSAGES,
            channelName = "RPM Messages",
            notificationId = conversationId?.hashCode() ?: System.currentTimeMillis().toInt(),
            title = title,
            body = body,
            conversationId = conversationId,
        )
    }

    private fun showAlertNotification(title: String, body: String) {
        showNotification(
            channelId = CHANNEL_ALERTS,
            channelName = "RPM Alerts",
            notificationId = System.currentTimeMillis().toInt(),
            title = title,
            body = body,
            conversationId = null,
        )
    }

    private fun showNotification(
        channelId: String,
        channelName: String,
        notificationId: Int,
        title: String,
        body: String,
        conversationId: String?,
    ) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH),
            )
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            conversationId?.let { putExtra(MainActivity.EXTRA_CONVERSATION_ID, it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(notificationId, notification)
    }

    companion object {
        const val CHANNEL_ALERTS = "rpm_alerts"
        const val CHANNEL_MESSAGES = "rpm_messages"
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface FcmServiceEntryPoint {
    fun fcmTokenRegistrar(): FcmTokenRegistrar
}
