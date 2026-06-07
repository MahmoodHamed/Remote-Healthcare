package com.rpm.app.data.signalr

import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.rpm.app.data.local.TokenDataStore
import com.rpm.app.data.remote.dto.VitalRecordDto
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

data class RealTimeAlert(
    val patientId: String,
    val alertId: String,
    val type: String,
    val severity: String,
    val message: String
)

@Singleton
class VitalsSignalRClient @Inject constructor(
    private val tokenStore: TokenDataStore
) {
    private var hub: HubConnection? = null
    private val baseUrl: String = com.rpm.app.BuildConfig.SIGNALR_URL

    private val _vitals = MutableSharedFlow<VitalRecordDto>(extraBufferCapacity = 64)
    val vitals: SharedFlow<VitalRecordDto> = _vitals.asSharedFlow()

    private val _alerts = MutableSharedFlow<RealTimeAlert>(extraBufferCapacity = 32)
    val alerts: SharedFlow<RealTimeAlert> = _alerts.asSharedFlow()

    fun connect(patientId: String) {
        val token = runBlocking { tokenStore.getAccessToken() } ?: return
        hub = HubConnectionBuilder.create("${baseUrl}hubs/vitals?access_token=$token").build()

        hub!!.on("ReceiveVitals", { vitals: VitalRecordDto ->
            _vitals.tryEmit(vitals)
        }, VitalRecordDto::class.java)

        hub!!.on("ReceiveAlert", { alert: RealTimeAlert ->
            _alerts.tryEmit(alert)
        }, RealTimeAlert::class.java)

        hub!!.start().blockingAwait()
        hub!!.send("SubscribeToPatient", patientId)
    }

    fun disconnect(patientId: String) {
        try {
            hub?.send("UnsubscribeFromPatient", patientId)
            hub?.stop()
        } catch (_: Exception) {}
        hub = null
    }
}
