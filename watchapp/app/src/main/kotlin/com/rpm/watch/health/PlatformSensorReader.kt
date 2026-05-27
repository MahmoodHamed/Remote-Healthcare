package com.rpm.watch.health

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

private const val TAG = "PlatformSensorReader"

/**
 * Reads Galaxy Watch vitals through Android [SensorManager] when the Samsung Health
 * Sensor SDK is unavailable or not yet delivering data.
 */
class PlatformSensorReader(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private var listener: SensorEventListener? = null
    private var registeredTypes = mutableSetOf<Int>()

    fun start(
        readHeartRate: ((Int) -> Unit)? = null,
        readSpO2: ((Float) -> Unit)? = null,
        readTemperature: ((Float) -> Unit)? = null
    ): Boolean {
        val sm = sensorManager ?: return false
        stop()

        val hrSensor = sm.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        val tempType = resolveSensorType("TYPE_SKIN_TEMPERATURE", "TYPE_AMBIENT_TEMPERATURE")
        val spo2Type = resolveSensorType("TYPE_OXYGEN_SATURATION", "TYPE_SPO2")
        val tempSensor = tempType?.let { sm.getDefaultSensor(it) }
        val spo2Sensor = spo2Type?.let { sm.getDefaultSensor(it) }

        Log.i(
            TAG,
            "Starting platform sensors: hr=${hrSensor != null}, temp=${tempSensor != null}, spo2=${spo2Sensor != null}"
        )

        if (hrSensor == null && tempSensor == null && spo2Sensor == null) return false

        val l = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val e = event ?: return
                val value = e.values.firstOrNull() ?: return
                when (e.sensor.type) {
                    Sensor.TYPE_HEART_RATE -> {
                        val bpm = value.toInt()
                        if (bpm > 0) readHeartRate?.invoke(bpm)
                    }
                }
                if (tempType != null && e.sensor.type == tempType) {
                    readTemperature?.invoke(value)
                }
                if (spo2Type != null && e.sensor.type == spo2Type) {
                    readSpO2?.invoke(value)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        listener = l
        if (hrSensor != null && readHeartRate != null) {
            sm.registerListener(l, hrSensor, SensorManager.SENSOR_DELAY_UI)
            registeredTypes.add(Sensor.TYPE_HEART_RATE)
        }
        if (tempSensor != null && readTemperature != null) {
            sm.registerListener(l, tempSensor, SensorManager.SENSOR_DELAY_NORMAL)
            registeredTypes.add(tempSensor.type)
        }
        if (spo2Sensor != null && readSpO2 != null) {
            sm.registerListener(l, spo2Sensor, SensorManager.SENSOR_DELAY_NORMAL)
            registeredTypes.add(spo2Sensor.type)
        }
        return true
    }

    fun stop() {
        val sm = sensorManager ?: return
        listener?.let { l ->
            try {
                sm.unregisterListener(l)
            } catch (_: Exception) {
            }
        }
        listener = null
        registeredTypes.clear()
    }

    private fun resolveSensorType(vararg names: String): Int? {
        for (name in names) {
            try {
                return Sensor::class.java.getField(name).getInt(null)
            } catch (_: Exception) {
            }
        }
        return null
    }
}
