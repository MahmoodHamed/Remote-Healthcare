package com.rpm.watch.sensor.spo2

import android.util.Log
import com.rpm.watch.sensor.HeartRateStatus
import com.rpm.watch.sensor.VitalReading
import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.ValueKey.SpO2Set

private const val TAG = "SpO2Samsung"

/** Samsung SpO₂ status: 2 = measurement complete (~30 s on-demand). */
private const val STATUS_MEASUREMENT_COMPLETED = 2

/**
 * Parses Galaxy Watch 8 SpO₂ from [HealthTrackerType.SPO2_ON_DEMAND].
 * Requires watch on wrist, screen on, user still ~30 seconds.
 */
object SpO2SamsungParser {

    fun parse(dp: DataPoint): VitalReading? {
        val status = runCatching { dp.getValue(SpO2Set.STATUS) }.getOrNull()
        if (status == null) {
            Log.d(TAG, "SpO2 DataPoint has no STATUS")
            return null
        }
        if (status != STATUS_MEASUREMENT_COMPLETED) {
            Log.d(TAG, "SpO2 in progress (status=$status, need $STATUS_MEASUREMENT_COMPLETED)")
            return null
        }

        val spo2 = runCatching { dp.getValue(SpO2Set.SPO2) }.getOrNull() ?: return null
        if (spo2 !in 1..100) {
            Log.d(TAG, "SpO2 out of range: $spo2")
            return null
        }

        Log.i(TAG, "SpO2 complete: $spo2%")
        return VitalReading(spO2Percent = spo2.toFloat(), status = HeartRateStatus.SUCCESS)
    }

    /** True while Samsung is still calculating (status 0). */
    fun isMeasuring(dp: DataPoint): Boolean =
        runCatching { dp.getValue(SpO2Set.STATUS) }.getOrNull() == 0
}
