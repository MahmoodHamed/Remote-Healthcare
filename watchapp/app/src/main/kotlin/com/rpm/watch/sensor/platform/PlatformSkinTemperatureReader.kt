package com.rpm.watch.sensor.platform

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.rpm.watch.sensor.temperature.SkinTemperatureSamsungParser

/** Android SensorManager fallback for skin temperature on Galaxy Watch 8. */
class PlatformSkinTemperatureReader(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private var listener: SensorEventListener? = null

    fun findSensor(sm: SensorManager): Sensor? {
        resolveSensorType("TYPE_SKIN_TEMPERATURE")?.let { type ->
            sm.getDefaultSensor(type)?.let { return it }
        }
        return sm.getSensorList(Sensor.TYPE_ALL).firstOrNull { sensor ->
            val typeName = sensor.stringType.uppercase()
            val name = sensor.name.uppercase()
            typeName.contains("SKIN") && typeName.contains("TEMP") ||
                name.contains("SKIN") && name.contains("TEMP")
        }
    }

    fun start(onCelsius: (Float) -> Unit): Boolean {
        val sm = sensorManager ?: return false
        val sensor = findSensor(sm) ?: return false
        val type = sensor.type
        stop()
        val l = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val c = event?.values?.firstOrNull() ?: return
                if (event.sensor.type == type && SkinTemperatureSamsungParser.isPlausible(c)) {
                    onCelsius(c)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        listener = l
        sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_FASTEST)
        return true
    }

    fun stop() {
        val sm = sensorManager ?: return
        listener?.let { runCatching { sm.unregisterListener(it) } }
        listener = null
    }
}
