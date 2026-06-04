package com.rpm.watch.sensor

/** Active vitals sensor on Galaxy Watch 8 (Samsung Health Sensor SDK). */
enum class SensorType {
    HEART_RATE,
    SKIN_TEMPERATURE,
    SPO2;

    val displayLabel: String
        get() = when (this) {
            HEART_RATE -> "Heart Rate"
            SKIN_TEMPERATURE -> "Skin Temperature"
            SPO2 -> "SpO₂"
        }
}
