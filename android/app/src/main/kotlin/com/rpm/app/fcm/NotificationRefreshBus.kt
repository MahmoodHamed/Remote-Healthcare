package com.rpm.app.fcm

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Notifies in-app notification inbox to refresh after a push is received. */
object NotificationRefreshBus {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    fun notifyRefresh() {
        _events.tryEmit(Unit)
    }
}
