package com.rpm.watch.health

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
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

data class HeartRateReading(
    val bpm: Int,
    val status: HrStatus,
    val timestampMs: Long = System.currentTimeMillis()
)

sealed class TrackerState {
    data object Connecting : TrackerState()
    data class Measuring(val reading: HeartRateReading) : TrackerState()
    data class Error(val message: String, val code: Int = -1) : TrackerState()
    data object Disconnected : TrackerState()
}

/**
 * Wraps the Samsung Health Sensor SDK to expose heart rate as a [Flow<TrackerState>].
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

    fun heartRateFlow(): Flow<TrackerState> = callbackFlow {
        trySend(TrackerState.Connecting)

        val trackerEventListener = object : HealthTracker.TrackerEventListener {
            override fun onDataReceived(dataPoints: List<DataPoint>) {
                dataPoints.forEach { dp ->
                    val bpm    = dp.getValue(ValueKey.HeartRateSet.HEART_RATE)
                    val status = dp.getValue(ValueKey.HeartRateSet.HEART_RATE_STATUS)

                    if (bpm != null) {
                        trySend(
                            TrackerState.Measuring(
                                HeartRateReading(
                                    bpm       = bpm.coerceAtLeast(0),
                                    status    = HrStatus.from(status ?: 0),
                                    timestampMs = System.currentTimeMillis()
                                )
                            )
                        )
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
                                    HeartRateReading(
                                        bpm = bpm,
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
                Log.i(TAG, "Samsung Health connected")
                try {
                    heartRateTracker = healthTrackingService
                        ?.getHealthTracker(HealthTrackerType.HEART_RATE)
                    heartRateTracker?.setEventListener(trackerEventListener)
                    Log.i(TAG, "Heart rate measurement listener set")
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
                                    HeartRateReading(
                                        bpm = bpm,
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