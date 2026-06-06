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

// HR: flush every 2s — tight loop needed for real-time monitoring.
private const val HR_FLUSH_MS = 2_000L

// Skin temp and EDA: flush far less often — these change slowly.
private const val SKIN_TEMP_FLUSH_MS = 5 * 60_000L   // 5 minutes
private const val EDA_FLUSH_MS       = 5 * 60_000L   // 5 minutes

// SpO₂ autonomous schedule: independent measurement every 3 minutes.
// Samsung needs ~15–30 s per measurement; we give the slot 40 s to complete.
private const val SPO2_INTERVAL_MS    = 3 * 60_000L  // 3 minutes between measurements
private const val SPO2_SLOT_MS        = 40_000L       // max time for one measurement

// On-demand burst: flush rapidly after setEventListener to retrieve buffered data.
private const val ON_DEMAND_BURST_COUNT    = 6
private const val ON_DEMAND_BURST_DELAY_MS = 500L

data class SensorTrackerEvent(
    val sensor: SensorType,
    val state: TrackerState,
)

@Singleton
class VitalsSensorCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * BIA and ECG require user interaction (touch crown + side keys) and have no real-time
     * medical value at sub-minute intervals. They are only triggered by explicit user action.
     * SpO₂ is scheduled autonomously every [SPO2_INTERVAL_MS].
     */
    private val manualOnDemandRequests = MutableSharedFlow<SensorType>(extraBufferCapacity = 4)

    /** Trigger BIA or ECG measurement manually (called from UI/service). */
    fun requestOnDemandMeasurement(sensor: SensorType) {
        manualOnDemandRequests.tryEmit(sensor)
    }

    private val platformHub = PlatformSensorHub(context)

    // HR is the only truly continuous sensor; skin temp and EDA are registered continuous
    // but flushed at a much lower rate to save power.
    private val continuousSensors = listOf(
        SensorType.HEART_RATE,
        SensorType.SKIN_TEMPERATURE,
        SensorType.EDA,
    )
    // Samsung allows only one on-demand listener at a time.
    private val onDemandSensors = listOf(SensorType.SPO2, SensorType.BIA, SensorType.ECG)
    private val monitoredSensors = continuousSensors + onDemandSensors

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

        /**
         * Activates a single on-demand tracker. Clears all other on-demand listeners first
         * (Samsung constraint: only one active at a time).
         */
        fun switchOnDemand(sensor: SensorType) {
            onDemandSensors.forEach { other ->
                if (other != sensor) runCatching { activeTrackers[other]?.unsetEventListener() }
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

        // Manual BIA / ECG requests from UI.
        launch {
            manualOnDemandRequests.collect { sensor ->
                if (activeTrackers.isEmpty()) {
                    Log.w(TAG, "On-demand request ignored — Samsung not connected yet: ${sensor.name}")
                    trySend(SensorTrackerEvent(sensor, TrackerState.Error("Sensors not ready — tap Start first")))
                } else {
                    Log.i(TAG, "Manual on-demand: ${sensor.name}")
                    switchOnDemand(sensor)
                }
            }
        }

        val connectionListener = object : ConnectionListener {
            override fun onConnectionSuccess() {
                Log.i(TAG, "Samsung Health connected")
                continuousSensors.forEach { startContinuous(it) }

                // HR: flush every 2 s for real-time monitoring.
                launch {
                    delay(2_000L)
                    while (isActive) {
                        runCatching { activeTrackers[SensorType.HEART_RATE]?.flush() }
                        delay(HR_FLUSH_MS)
                    }
                }

                // Skin temperature: registered continuous but flushed every 5 min.
                // Temperature changes slowly — 2 s flush interval is pure battery waste.
                launch {
                    delay(10_000L)
                    while (isActive) {
                        runCatching { activeTrackers[SensorType.SKIN_TEMPERATURE]?.flush() }
                        delay(SKIN_TEMP_FLUSH_MS)
                    }
                }

                // EDA: registered continuous but flushed every 5 min.
                // Stress arousal is a slow physiological response.
                launch {
                    delay(15_000L)
                    while (isActive) {
                        runCatching { activeTrackers[SensorType.EDA]?.flush() }
                        delay(EDA_FLUSH_MS)
                    }
                }

                // SpO₂: autonomous measurement every SPO2_INTERVAL_MS.
                // Independent of BIA/ECG — no longer blocked by rotation.
                launch {
                    delay(5_000L)
                    while (isActive) {
                        Log.i(TAG, "SpO₂ scheduled measurement starting")
                        switchOnDemand(SensorType.SPO2)
                        delay(SPO2_SLOT_MS)
                        // Release SpO₂ listener so manual BIA/ECG can take the slot.
                        runCatching { activeTrackers[SensorType.SPO2]?.unsetEventListener() }
                        activeOnDemand = null
                        delay(SPO2_INTERVAL_MS - SPO2_SLOT_MS)
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
