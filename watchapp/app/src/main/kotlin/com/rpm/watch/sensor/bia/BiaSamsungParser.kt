package com.rpm.watch.sensor.bia

import android.util.Log
import com.rpm.watch.sensor.HeartRateStatus
import com.rpm.watch.sensor.VitalReading
import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.ValueKey.BiaSet

private const val TAG = "BiaSamsung"

/** Samsung BIA: measurement finished (same pattern as SpO₂). */
private const val STATUS_MEASUREMENT_COMPLETED = 2

/**
 * Body composition from [HealthTrackerType.BIA_ON_DEMAND].
 * Requires user to touch crown + side keys per Samsung guide.
 */
object BiaSamsungParser {

    fun parse(dp: DataPoint): VitalReading? {
        val status = runCatching { dp.getValue(BiaSet.STATUS) }.getOrNull() ?: return null
        if (status != STATUS_MEASUREMENT_COMPLETED) {
            Log.d(TAG, "BIA in progress (status=$status)")
            return null
        }
        val ratio = runCatching { dp.getValue(BiaSet.BODY_FAT_RATIO) }.getOrNull() ?: return null
        if (ratio !in 1f..60f) {
            Log.d(TAG, "BIA body fat out of range: $ratio")
            return null
        }
        Log.i(TAG, "BIA complete: body fat ${ratio}%")
        return VitalReading(bodyFatPercent = ratio, status = HeartRateStatus.SUCCESS)
    }

    fun isMeasuring(dp: DataPoint): Boolean =
        runCatching { dp.getValue(BiaSet.STATUS) }.getOrNull() == 0
}
