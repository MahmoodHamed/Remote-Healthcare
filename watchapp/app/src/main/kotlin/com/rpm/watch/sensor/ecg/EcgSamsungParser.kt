package com.rpm.watch.sensor.ecg

import android.util.Log
import com.rpm.watch.sensor.HeartRateStatus
import com.rpm.watch.sensor.VitalReading
import com.rpm.watch.sensor.samsung.readKeyValue
import com.samsung.android.service.health.tracking.data.DataPoint

private const val TAG = "EcgSamsung"

/** Samsung ECG on-demand: progress 100 = session finished. */
private const val PROGRESS_COMPLETE = 100

/**
 * ECG on-demand session from [HealthTrackerType.ECG_ON_DEMAND].
 * Average HR for the dashboard is taken from the latest continuous HR when this completes.
 */
object EcgSamsungParser {

    fun parse(dp: DataPoint): VitalReading? {
        val progress = readKeyValue(dp, "EcgSet", listOf("PROGRESS"), emptyList())?.toInt()
        if (progress != PROGRESS_COMPLETE) {
            Log.d(TAG, "ECG in progress (progress=$progress)")
            return null
        }
        Log.i(TAG, "ECG measurement complete")
        return VitalReading(ecgComplete = true, status = HeartRateStatus.SUCCESS)
    }

    fun isMeasuring(dp: DataPoint): Boolean {
        val progress = readKeyValue(dp, "EcgSet", listOf("PROGRESS"), emptyList())?.toInt()
            ?: return false
        return progress in 1 until PROGRESS_COMPLETE
    }
}
