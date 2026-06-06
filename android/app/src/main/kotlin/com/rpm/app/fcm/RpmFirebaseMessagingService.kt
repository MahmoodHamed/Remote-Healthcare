package com.rpm.app.fcm

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.rpm.app.MainActivity
import com.rpm.app.data.local.TokenDataStore
import com.rpm.app.data.remote.api.RpmApiService
import com.rpm.app.data.remote.dto.UpdateFcmTokenRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RpmFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var api: RpmApiService
    @Inject lateinit var tokenStore: TokenDataStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        NotificationChannels.createAll(this)

        val data = message.data
        val title = message.notification?.title ?: data["title"] ?: "Remote Care"
        val body = message.notification?.body ?: data["body"] ?: ""
        val channelId = data["channelId"]
            ?: if (data["type"] == "ChatMessage") NotificationChannels.CHAT
            else NotificationChannels.ALERTS
        val conversationId = data["conversationId"]

        showNotification(title, body, channelId, conversationId)
    }

    override fun onNewToken(token: String) {
        scope.launch {
            val accessToken = tokenStore.getAccessToken()
            if (!accessToken.isNullOrBlank()) {
                runCatching { api.updateFcmToken(UpdateFcmTokenRequest(token)) }
            }
        }
    }

    private fun showNotification(
        title: String,
        body: String,
        channelId: String,
        conversationId: String?,
    ) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            conversationId?.let { putExtra("conversationId", it) }
            putExtra("openNotifications", conversationId == null)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            (conversationId ?: title).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
