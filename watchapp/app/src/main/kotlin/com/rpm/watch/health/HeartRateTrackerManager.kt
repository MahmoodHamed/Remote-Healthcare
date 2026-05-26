package com.rpm.watch.health

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.rpm.watch.MonitoringMode
import com.samsung.android.service.health.tracking.ConnectionListener
import com.samsung.android.service.health.tracking.HealthTracker
import com.samsung.android.service.health.tracking.HealthTrackingService
import com.samsung.android.service.health.tracking.HealthTrackerException
import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.ValueKey
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "HeartRateTracker"

/** Heart rate status values returned by Samsung Health Sensor SDK */
enum class HrStatus(val code: Int) {
    INITIAL(0),
    LOW_PASS(1),
    GOOD(2),
    MOVING(8),
    DEVICE_MOVING(15);

    companion object {
        fun from(code: Int) = entries.firstOrNull { it.code == code } ?: INITIAL
    }
}

data class VitalReading(
    val heartRateBpm: Int? = null,
    val spO2Percent: Float? = null,
    val temperatureC: Float? = null,
    val status: HrStatus,
    val timestampMs: Long = System.currentTimeMillis()
)

sealed class TrackerState {
    data object Connecting : TrackerState()
    data class Measuring(val reading: VitalReading) : TrackerState()
    data class Error(val message: String, val code: Int = -1) : TrackerState()
    data object Disconnected : TrackerState()
}

/**
 * Wraps the Samsung Health Sensor SDK to expose wearable vitals as a [Flow<TrackerState>].
 */
