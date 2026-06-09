package com.rpm.watch.sensor

/** Active vitals sensor on Galaxy Watch 8 (Samsung Health Sensor SDK). */
enum class SensorType {
    HEART_RATE,
    SKIN_TEMPERATURE,
    SPO2,
    /** Electrodermal activity → stress index (continuous). */
    EDA,
    /** Body composition / body fat (on-demand, user action). */
    BIA,
    /** ECG session (on-demand). */
    ECG;

    val displayLabel: String
        get() = when (this) {
            HEART_RATE -> "Heart Rate"
            SKIN_TEMPERATURE -> "Skin Temperature"
            SPO2 -> "SpO₂"
            EDA -> "Stress (EDA)"
            BIA -> "Body composition"
            ECG -> "ECG"
        }
}
