package com.rpm.watch.sensor

import android.content.Context
import android.util.Log
import com.rpm.watch.WatchPermissions
import com.rpm.watch.sensor.bia.BiaSamsungParser
import com.rpm.watch.sensor.ecg.EcgSamsungParser
import com.rpm.watch.sensor.eda.EdaSamsungParser
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "VitalsSensorCoordinator"
private const val CONTINUOUS_FLUSH_MS = 2_000L
private const val ON_DEMAND_BURST_COUNT = 8
private const val ON_DEMAND_BURST_DELAY_MS = 200L
private const val SLOT_SPO2_MS = 45_000L
private const val SLOT_BIA_MS = 90_000L
private const val SLOT_ECG_MS = 60_000L

data class SensorTrackerEvent(
    val sensor: SensorType,
    val state: TrackerState,
)

@Singleton
class VitalsSensorCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val onDemandRequests = MutableSharedFlow<SensorType>(extraBufferCapacity = 2)

    /** Request an immediate on-demand measurement (ECG, SpO₂, BIA). */
    fun requestOnDemandMeasurement(sensor: SensorType) {
        onDemandRequests.tryEmit(sensor)
    }

    private val platformHub = PlatformSensorHub(context)
    private val continuousSensors = listOf(
        SensorType.HEART_RATE,
        SensorType.SKIN_TEMPERATURE,
        SensorType.EDA,
    )
    /** Samsung allows only one on-demand tracker listener at a time. */
    private val onDemandRotation = listOf(
        SensorType.SPO2,
        SensorType.BIA,
        SensorType.ECG,
    )
    private val monitoredSensors = continuousSensors + onDemandRotation

    fun allVitalsFlow(): Flow<SensorTrackerEvent> = callbackFlow {
        monitoredSensors.forEach {
            trySend(SensorTrackerEvent(it, TrackerState.Connecting))
        }

        val platformStarted = SensorType.entries.associateWith { sensor ->
            platformHub.start(sensor) { state ->
                trySend(SensorTrackerEvent(sensor, state))
            }
        }

        var healthTrackingService: HealthTrackingService? = null
        val activeTrackers = mutableMapOf<SensorType, HealthTracker>()
        var activeOnDemand: SensorType? = null

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
                SensorType.BIA -> {
                    val reading = BiaSamsungParser.parse(dp)
                    if (reading != null) {
                        trySend(SensorTrackerEvent(sensor, TrackerState.Measuring(reading)))
                    } else if (BiaSamsungParser.isMeasuring(dp)) {
                        trySend(
                            SensorTrackerEvent(
                                sensor,
                                TrackerState.Measuring(VitalReading(status = HeartRateStatus.INITIAL)),
                            ),
                        )
                    }
                }
                SensorType.ECG -> {
                    val reading = EcgSamsungParser.parse(dp)
                    if (reading != null) {
                        trySend(SensorTrackerEvent(sensor, TrackerState.Measuring(reading)))
                    } else if (EcgSamsungParser.isMeasuring(dp)) {
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
                SensorType.EDA -> {
                    EdaSamsungParser.parse(dp)?.let { reading ->
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

        fun ensureTracker(sensor: SensorType): HealthTracker? {
            activeTrackers[sensor]?.let { return it }
            val trackerType = SamsungTrackerResolver.resolve(sensor) ?: return null
            return try {
                val tracker = healthTrackingService?.getHealthTracker(trackerType) ?: return null
                activeTrackers[sensor] = tracker
                Log.i(TAG, "Tracker ready: ${trackerType.name}")
                tracker
            } catch (e: Exception) {
                Log.e(TAG, "Tracker init failed for ${sensor.name}: ${e.message}")
                null
            }
        }

        fun startContinuous(sensor: SensorType) {
            val tracker = ensureTracker(sensor) ?: run {
                if (platformStarted[sensor] != true) {
                    trySend(SensorTrackerEvent(sensor, unsupportedMessage(sensor)))
                }
                return
            }
            try {
                tracker.setEventListener(createListener(sensor))
            } catch (e: Exception) {
                Log.e(TAG, "Continuous start failed ${sensor.name}: ${e.message}")
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

        fun switchOnDemand(sensor: SensorType) {
            onDemandRotation.forEach { other ->
                if (other != sensor) {
                    runCatching { activeTrackers[other]?.unsetEventListener() }
                }
            }
            val tracker = ensureTracker(sensor) ?: run {
                if (platformStarted[sensor] != true) {
                    trySend(SensorTrackerEvent(sensor, unsupportedMessage(sensor)))
                }
                return
            }
            try {
                tracker.setEventListener(createListener(sensor))
                activeOnDemand = sensor
                Log.i(TAG, "On-demand active: ${sensor.name}")
                launch {
                    repeat(ON_DEMAND_BURST_COUNT) {
                        runCatching { tracker.flush() }
                        delay(ON_DEMAND_BURST_DELAY_MS)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "On-demand start failed ${sensor.name}: ${e.message}")
            }
        }

        fun slotDuration(sensor: SensorType): Long = when (sensor) {
            SensorType.SPO2 -> SLOT_SPO2_MS
            SensorType.BIA -> SLOT_BIA_MS
            SensorType.ECG -> SLOT_ECG_MS
            else -> SLOT_SPO2_MS
        }

        launch {
            onDemandRequests.collect { sensor ->
                if (activeTrackers.isEmpty()) {
                    Log.w(TAG, "On-demand request ignored — Samsung not connected yet: ${sensor.name}")
                    trySend(
                        SensorTrackerEvent(
                            sensor,
                            TrackerState.Error("Sensors not ready — tap Start first"),
                        ),
                    )
                } else {
                    switchOnDemand(sensor)
                }
            }
        }

        val connectionListener = object : ConnectionListener {
            override fun onConnectionSuccess() {
                Log.i(TAG, "Samsung Health connected")
                continuousSensors.forEach { startContinuous(it) }

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
                    var index = 0
                    while (isActive) {
                        val sensor = onDemandRotation[index % onDemandRotation.size]
                        switchOnDemand(sensor)
                        delay(slotDuration(sensor))
                        index++
                    }
                }
            }

            override fun onConnectionEnded() {
                monitoredSensors.forEach {
                    trySend(SensorTrackerEvent(it, TrackerState.Disconnected))
                }
            }

            override fun onConnectionFailed(exception: HealthTrackerException) {
                Log.e(TAG, "Samsung connection failed: ${exception.message}")
                monitoredSensors.forEach { sensor ->
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
            monitoredSensors.forEach { sensor ->
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
            activeOnDemand = null
            runCatching { healthTrackingService?.disconnectService() }
        }
    }

    private fun unsupportedMessage(sensor: SensorType): TrackerState.Error = when (sensor) {
        SensorType.SPO2 -> TrackerState.Error("SpO₂ not supported on this watch")
        SensorType.SKIN_TEMPERATURE -> TrackerState.Error("Skin temperature needs Galaxy Watch5+")
        SensorType.EDA -> TrackerState.Error("EDA (stress) not supported on this watch")
        SensorType.BIA -> TrackerState.Error("Body composition not supported on this watch")
        SensorType.ECG -> TrackerState.Error("ECG not supported on this watch")
        else -> TrackerState.Error("Sensor not available")
    }
}
