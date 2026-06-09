package com.rpm.app.data.signalr

import android.util.Log
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.rpm.app.data.local.TokenDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

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

    private val _vitals = MutableSharedFlow<RealTimeVitals>(replay = 1, extraBufferCapacity = 64)
    val vitals: SharedFlow<RealTimeVitals> = _vitals.asSharedFlow()

    private val _alerts = MutableSharedFlow<RealTimeAlert>(extraBufferCapacity = 32)
    val alerts: SharedFlow<RealTimeAlert> = _alerts.asSharedFlow()

    /** Connect on a background thread; retries until connected or coroutine cancelled. */
    suspend fun connect(patientId: String): Boolean = withContext(Dispatchers.IO) {
        val normalizedId = normalizePatientIdForHub(patientId)
        while (coroutineContext.isActive) {
            try {
                if (connectedPatientId == normalizedId && hub != null) {
                    return@withContext true
                }
                disconnectInternal()
                val token = tokenStore.getAccessToken()
                if (token.isNullOrBlank()) {
                    Log.w(TAG, "No access token — waiting before vitals hub connect")
                    delay(2_000)
                    continue
                }
                val hubUrl = "${baseUrl}hubs/vitals?access_token=${java.net.URLEncoder.encode(token, "UTF-8")}"
                val connection = HubConnectionBuilder.create(hubUrl).build()

                // Use Object callback so Gson returns a LinkedTreeMap (Map<*,*>), then
                // VitalsPayloadParser normalises both camelCase and PascalCase keys.
                // This mirrors the web app's normalizePayload() and avoids Gson reflection
                // failures with Kotlin data classes in release/R8 builds.
                connection.on(
                    "ReceiveVitals",
                    { raw: Any? ->
                        val vitals = VitalsPayloadParser.parse(raw)
                        if (vitals != null) {
                            Log.d(TAG, "ReceiveVitals HR=${vitals.heartRateBpm} SpO2=${vitals.spO2Percent}")
                            _vitals.tryEmit(vitals)
                        } else {
                            Log.w(TAG, "ReceiveVitals: could not parse payload – raw type=${raw?.javaClass?.name}")
                        }
                    },
                    Object::class.java,
                )
                connection.on(
                    "ReceiveAlert",
                    { payload ->
                        val alert = parseAlert(payload)
                        if (alert != null) _alerts.tryEmit(alert)
                    },
                    Object::class.java,
                )

                connection.start().blockingAwait()
                connection.send("SubscribeToPatient", normalizedId)
                hub = connection
                connectedPatientId = normalizedId
                Log.i(TAG, "Connected to vitals hub for patient $normalizedId")
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "SignalR connect failed: ${e.message}", e)
                disconnectInternal()
                delay(3_000)
            }
        }
        false
    }

    fun disconnect(patientId: String) {
        val normalizedId = normalizePatientIdForHub(patientId)
        if (connectedPatientId != normalizedId && connectedPatientId != null) return
        try {
            hub?.send("UnsubscribeFromPatient", normalizedId)
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

    @Suppress("UNCHECKED_CAST")
    private fun parseAlert(raw: Any?): RealTimeAlert? = runCatching {
        val map = raw as? Map<*, *> ?: return null
        fun pick(camel: String, pascal: String): String? =
            (map[camel] ?: map[pascal])?.toString()
        RealTimeAlert(
            patientId = pick("patientId", "PatientId") ?: return null,
            alertId   = pick("alertId", "AlertId") ?: pick("id", "Id") ?: return null,
            type      = pick("type", "Type") ?: "",
            severity  = pick("severity", "Severity") ?: "",
            message   = pick("message", "Message") ?: "",
        )
    }.getOrNull()
}
