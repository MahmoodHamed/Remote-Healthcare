package com.rpm.watch.sensor

import android.content.Context
import android.util.Log
import com.rpm.watch.WatchPermissions
import com.rpm.watch.sensor.heart.HeartRateSamsungParser
import com.rpm.watch.sensor.platform.PlatformSensorHub
import com.rpm.watch.sensor.samsung.SamsungTrackerResolver
import com.rpm.watch.sensor.samsung.logDataPointContents
import com.rpm.watch.sensor.spo2.SpO2SamsungParser
import com.rpm.watch.sensor.temperature.SkinTemperatureSamsungParser
import com.samsung.android.service.health.tracking.ConnectionListener
import com.samsung.android.service.health.tracking.HealthTracker
import com.samsung.android.service.health.tracking.HealthTrackingService
import com.samsung.android.service.health.tracking.HealthTrackerException
import com.samsung.android.service.health.tracking.data.DataPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "VitalsSensorCoordinator"
private const val CONTINUOUS_FLUSH_MS = 2_000L
private const val SPO2_INTERVAL_MS = 45_000L
private const val SPO2_BURST_COUNT = 8
private const val SPO2_BURST_DELAY_MS = 200L

data class SensorTrackerEvent(
    val sensor: SensorType,
    val state: TrackerState,
)

@Singleton
class VitalsSensorCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val platformHub = PlatformSensorHub(context)
    private val continuousSensors = listOf(SensorType.HEART_RATE, SensorType.SKIN_TEMPERATURE)

    fun allVitalsFlow(): Flow<SensorTrackerEvent> = callbackFlow {
        (continuousSensors + SensorType.SPO2).forEach {
            trySend(SensorTrackerEvent(it, TrackerState.Connecting))
        }

        val platformStarted = SensorType.entries.associateWith { sensor ->
            platformHub.start(sensor) { state ->
                trySend(SensorTrackerEvent(sensor, state))
            }
        }

        var healthTrackingService: HealthTrackingService? = null
        val activeTrackers = mutableMapOf<SensorType, HealthTracker>()

        fun emitMeasuring(sensor: SensorType, dp: DataPoint) {
            logDataPointContents(dp)
            when (sensor) {
                SensorType.SPO2 -> {
                    val reading = SpO2SamsungParser.parse(dp)
                    if (reading != null) {
                        trySend(SensorTrackerEvent(sensor, TrackerState.Measuring(reading)))
                    } else if (SpO2SamsungParser.isMeasuring(dp)) {
                        trySend(
                            SensorTrackerEvent(
                                sensor,
                                TrackerState.Measuring(VitalReading(status = HeartRateStatus.INITIAL)),
                            ),
                        )
                    }
                }
                SensorType.HEART_RATE -> {
                    trySend(
                        SensorTrackerEvent(
                            sensor,
                            TrackerState.Measuring(HeartRateSamsungParser.parse(dp)),
                        ),
                    )
                }
                SensorType.SKIN_TEMPERATURE -> {
                    SkinTemperatureSamsungParser.parse(dp)?.let { reading ->
                        trySend(SensorTrackerEvent(sensor, TrackerState.Measuring(reading)))
                    }
                }
            }
        }

        fun createListener(sensor: SensorType) = object : HealthTracker.TrackerEventListener {
            override fun onDataReceived(dataPoints: List<DataPoint>) {
                dataPoints.forEach { emitMeasuring(sensor, it) }
            }

            override fun onError(error: HealthTracker.TrackerError) {
                Log.e(TAG, "Samsung ${sensor.name} error: $error")
                if (error == HealthTracker.TrackerError.PERMISSION_ERROR) {
                    trySend(
                        SensorTrackerEvent(
                            sensor,
                            TrackerState.Error(WatchPermissions.deniedMessage(sensor)),
                        ),
                    )
                }
            }

            override fun onFlushCompleted() {
                Log.d(TAG, "Samsung ${sensor.name} flush completed")
            }
        }

        fun startTracker(sensor: SensorType) {
            val trackerType = SamsungTrackerResolver.resolve(sensor) ?: run {
                Log.w(TAG, "No Samsung tracker for ${sensor.name}")
                if (platformStarted[sensor] != true) {
                    trySend(SensorTrackerEvent(sensor, unsupportedMessage(sensor)))
                }
                return
            }
            try {
                val tracker = healthTrackingService?.getHealthTracker(trackerType) ?: return
                tracker.setEventListener(createListener(sensor))
                activeTrackers[sensor] = tracker
                Log.i(TAG, "Tracker active: ${trackerType.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Tracker init failed for ${sensor.name}: ${e.message}")
                if (platformStarted[sensor] != true) {
                    trySend(
                        SensorTrackerEvent(
                            sensor,
                            TrackerState.Error(e.message ?: "Sensor start failed"),
                        ),
                    )
                }
            }
        }

        val connectionListener = object : ConnectionListener {
            override fun onConnectionSuccess() {
                Log.i(TAG, "Samsung Health connected")
                continuousSensors.forEach { startTracker(it) }
                startTracker(SensorType.SPO2)

                launch {
                    delay(2_000L)
                    while (isActive) {
                        continuousSensors.forEach { sensor ->
                            runCatching { activeTrackers[sensor]?.flush() }
                        }
                        delay(CONTINUOUS_FLUSH_MS)
                    }
                }

                launch {
                    delay(5_000L)
                    while (isActive) {
                        val spo2 = activeTrackers[SensorType.SPO2]
                        if (spo2 != null) {
                            repeat(SPO2_BURST_COUNT) {
                                runCatching { spo2.flush() }
                                delay(SPO2_BURST_DELAY_MS)
                            }
                        }
                        delay(SPO2_INTERVAL_MS)
                    }
                }
            }

            override fun onConnectionEnded() {
                SensorType.entries.forEach {
                    trySend(SensorTrackerEvent(it, TrackerState.Disconnected))
                }
            }

            override fun onConnectionFailed(exception: HealthTrackerException) {
                Log.e(TAG, "Samsung connection failed: ${exception.message}")
                SensorType.entries.forEach { sensor ->
                    if (platformStarted[sensor] != true) {
                        trySend(
                            SensorTrackerEvent(
                                sensor,
                                TrackerState.Error(
                                    exception.message ?: "Samsung Health unavailable",
                                    exception.errorCode,
                                ),
                            ),
                        )
                    }
                }
            }
        }

        try {
            healthTrackingService = HealthTrackingService(connectionListener, context)
            healthTrackingService?.connectService()
        } catch (e: Exception) {
            Log.e(TAG, "HealthTrackingService failed: ${e.message}")
            SensorType.entries.forEach { sensor ->
                if (platformStarted[sensor] != true) {
                    trySend(
                        SensorTrackerEvent(
                            sensor,
                            TrackerState.Error(e.message ?: "Sensor SDK unavailable"),
                        ),
                    )
                }
            }
        }

        awaitClose {
            platformHub.stop()
            activeTrackers.values.forEach { runCatching { it.unsetEventListener() } }
            activeTrackers.clear()
            runCatching { healthTrackingService?.disconnectService() }
        }
    }

    private fun unsupportedMessage(sensor: SensorType): TrackerState.Error = when (sensor) {
        SensorType.SPO2 -> TrackerState.Error("SpO₂ not supported on this watch")
        SensorType.SKIN_TEMPERATURE -> TrackerState.Error("Skin temperature needs Galaxy Watch5+")
        else -> TrackerState.Error("Sensor not available")
    }
}
