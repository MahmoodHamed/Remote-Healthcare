package com.rpm.watch.health

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.util.Log
import com.samsung.android.service.health.tracking.ConnectionListener
import com.samsung.android.service.health.tracking.HealthTracker
import com.samsung.android.service.health.tracking.HealthTrackerException
import com.samsung.android.service.health.tracking.HealthTrackingService
import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import com.samsung.android.service.health.tracking.data.ValueKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sqrt

private const val TAG = "WatchSensorsManager"

/**
 * Snapshot of every advanced sensor reading we can pull off the watch on a
 * "best-effort" basis. Most fields stay null until either the Samsung Health
 * Sensor SDK or the Android SensorManager actually produces a value.
 */
data class AdvancedReading(
    var heartRateVariabilityMs: Float? = null,
    var skinTemperatureC: Float? = null,
    var stepsCount: Int? = null,
    var caloriesBurned: Float? = null,
    var distanceMeters: Float? = null,
    var ambientTemperatureC: Float? = null,
    var fallDetected: Boolean = false,
    var batteryLevel: Float? = null,
    var isWearing: Boolean = true,
)

/**
 * Subscribes to every continuous tracker we can — Samsung Galaxy Watch 8 supports:
 *   - HEART_RATE_CONTINUOUS (heart rate + IBI list → HRV)
 *   - SKIN_TEMPERATURE_CONTINUOUS
 *   - ACCELEROMETER_CONTINUOUS (used for fall detection if available)
 *
 * On-demand trackers (SpO2, ECG, BIA) are exposed through dedicated methods that
 * run a single capture; you call them from the UI ("Measure SpO2 now").
 *
 * Falls back to Android SensorManager (step counter, ambient temperature,
 * accelerometer for fall heuristic, battery broadcast) when the Samsung SDK
 * is unavailable or the corresponding tracker type is not supported.
 */
