package com.rpm.app.data.remote.api

import com.rpm.app.data.auth.SessionManager
import com.rpm.app.data.local.TokenDataStore
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenRefresher: TokenRefresher,
    private val tokenStore: TokenDataStore,
    private val sessionManager: SessionManager,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) {
            expireSession()
            return null
        }
        if (response.request.url.encodedPath.contains("/api/auth/refresh")) {
            expireSession()
            return null
        }
        if (!tokenRefresher.refreshTokens()) {
            expireSession()
            return null
        }
        val newToken = runBlocking { tokenStore.getAccessToken() } ?: run {
            expireSession()
            return null
        }
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }

    private fun expireSession() {
        runBlocking { tokenStore.clearSession() }
        // Only notify if a session was active; stale 401s are ignored via generation in AuthViewModel.
        sessionManager.notifySessionExpired()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
