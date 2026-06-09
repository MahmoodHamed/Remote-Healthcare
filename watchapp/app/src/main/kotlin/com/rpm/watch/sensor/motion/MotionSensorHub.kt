package com.rpm.watch.sensor.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Steps + fall detection. Fall only counted when the watch is on the wrist.
 */
class MotionSensorHub(context: Context) {

    data class Snapshot(
        val stepsCount: Int?,
        val caloriesBurned: Float?,
        val fallDetected: Boolean,
    )

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private var listener: SensorEventListener? = null
    private var baseStepCounter: Float? = null
    private var latestSteps = 0
    private var latestCalories = 0f
    private var fallDetectedUntilMs = 0L

    @Volatile private var watchOnWrist = true
    private var lastAccelG = 9.8f
    private var peakAccelG = 9.8f
    private var lowAccelSinceMs: Long = 0L

    fun setWatchOnWrist(onWrist: Boolean) {
        watchOnWrist = onWrist
        if (!onWrist) {
            fallDetectedUntilMs = 0L
            peakAccelG = 9.8f
            lowAccelSinceMs = 0L
        }
    }

    fun start() {
        val sm = sensorManager ?: return
        stop()
        val stepSensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val accelSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (stepSensor == null && accelSensor == null) return

        val l = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val e = event ?: return
                when (e.sensor.type) {
                    Sensor.TYPE_STEP_COUNTER -> {
                        val raw = e.values.firstOrNull() ?: return
                        if (baseStepCounter == null) {
                            baseStepCounter = raw
                            latestSteps = 0
                            latestCalories = 0f
                        } else {
                            latestSteps = (raw - baseStepCounter!!).toInt().coerceAtLeast(0)
                            latestCalories = latestSteps * 0.04f
                        }
                    }
                    Sensor.TYPE_ACCELEROMETER -> processAccelerometer(e)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        listener = l
        stepSensor?.let { sm.registerListener(l, it, SensorManager.SENSOR_DELAY_NORMAL) }
        // SENSOR_DELAY_GAME (~50 Hz) is excessive for fall detection.
        // 20 ms sampling (50 Hz custom) with 200 ms max report latency gives Sensor Batching:
        // the hardware buffers events and wakes the CPU only ~5 times/second instead of 50.
        accelSensor?.let {
            sm.registerListener(l, it, 20_000 /* µs = 50 Hz */, 200_000 /* µs max latency */)
        }
    }

    private fun processAccelerometer(e: SensorEvent) {
        if (!watchOnWrist || e.values.size < 3) return

        val x = e.values[0]
        val y = e.values[1]
        val z = e.values[2]
        val g = sqrt(x * x + y * y + z * z)
        val now = System.currentTimeMillis()

        val jerk = kotlin.math.abs(g - lastAccelG)
        lastAccelG = g

        // Impact phase: strong shock (> ~3.5g)
        if (g > 35f || jerk > 18f) {
            peakAccelG = g
            lowAccelSinceMs = 0L
        }

        // Free-fall phase after impact: very low acceleration
        if (peakAccelG > 35f && g < 12f) {
            if (lowAccelSinceMs == 0L) lowAccelSinceMs = now
            if (now - lowAccelSinceMs > 120L) {
                fallDetectedUntilMs = now + 15_000L
                peakAccelG = 9.8f
                lowAccelSinceMs = 0L
            }
        } else if (g > 20f) {
            peakAccelG = g
            lowAccelSinceMs = 0L
        }
    }

    fun stop() {
        listener?.let { sensorManager?.unregisterListener(it) }
        listener = null
    }

    fun snapshot(): Snapshot {
        val now = System.currentTimeMillis()
        return Snapshot(
            stepsCount = latestSteps.takeIf { baseStepCounter != null },
            caloriesBurned = latestCalories.takeIf { baseStepCounter != null },
            fallDetected = watchOnWrist && now < fallDetectedUntilMs,
        )
    }
}
