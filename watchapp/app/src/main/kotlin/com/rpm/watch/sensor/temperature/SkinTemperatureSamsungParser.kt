package com.rpm.watch.sensor.temperature

import android.util.Log
import com.rpm.watch.sensor.HeartRateStatus
import com.rpm.watch.sensor.VitalReading
import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.ValueKey.SkinTemperatureSet

private const val TAG = "SkinTempSamsung"
private const val STATUS_ERROR = -1

object SkinTemperatureSamsungParser {

    fun parse(dp: DataPoint): VitalReading? {
        val wristC = runCatching { dp.getValue(SkinTemperatureSet.OBJECT_TEMPERATURE) }.getOrNull()
        val ambientC = runCatching { dp.getValue(SkinTemperatureSet.AMBIENT_TEMPERATURE) }.getOrNull()
        val status = runCatching { dp.getValue(SkinTemperatureSet.STATUS) }.getOrNull()

        val skin = wristC?.takeIf { isPlausible(it) }
        val ambient = ambientC?.takeIf { isPlausible(it) }

        if (skin != null || ambient != null) {
            Log.i(TAG, "Skin temp skin=$skin ambient=$ambient status=$status")
            return VitalReading(
                skinTemperatureC = skin,
                ambientTemperatureC = ambient,
                status = HeartRateStatus.SUCCESS,
            )
        }

        if (status != STATUS_ERROR) {
            Log.d(TAG, "Skin temp not ready (wrist=$wristC ambient=$ambientC status=$status)")
        }
        return null
    }

    fun isPlausible(celsius: Float): Boolean = celsius in 15f..45f
}
