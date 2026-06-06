package com.rpm.app.data.signalr

import android.util.Log
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.rpm.app.data.local.TokenDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "VitalsSignalR"

/** JSON payload received from VitalsHub "ReceiveVitals" */
data class RealTimeVitals(
    val patientId: String,
    val heartRateBpm: Float? = null,
    val spO2Percent: Float? = null,
    val systolicBp: Float? = null,
    val diastolicBp: Float? = null,
    val temperatureC: Float? = null,
    val skinTemperatureC: Float? = null,
    val ambientTemperatureC: Float? = null,
    val hrvMs: Float? = null,
    val stressScore: Float? = null,
    val bodyFatPercent: Float? = null,
    val ecgAvgHeartRateBpm: Float? = null,
    val stepsCount: Int? = null,
    val caloriesBurned: Float? = null,
    val fallDetected: Boolean = false,
    val isWearing: Boolean = true,
    val recordedAt: String = "",
)

data class RealTimeAlert(
    val patientId: String,
    val alertId: String,
    val type: String,
    val severity: String,
    val message: String,
)

@Singleton
class VitalsSignalRClient @Inject constructor(
    private val tokenStore: TokenDataStore,
) {
    private var hub: HubConnection? = null
    private var connectedPatientId: String? = null
    private val baseUrl: String = com.rpm.app.BuildConfig.SIGNALR_URL

    private val _vitals = MutableSharedFlow<RealTimeVitals>(extraBufferCapacity = 64)
    val vitals: SharedFlow<RealTimeVitals> = _vitals.asSharedFlow()

    private val _alerts = MutableSharedFlow<RealTimeAlert>(extraBufferCapacity = 32)
    val alerts: SharedFlow<RealTimeAlert> = _alerts.asSharedFlow()

    /** Connect on a background thread; never throws — failures are logged only. */
    suspend fun connect(patientId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (connectedPatientId == patientId && hub != null) return@withContext true
            disconnectInternal()
            val token = tokenStore.getAccessToken()
            if (token.isNullOrBlank()) {
                Log.w(TAG, "No access token — skipping SignalR connect")
                return@withContext false
            }
            val connection = HubConnectionBuilder
                .create("${baseUrl}hubs/vitals?access_token=$token")
                .build()
            connection.on(
                "ReceiveVitals",
                { vitals: RealTimeVitals -> _vitals.tryEmit(vitals) },
                RealTimeVitals::class.java,
            )
            connection.on(
                "ReceiveAlert",
                { alert: RealTimeAlert -> _alerts.tryEmit(alert) },
                RealTimeAlert::class.java,
            )
            connection.start().blockingAwait()
            connection.send("SubscribeToPatient", patientId)
            hub = connection
            connectedPatientId = patientId
            Log.i(TAG, "Connected to vitals hub for patient $patientId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "SignalR connect failed: ${e.message}", e)
            disconnectInternal()
            false
        }
    }

    fun disconnect(patientId: String) {
        if (connectedPatientId != patientId && connectedPatientId != null) return
        try {
            hub?.send("UnsubscribeFromPatient", patientId)
        } catch (_: Exception) {}
        disconnectInternal()
    }

    private fun disconnectInternal() {
        try {
            hub?.stop()
        } catch (_: Exception) {}
        hub = null
        connectedPatientId = null
    }
}
