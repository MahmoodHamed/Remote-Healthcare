package com.rpm.watch.sensor.platform

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/** Android SensorManager fallback for SpO₂ on Galaxy Watch 8. */
class PlatformSpO2Reader(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private var listener: SensorEventListener? = null

    fun start(onPercent: (Float) -> Unit): Boolean {
        val sm = sensorManager ?: return false
        val type = resolveSensorType("TYPE_OXYGEN_SATURATION", "TYPE_SPO2") ?: return false
        val sensor = sm.getDefaultSensor(type) ?: return false
        stop()
        val l = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val pct = event?.values?.firstOrNull() ?: return
                if (event.sensor.type == type && pct > 0f) onPercent(pct)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        listener = l
        sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        return true
    }

    fun stop() {
        val sm = sensorManager ?: return
        listener?.let { runCatching { sm.unregisterListener(it) } }
        listener = null
    }
}