@Singleton
class HeartRateTrackerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var healthTrackingService: HealthTrackingService? = null
    private var heartRateTracker: HealthTracker? = null
    private var fallbackSensorManager: SensorManager? = null
    private var fallbackListener: SensorEventListener? = null
    private var fallbackStarted = false

    fun monitoringFlow(mode: MonitoringMode): Flow<TrackerState> = callbackFlow {
        trySend(TrackerState.Connecting)

        val trackerEventListener = object : HealthTracker.TrackerEventListener {
            override fun onDataReceived(dataPoints: List<DataPoint>) {
                dataPoints.forEach { dp ->
                    // Debug: dump any available ValueKey fields and their values for diagnostics
                    logDataPointContents(dp)

                    val reading = when (mode) {
                        MonitoringMode.HEART_RATE -> VitalReading(
                            heartRateBpm = dp.getValue(ValueKey.HeartRateSet.HEART_RATE)?.coerceAtLeast(0),
                            status = HrStatus.from(dp.getValue(ValueKey.HeartRateSet.HEART_RATE_STATUS) ?: 0),
                            timestampMs = System.currentTimeMillis()
                        )

                        MonitoringMode.SPO2 -> VitalReading(
                            spO2Percent = readKeyValue(
                                dp = dp,
                                nestedClassName = "SpO2Set",
                                valueCandidates = listOf("SPO2", "SP02", "OXYGEN_SATURATION", "OXYGEN"),
                                statusCandidates = listOf("SPO2_STATUS", "OXYGEN_STATUS", "STATUS")
                            )?.toFloat(),
                            status = HrStatus.GOOD,
                            timestampMs = System.currentTimeMillis()
                        )

                        MonitoringMode.TEMPERATURE -> VitalReading(
                            temperatureC = readKeyValue(
                                dp = dp,
                                nestedClassName = "SkinTemperatureSet",
                                valueCandidates = listOf("SKIN_TEMPERATURE", "TEMPERATURE", "TEMP"),
                                statusCandidates = listOf("SKIN_TEMPERATURE_STATUS", "TEMPERATURE_STATUS", "STATUS")
                            )?.toFloat(),
                            status = HrStatus.GOOD,
                            timestampMs = System.currentTimeMillis()
                        )
                    }

                    if (
                        reading.heartRateBpm != null ||
                        reading.spO2Percent != null ||
                        reading.temperatureC != null
                    ) {
                        trySend(TrackerState.Measuring(reading))
                    }
                }
            }

            override fun onError(error: HealthTracker.TrackerError) {
                Log.e(TAG, "Tracker error: $error")
                if (error.name.contains("SDK_POLICY_ERROR", ignoreCase = true)) {
                    Log.w(TAG, "Tracker reported SDK policy error; switching to SensorManager fallback")
                    startPlatformHrFallback(
                        onReading = { bpm ->
                            trySend(
                                TrackerState.Measuring(
                                    VitalReading(
                                        heartRateBpm = bpm,
                                        status = HrStatus.GOOD,
                                        timestampMs = System.currentTimeMillis()
                                    )
                                )
                            )
                        },
                        onError = { err ->
                            trySend(TrackerState.Error(err))
                        }
                    )
                    trySend(TrackerState.Connecting)
                    return
                }
                trySend(TrackerState.Error("Tracker error: $error"))
                // TrackerError is an enum (PERMISSION_ERROR, SDK_POLICY_ERROR), usually not recoverable by retrying
            }

            override fun onFlushCompleted() {
                Log.d(TAG, "Flush completed")
            }
        }

        val connectionListener = object : ConnectionListener {
            override fun onConnectionSuccess() {
                Log.i(TAG, "Samsung Health connected for ${mode.name}")
                try {
                    val trackerType = resolveTrackerType(mode)
                    if (trackerType == null) {
                        trySend(TrackerState.Error("No Samsung tracker available for ${mode.name}"))
                        return
                    }

                    heartRateTracker = healthTrackingService?.getHealthTracker(trackerType)
                    if (heartRateTracker == null) {
                        trySend(TrackerState.Error("Samsung tracker ${trackerType.name} is not available on this watch"))
                        return
                    }
                    heartRateTracker?.setEventListener(trackerEventListener)
                    Log.i(TAG, "Samsung tracker listener set for ${mode.name} using ${trackerType.name}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize tracker: ${e.message}")
                    trySend(TrackerState.Error(e.message ?: "Failed to initialize tracker"))
                }
            }

            override fun onConnectionEnded() {
                Log.i(TAG, "Samsung Health connection ended")
                trySend(TrackerState.Disconnected)
            }

            override fun onConnectionFailed(exception: HealthTrackerException) {
                Log.e(TAG, "Connection failed: ${exception.message}")
                val message = exception.message ?: "Connection failed"
                if (
                    message.contains("SDK_POLICY_ERROR", ignoreCase = true) ||
                    exception.errorCode.toString().contains("SDK_POLICY_ERROR", ignoreCase = true)
                ) {
                    Log.w(TAG, "Samsung SDK policy blocked app; switching to SensorManager fallback")
                    startPlatformHrFallback(
                        onReading = { bpm ->
                            trySend(
                                TrackerState.Measuring(
                                    VitalReading(
                                        heartRateBpm = bpm,
                                        status = HrStatus.GOOD,
                                        timestampMs = System.currentTimeMillis()
                                    )
                                )
                            )
                        },
                        onError = { err ->
                            trySend(TrackerState.Error(err, exception.errorCode))
                        }
                    )
                    trySend(TrackerState.Connecting)
                    return
                }
                trySend(
                    TrackerState.Error(
                        message = message,
                        code    = exception.errorCode
                    )
                )
                close(exception)
            }
        }

        healthTrackingService = HealthTrackingService(connectionListener, context)
        healthTrackingService?.connectService()

        awaitClose {
            Log.i(TAG, "Stopping heart rate measurement and disconnecting")
            try {
                heartRateTracker?.unsetEventListener()
            } catch (_: Exception) {}
            try {
                healthTrackingService?.disconnectService()
            } catch (_: Exception) {}
            try {
                fallbackListener?.let { l -> fallbackSensorManager?.unregisterListener(l) }
            } catch (_: Exception) {}
            fallbackListener = null
            fallbackSensorManager = null
            fallbackStarted = false
            heartRateTracker       = null
            healthTrackingService  = null
        }
    }

    private fun resolveTrackerType(mode: MonitoringMode): HealthTrackerType? {
        val keywords = when (mode) {
            MonitoringMode.HEART_RATE -> listOf("HEART")
            MonitoringMode.SPO2 -> listOf("SPO2", "OXYGEN")
            MonitoringMode.TEMPERATURE -> listOf("SKIN_TEMPERATURE", "TEMPERATURE", "TEMP")
        }

        return HealthTrackerType.entries.firstOrNull { trackerType ->
            keywords.any { keyword -> trackerType.name.contains(keyword, ignoreCase = true) }
        }
    }

    private fun readKeyValue(
        dp: DataPoint,
        nestedClassName: String,
        valueCandidates: List<String>,
        statusCandidates: List<String>
    ): Number? {
        val nestedClass = runCatching {
            Class.forName("com.samsung.android.service.health.tracking.data.ValueKey$$nestedClassName")
        }.getOrNull() ?: return null

        val valueKey = resolveValueKeyField(nestedClass, valueCandidates, excludeStatus = true)
        if (valueKey != null) {
            runCatching { return dp.getValue(valueKey) as? Number }.getOrNull()
        }

        val statusKey = resolveValueKeyField(nestedClass, statusCandidates, excludeStatus = false)
        return statusKey?.let { dp.getValue(it) as? Number }
    }

    private fun resolveValueKeyField(
        nestedClass: Class<*>,
        preferredNames: List<String>,
        excludeStatus: Boolean
    ): ValueKey<*>? {
        val fields = nestedClass.fields.filter { field ->
            ValueKey::class.java.isAssignableFrom(field.type) && java.lang.reflect.Modifier.isStatic(field.modifiers)
        }

        fun score(fieldName: String): Int {
            val upper = fieldName.uppercase()
            var score = 0
            if (preferredNames.any { upper == it.uppercase() }) score += 100
            if (preferredNames.any { upper.contains(it.uppercase()) }) score += 50
            if (excludeStatus && upper.contains("STATUS")) score -= 100
            if (upper.contains("VALUE")) score += 10
            return score
        }

        return fields.maxByOrNull { score(it.name) }?.let { field ->
            runCatching { field.get(null) as? ValueKey<*> }.getOrNull()
        }
    }

    private fun logDataPointContents(dp: DataPoint) {
        try {
            val nestedNames = listOf("HeartRateSet", "SpO2Set", "SkinTemperatureSet", "PpgSet")
            for (n in nestedNames) {
                val cls = runCatching {
                    Class.forName("com.samsung.android.service.health.tracking.data.ValueKey\$$n")
                }.getOrNull() ?: continue

                val fields = cls.fields.filter { field ->
                    ValueKey::class.java.isAssignableFrom(field.type) && java.lang.reflect.Modifier.isStatic(field.modifiers)
                }

                for (f in fields) {
                    val vk = runCatching { f.get(null) as? ValueKey<*> }.getOrNull() ?: continue
                    val v = runCatching { dp.getValue(vk) }.getOrNull()
                    if (v != null) {
                        Log.d(TAG, "DataPoint ${n}.${f.name} = $v")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to dump DataPoint contents: ${e.message}")
        }
    }

    private fun startPlatformHrFallback(
        onReading: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        if (fallbackStarted) return

        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        if (sm == null) {
            onError("SensorManager unavailable")
            return
        }

        val hrSensor = sm.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        if (hrSensor == null) {
            onError("Heart-rate sensor not available")
            return
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val value = event?.values?.firstOrNull() ?: return
                onReading(value.toInt().coerceAtLeast(0))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        fallbackSensorManager = sm
        fallbackListener = listener
        sm.registerListener(listener, hrSensor, SensorManager.SENSOR_DELAY_NORMAL)
        fallbackStarted = true
    }
}