package com.rpm.watch.sensor.samsung

import com.rpm.watch.sensor.SensorType
import com.samsung.android.service.health.tracking.data.HealthTrackerType

internal object SamsungTrackerResolver {

    fun resolve(sensor: SensorType): HealthTrackerType? {
        val preferred = when (sensor) {
            SensorType.HEART_RATE -> listOf(
                "HEART_RATE_CONTINUOUS",
                "HEART_RATE",
            )
            SensorType.SPO2 -> listOf("SPO2_ON_DEMAND")
            SensorType.SKIN_TEMPERATURE -> listOf(
                "SKIN_TEMPERATURE_CONTINUOUS",
            )
        }

        for (name in preferred) {
            val match = HealthTrackerType.entries.firstOrNull {
                it.name.equals(name, ignoreCase = true)
            }
            if (match != null) return match
        }

        val keywords = when (sensor) {
            SensorType.HEART_RATE -> listOf("HEART_RATE")
            SensorType.SPO2 -> listOf("SPO2")
            SensorType.SKIN_TEMPERATURE -> listOf("SKIN_TEMPERATURE", "CONTINUOUS")
        }
        return HealthTrackerType.entries.firstOrNull { trackerType ->
            keywords.all { keyword -> trackerType.name.contains(keyword, ignoreCase = true) } ||
                keywords.any { keyword -> trackerType.name.contains(keyword, ignoreCase = true) }
        }
    }

    /** Samsung: only one on-demand tracker at a time — SpO₂ uses scheduled flush. */
    fun isOnDemand(sensor: SensorType): Boolean = sensor == SensorType.SPO2

    fun isContinuous(sensor: SensorType): Boolean =
        sensor == SensorType.HEART_RATE || sensor == SensorType.SKIN_TEMPERATURE
}
