package com.rpm.app.data.fcm

import com.google.firebase.messaging.FirebaseMessaging
import com.rpm.app.data.remote.api.RpmApiService
import com.rpm.app.data.remote.dto.UpdateFcmTokenRequest
import com.rpm.app.data.remote.httpErrorMessage
import com.rpm.app.domain.model.Resource
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class FcmTokenRegistrar @Inject constructor(
    private val api: RpmApiService,
) {
    suspend fun fetchToken(): String? = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resume(null) }
    }

    suspend fun registerToken(token: String? = null): Resource<Unit> {
        val resolvedToken = token ?: fetchToken()
        if (resolvedToken.isNullOrBlank()) return Resource.Error("FCM token unavailable")
        return try {
            val response = api.updateFcmToken(UpdateFcmTokenRequest(resolvedToken))
            if (response.isSuccessful) Resource.Success(Unit)
            else Resource.Error(httpErrorMessage(response))
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to register FCM token")
        }
    }
}
