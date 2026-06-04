package com.rpm.watch.sensor.wear

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "WearDetection"
/** Require this many consecutive "off wrist" samples before clearing vitals. */
private const val OFF_WRIST_STREAK_REQUIRED = 3

/**
 * Samsung off-body sensor: 1 = on wrist, 0 = off wrist.
 * See Samsung HR Tracker sample (TYPE_LOW_LATENCY_OFFBODY_DETECT).
 */
class WearDetectionHub(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private var listener: SensorEventListener? = null
    private var offWristStreak = 0

    private val _hasOffBodySensor = MutableStateFlow(false)
    val hasOffBodySensor: StateFlow<Boolean> = _hasOffBodySensor.asStateFlow()

    /** Latest raw reading; null until first event. */
    private val _onWrist = MutableStateFlow<Boolean?>(null)
    val onWrist: StateFlow<Boolean?> = _onWrist.asStateFlow()

    /**
     * True only after a stable off-wrist signal (avoids clearing HR on noisy first samples).
     */
    private val _confirmedOffWrist = MutableStateFlow(false)
    val confirmedOffWrist: StateFlow<Boolean> = _confirmedOffWrist.asStateFlow()

    fun start() {
        val sm = sensorManager ?: return
        stop()
        val sensor = findOffBodySensor(sm) ?: run {
            Log.w(TAG, "Off-body sensor not available — HR gated by Samsung status only")
            _hasOffBodySensor.value = false
            _confirmedOffWrist.value = false
            return
        }
        _hasOffBodySensor.value = true
        val l = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val value = event?.values?.firstOrNull()?.toInt() ?: return
                val worn = value == 1
                _onWrist.value = worn
                if (worn) {
                    offWristStreak = 0
                    if (_confirmedOffWrist.value) {
                        Log.i(TAG, "Watch on wrist again")
                    }
                    _confirmedOffWrist.value = false
                } else {
                    offWristStreak++
                    if (offWristStreak >= OFF_WRIST_STREAK_REQUIRED && !_confirmedOffWrist.value) {
                        Log.i(TAG, "Watch off wrist (confirmed, raw=$value)")
                        _confirmedOffWrist.value = true
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        listener = l
        sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        Log.i(TAG, "Off-body sensor registered: ${sensor.name}")
    }

    fun stop() {
        listener?.let { sensorManager?.unregisterListener(it) }
        listener = null
        offWristStreak = 0
        _confirmedOffWrist.value = false
    }

    private fun findOffBodySensor(sm: SensorManager): Sensor? {
        resolveSensorType("TYPE_LOW_LATENCY_OFFBODY_DETECT")?.let { type ->
            sm.getDefaultSensor(type)?.let { return it }
        }
        return sm.getSensorList(Sensor.TYPE_ALL).firstOrNull { s ->
            s.stringType.contains("OFFBODY", ignoreCase = true) ||
                s.name.contains("off body", ignoreCase = true) ||
                s.name.contains("offbody", ignoreCase = true)
        }
    }

    private fun resolveSensorType(name: String): Int? = try {
        Sensor::class.java.getField(name).getInt(null)
    } catch (_: Exception) {
        null
    }
}
