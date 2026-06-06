package com.rpm.app.data.remote.api

import com.rpm.app.BuildConfig
import com.rpm.app.data.local.TokenDataStore
import com.rpm.app.data.remote.dto.AuthTokensDto
import com.rpm.app.data.remote.dto.RefreshTokenRequest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/** Refreshes JWT tokens without Retrofit to avoid circular OkHttp dependencies. */
@Singleton
class TokenRefresher @Inject constructor(
    private val tokenStore: TokenDataStore,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder().build()

    @Synchronized
    fun refreshTokens(): Boolean = runBlocking {
        val accessToken = tokenStore.getAccessToken() ?: return@runBlocking false
        val refreshToken = tokenStore.refreshToken.firstOrNull() ?: return@runBlocking false

        val body = json.encodeToString(
            RefreshTokenRequest.serializer(),
            RefreshTokenRequest(accessToken = accessToken, refreshToken = refreshToken),
        )
        val request = Request.Builder()
            .url("${BuildConfig.BASE_URL}api/auth/refresh")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        return@runBlocking try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runBlocking false
                val payload = response.body?.string() ?: return@runBlocking false
                val tokens = json.decodeFromString(AuthTokensDto.serializer(), payload)
                tokenStore.updateTokens(tokens.accessToken, tokens.refreshToken)
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}
