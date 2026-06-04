package com.rpm.watch.sensor

/**
 * Samsung [ValueKey.HeartRateSet.HEART_RATE_STATUS] codes.
 */
enum class HeartRateStatus {
    INITIAL,
    SUCCESS,
    MOVEMENT,
    DETACHED,
    WEAK_SIGNAL,
    SENSOR_BUSY,
    UNKNOWN;

    val isSuccessful: Boolean get() = this == SUCCESS
    val isOnWrist: Boolean get() = this != DETACHED

    companion object {
        fun isSamsungSuccess(code: Int): Boolean = code == 1

        fun fromSamsung(code: Int): HeartRateStatus = when (code) {
            1 -> SUCCESS
            0 -> INITIAL
            -2 -> MOVEMENT
            -3 -> DETACHED
            -8, -10 -> WEAK_SIGNAL
            -999 -> SENSOR_BUSY
            else -> UNKNOWN
        }
    }
}

data class VitalReading(
    val heartRateBpm: Int? = null,
    val spO2Percent: Float? = null,
    /** Wrist / object temperature (body temp on dashboard). */
    val temperatureC: Float? = null,
    val skinTemperatureC: Float? = null,
    val ambientTemperatureC: Float? = null,
    /** RMSSD from IBI list (ms). */
    val hrvMs: Float? = null,
    val status: HeartRateStatus = HeartRateStatus.INITIAL,
    val timestampMs: Long = System.currentTimeMillis(),
)

sealed class TrackerState {
    data object Connecting : TrackerState()
    data class Measuring(val reading: VitalReading) : TrackerState()
    data class Error(val message: String, val code: Int = -1) : TrackerState()
    data object Disconnected : TrackerState()
}
