package com.rpm.app.data.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks session generation so stale 401 events (e.g. from a slow startup /me call)
 * cannot log the user out after a fresh login.
 */
@Singleton
class SessionManager @Inject constructor() {

    private val _sessionExpired = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Long> = _sessionExpired.asSharedFlow()

    private var generation = 0L
    private var expired = false

    /** Call after login/register/logout to start a new session epoch. */
    fun beginSession(): Long {
        expired = false
        generation++
        return generation
    }

    fun reset() = beginSession()

    fun currentGeneration(): Long = generation

    fun notifySessionExpired() {
        if (expired) return
        expired = true
        _sessionExpired.tryEmit(generation)
    }
}
