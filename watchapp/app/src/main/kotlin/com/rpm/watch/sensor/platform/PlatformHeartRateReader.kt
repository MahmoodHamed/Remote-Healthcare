package com.rpm.watch.sensor.platform

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/** Android SensorManager fallback for heart rate on Galaxy Watch 8. */
class PlatformHeartRateReader(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private var listener: SensorEventListener? = null

    fun start(onBpm: (Int) -> Unit): Boolean {
        val sm = sensorManager ?: return false
        val sensor = sm.getDefaultSensor(Sensor.TYPE_HEART_RATE) ?: return false
        stop()
        val l = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val bpm = event?.values?.firstOrNull()?.toInt() ?: return
                if (bpm > 0) onBpm(bpm)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        listener = l
        sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_UI)
        return true
    }

    fun stop() {
        val sm = sensorManager ?: return
        listener?.let { runCatching { sm.unregisterListener(it) } }
        listener = null
    }
}