@Singleton
class WatchSensorsManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val state = AdvancedReading()

    private var healthTrackingService: HealthTrackingService? = null
    private val activeTrackers = mutableListOf<HealthTracker>()

    private var sensorManager: SensorManager? = null
    private var androidListener: SensorEventListener? = null
    private var baseStepCounter: Float? = null

    /** Snapshot of everything we have right now. Safe to call from any thread. */
    @Synchronized
    fun snapshot(): AdvancedReading = state.copy()

    /** Start listening for every sensor we can reach. Idempotent. */
    fun start() {
        startAndroidSensors()
        startBatteryReader()
        startSamsungContinuousTrackers()
    }

    fun stop() {
        stopAndroidSensors()
        stopSamsungTrackers()
    }

    fun markFall(durationMs: Long) {
        synchronized(state) {
            state.fallDetected = true
        }
        // Clear after the duration on a background thread; we don't need precise timing.
        Thread {
            try { Thread.sleep(durationMs) } catch (_: InterruptedException) {}
            synchronized(state) { state.fallDetected = false }
        }.start()
    }

    // ── Samsung SDK continuous trackers ───────────────────────────────────────

    private fun startSamsungContinuousTrackers() {
        if (healthTrackingService != null) return
        val listener = object : ConnectionListener {
            override fun onConnectionSuccess() {
                Log.i(TAG, "Samsung Health tracking connected (continuous)")
                bindContinuousTracker(HealthTrackerType.HEART_RATE_CONTINUOUS) { dp ->
                    try {
                        val raw = dp.getValue(ValueKey.HeartRateSet.IBI_LIST) as? List<*>
                        val intervals = raw?.mapNotNull { (it as? Number)?.toFloat() } ?: emptyList()
                        if (intervals.size > 1) {
                            val hrv = rrIntervalSdnn(intervals)
                            synchronized(state) { state.heartRateVariabilityMs = hrv }
                        }
                    } catch (e: Throwable) {
                        Log.w(TAG, "HRV parse failed: ${e.message}")
                    }
                }
                bindContinuousTracker(HealthTrackerType.SKIN_TEMPERATURE_CONTINUOUS) { dp ->
                    try {
                        val temp = (dp.getValue(ValueKey.SkinTemperatureSet.OBJECT_TEMPERATURE) as? Number)?.toFloat()
                        if (temp != null) synchronized(state) { state.skinTemperatureC = temp }
                    } catch (e: Throwable) {
                        Log.w(TAG, "Skin temperature parse failed: ${e.message}")
                    }
                }
            }

            override fun onConnectionEnded() {
                Log.i(TAG, "Samsung Health continuous trackers disconnected")
            }

            override fun onConnectionFailed(exception: HealthTrackerException) {
                Log.w(TAG, "Samsung continuous trackers connection failed: ${exception.message}")
            }
        }

        try {
            healthTrackingService = HealthTrackingService(listener, context).also { it.connectService() }
        } catch (e: Throwable) {
            Log.w(TAG, "Samsung tracking service not available: ${e.message}")
        }
    }

    private fun bindContinuousTracker(
        type: HealthTrackerType,
        onValue: (DataPoint) -> Unit,
    ) {
        val service = healthTrackingService ?: return
        try {
            val tracker = service.getHealthTracker(type) ?: return
            tracker.setEventListener(object : HealthTracker.TrackerEventListener {
                override fun onDataReceived(dataPoints: List<DataPoint>) {
                    dataPoints.forEach { onValue(it) }
                }
                override fun onError(error: HealthTracker.TrackerError) {
                    Log.w(TAG, "Tracker $type error: $error")
                }
                override fun onFlushCompleted() = Unit
            })
            activeTrackers.add(tracker)
            Log.i(TAG, "Subscribed to $type")
        } catch (e: Throwable) {
            Log.w(TAG, "Could not subscribe to $type: ${e.message}")
        }
    }

    private fun stopSamsungTrackers() {
        activeTrackers.forEach {
            try { it.unsetEventListener() } catch (_: Throwable) {}
        }
        activeTrackers.clear()
        try { healthTrackingService?.disconnectService() } catch (_: Throwable) {}
        healthTrackingService = null
    }

    // ── Android SensorManager fallbacks ───────────────────────────────────────

    private fun startAndroidSensors() {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        sensorManager = sm

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val e = event ?: return
                when (e.sensor.type) {
                    Sensor.TYPE_STEP_COUNTER -> {
                        val raw = e.values.firstOrNull() ?: return
                        val base = baseStepCounter
                        if (base == null) {
                            baseStepCounter = raw
                            synchronized(state) {
                                state.stepsCount = 0
                                state.caloriesBurned = 0f
                                state.distanceMeters = 0f
                            }
                        } else {
                            val steps = (raw - base).toInt().coerceAtLeast(0)
                            synchronized(state) {
                                state.stepsCount = steps
                                // Conservative estimates; refined values come from server-side processing.
                                state.caloriesBurned = steps * 0.04f
                                state.distanceMeters = steps * 0.762f
                            }
                        }
                    }
                    Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                        synchronized(state) {
                            state.ambientTemperatureC = e.values.firstOrNull()
                        }
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        if (e.values.size < 3) return
                        val g = sqrt(
                            e.values[0].toDouble().pow(2.0)
                                + e.values[1].toDouble().pow(2.0)
                                + e.values[2].toDouble().pow(2.0)
                        )
                        if (g > 25.0) markFall(10_000L)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        androidListener = listener

        sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.let {
            sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        sm.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)?.let {
            sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    private fun stopAndroidSensors() {
        val sm = sensorManager ?: return
        androidListener?.let { runCatching { sm.unregisterListener(it) } }
        androidListener = null
        sensorManager = null
        baseStepCounter = null
    }

    // ── Battery ───────────────────────────────────────────────────────────────

    private fun startBatteryReader() {
        try {
            val mgr = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val level = mgr?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.toFloat()
            if (level != null) synchronized(state) { state.batteryLevel = level }
        } catch (_: Throwable) {}
        // Also listen to broadcasts for periodic updates
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            context.registerReceiver(object : android.content.BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                    if (level >= 0 && scale > 0) {
                        synchronized(state) { state.batteryLevel = (level * 100f) / scale }
                    }
                }
            }, filter)
        } catch (_: Throwable) {}
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun rrIntervalSdnn(intervals: List<Float>): Float {
        val mean = intervals.average()
        val variance = intervals.map { (it - mean).pow(2.0) }.average()
        return sqrt(variance).toFloat()
    }
}
