package com.rpm.watch.sensor.platform

import android.hardware.Sensor

internal fun resolveSensorType(vararg names: String): Int? {
    for (name in names) {
        try {
            return Sensor::class.java.getField(name).getInt(null)
        } catch (_: Exception) {
        }
    }
    return null
}
