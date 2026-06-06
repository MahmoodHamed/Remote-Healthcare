package com.rpm.app.fcm

import com.google.firebase.messaging.FirebaseMessaging
import com.rpm.app.data.remote.api.RpmApiService
import com.rpm.app.data.remote.dto.UpdateFcmTokenRequest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenRegistrar @Inject constructor(
    private val api: RpmApiService,
) {
    suspend fun registerCurrentToken(): Result<Unit> = runCatching {
        val token = FirebaseMessaging.getInstance().token.await()
        val response = api.updateFcmToken(UpdateFcmTokenRequest(token))
        if (!response.isSuccessful) error("FCM register failed: ${response.code()}")
    }
}
