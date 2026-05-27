package com.rpm.watch.health

import android.content.Context
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "HeartRateTracker"
private const val SAMSUNG_FLUSH_INTERVAL_MS = 15_000L

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
 * Samsung Health Sensor SDK (primary) + Android SensorManager (fallback/parallel).
 * Galaxy Watch requires the watch display on and a snug fit for optical sensors.
 */
@Singleton
class HeartRateTrackerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var healthTrackingService: HealthTrackingService? = null
    private var activeTracker: HealthTracker? = null
    private val platformReader = PlatformSensorReader(context)

    fun monitoringFlow(mode: MonitoringMode): Flow<TrackerState> = callbackFlow {
        trySend(TrackerState.Connecting)

        // Platform sensors start immediately — works even when Samsung SDK is blocked.
        val platformStarted = platformReader.start(
            readHeartRate = if (mode == MonitoringMode.HEART_RATE) {
                { bpm ->
                    trySend(
                        TrackerState.Measuring(
                            VitalReading(
                                heartRateBpm = bpm,
                                status = HrStatus.GOOD,
                                timestampMs = System.currentTimeMillis()
                            )
                        )
                    )
                }
            } else null,
            readSpO2 = if (mode == MonitoringMode.SPO2) {
                { pct ->
                    trySend(
                        TrackerState.Measuring(
                            VitalReading(
                                spO2Percent = pct,
                                status = HrStatus.GOOD,
                                timestampMs = System.currentTimeMillis()
                            )
                        )
                    )
                }
            } else null,
            readTemperature = if (mode == MonitoringMode.TEMPERATURE) {
                { c ->
                    trySend(
                        TrackerState.Measuring(
                            VitalReading(
                                temperatureC = c,
                                status = HrStatus.GOOD,
                                timestampMs = System.currentTimeMillis()
                            )
                        )
                    )
                }
            } else null
        )
        if (!platformStarted) {
            Log.w(TAG, "No platform sensors registered for ${mode.name}")
        }

        val trackerEventListener = object : HealthTracker.TrackerEventListener {
            override fun onDataReceived(dataPoints: List<DataPoint>) {
                dataPoints.forEach { dp ->
                    logDataPointContents(dp)
                    val reading = parseSamsungReading(mode, dp) ?: return@forEach
                    trySend(TrackerState.Measuring(reading))
                }
            }

            override fun onError(error: HealthTracker.TrackerError) {
                Log.e(TAG, "Samsung tracker error: $error")
                if (error == HealthTracker.TrackerError.PERMISSION_ERROR) {
                    trySend(TrackerState.Error("Body sensor permission required"))
                }
            }

            override fun onFlushCompleted() {
                Log.d(TAG, "Samsung flush completed")
            }
        }

        val connectionListener = object : ConnectionListener {
            override fun onConnectionSuccess() {
                Log.i(TAG, "Samsung Health connected for ${mode.name}")
                val available = HealthTrackerType.entries.joinToString { it.name }
                Log.d(TAG, "Available Samsung trackers: $available")

                val trackerType = resolveTrackerType(mode)
                if (trackerType == null) {
                    Log.w(TAG, "No Samsung tracker for ${mode.name}; using platform sensors only")
                    return
                }

                try {
                    activeTracker = healthTrackingService?.getHealthTracker(trackerType)
                    if (activeTracker == null) {
                        Log.w(TAG, "Samsung tracker ${trackerType.name} returned null")
                        return
                    }
                    activeTracker?.setEventListener(trackerEventListener)
                    Log.i(TAG, "Samsung listener active: ${trackerType.name}")
                    runCatching { activeTracker?.flush() }
                } catch (e: Exception) {
                    Log.e(TAG, "Samsung tracker init failed: ${e.message}")
                }
            }

            override fun onConnectionEnded() {
                Log.i(TAG, "Samsung Health connection ended")
                trySend(TrackerState.Disconnected)
            }

            override fun onConnectionFailed(exception: HealthTrackerException) {
                Log.e(TAG, "Samsung connection failed: ${exception.message} (code=${exception.errorCode})")
                if (!platformStarted) {
                    trySend(
                        TrackerState.Error(
                            exception.message ?: "Samsung Health unavailable",
                            exception.errorCode
                        )
                    )
                }
            }
        }

        val flushJob = launch {
            delay(5_000)
            while (isActive) {
                runCatching { activeTracker?.flush() }
                delay(SAMSUNG_FLUSH_INTERVAL_MS)
            }
        }

        try {
            healthTrackingService = HealthTrackingService(connectionListener, context)
            healthTrackingService?.connectService()
        } catch (e: Exception) {
            Log.e(TAG, "Samsung HealthTrackingService failed: ${e.message}")
            if (!platformStarted) {
                trySend(TrackerState.Error(e.message ?: "Sensor SDK unavailable"))
            }
        }

        awaitClose {
            flushJob.cancel()
            Log.i(TAG, "Stopping ${mode.name} tracking")
            platformReader.stop()
            try {
                activeTracker?.unsetEventListener()
            } catch (_: Exception) {
            }
            try {
                healthTrackingService?.disconnectService()
            } catch (_: Exception) {
            }
            activeTracker = null
            healthTrackingService = null
        }
    }

    private fun parseSamsungReading(mode: MonitoringMode, dp: DataPoint): VitalReading? {
        return when (mode) {
            MonitoringMode.HEART_RATE -> {
                val hr = dp.getValue(ValueKey.HeartRateSet.HEART_RATE)
                val statusCode = dp.getValue(ValueKey.HeartRateSet.HEART_RATE_STATUS) ?: 0
                val status = HrStatus.from(statusCode)
                if (hr == null || hr <= 0) return null
                VitalReading(
                    heartRateBpm = hr.coerceAtLeast(0),
                    status = status,
                    timestampMs = System.currentTimeMillis()
                )
            }

            MonitoringMode.SPO2 -> {
                val spo2 = readKeyValue(
                    dp = dp,
                    nestedClassName = "SpO2Set",
                    valueCandidates = listOf("SPO2", "SP02", "OXYGEN_SATURATION", "OXYGEN"),
                    statusCandidates = listOf("SPO2_STATUS", "OXYGEN_STATUS", "STATUS")
                )?.toFloat() ?: readKeyValue(
                    dp = dp,
                    nestedClassName = "PpgSet",
                    valueCandidates = listOf("SPO2", "OXYGEN", "STATUS"),
                    statusCandidates = listOf("SPO2_STATUS", "STATUS")
                )?.toFloat()
                if (spo2 == null || spo2 <= 0f) return null
                VitalReading(spO2Percent = spo2, status = HrStatus.GOOD)
            }

            MonitoringMode.TEMPERATURE -> {
                val temp = readKeyValue(
                    dp = dp,
                    nestedClassName = "SkinTemperatureSet",
                    valueCandidates = listOf("SKIN_TEMPERATURE", "TEMPERATURE", "TEMP", "OBJECT_TEMPERATURE"),
                    statusCandidates = listOf("SKIN_TEMPERATURE_STATUS", "TEMPERATURE_STATUS", "STATUS")
                )?.toFloat()
                if (temp == null) return null
                VitalReading(temperatureC = temp, status = HrStatus.GOOD)
            }
        }
    }

    private fun resolveTrackerType(mode: MonitoringMode): HealthTrackerType? {
        val preferred = when (mode) {
            MonitoringMode.HEART_RATE -> listOf(
                "HEART_RATE_CONTINUOUS",
                "HEART_RATE"
            )
            MonitoringMode.SPO2 -> listOf(
                "SPO2_ON_DEMAND",
                "SPO2",
                "PPG_ON_DEMAND",
                "PPG_CONTINUOUS"
            )
            MonitoringMode.TEMPERATURE -> listOf(
                "SKIN_TEMPERATURE_CONTINUOUS",
                "SKIN_TEMPERATURE",
                "SKIN_TEMPERATURE_ON_DEMAND"
            )
        }

        for (name in preferred) {
            val match = HealthTrackerType.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
            if (match != null) return match
        }

        val keywords = when (mode) {
            MonitoringMode.HEART_RATE -> listOf("HEART_RATE")
            MonitoringMode.SPO2 -> listOf("SPO2", "OXYGEN")
            MonitoringMode.TEMPERATURE -> listOf("SKIN_TEMPERATURE")
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
            ValueKey::class.java.isAssignableFrom(field.type) &&
                java.lang.reflect.Modifier.isStatic(field.modifiers)
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
                    ValueKey::class.java.isAssignableFrom(field.type) &&
                        java.lang.reflect.Modifier.isStatic(field.modifiers)
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
            Log.w(TAG, "Failed to dump DataPoint: ${e.message}")
        }
    }
}
