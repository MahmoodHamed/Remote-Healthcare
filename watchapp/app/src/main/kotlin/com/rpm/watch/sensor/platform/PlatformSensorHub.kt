package com.rpm.watch.sensor.platform

import android.content.Context
import android.util.Log
import com.rpm.watch.sensor.SensorType
import com.rpm.watch.sensor.TrackerState

private const val TAG = "PlatformSensorHub"

/**
 * Platform fallback — **not** used for heart rate (stale/false BPM when watch is off).
 * Samsung SDK is the only source for HR on Galaxy Watch 8.
 */
class PlatformSensorHub(context: Context) {

    private val skinTempReader = PlatformSkinTemperatureReader(context)
    private val spO2Reader = PlatformSpO2Reader(context)

    fun start(sensor: SensorType, emit: (TrackerState) -> Unit): Boolean = when (sensor) {
        SensorType.HEART_RATE -> {
            Log.i(TAG, "HR uses Samsung SDK only (no platform fallback)")
            false
        }
        SensorType.SKIN_TEMPERATURE -> skinTempReader.start { c ->
            emit(TrackerState.Measuring(com.rpm.watch.sensor.VitalReading(temperatureC = c)))
        }
        SensorType.SPO2 -> spO2Reader.start { pct ->
            emit(TrackerState.Measuring(com.rpm.watch.sensor.VitalReading(spO2Percent = pct)))
        }
        SensorType.EDA, SensorType.BIA, SensorType.ECG -> false
    }.also { started ->
        if (!started) Log.w(TAG, "No platform sensor for ${sensor.name}")
    }

    fun stop() {
        skinTempReader.stop()
        spO2Reader.stop()
    }
}
