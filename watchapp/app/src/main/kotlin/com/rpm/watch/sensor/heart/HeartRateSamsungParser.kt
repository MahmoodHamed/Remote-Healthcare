package com.rpm.watch.sensor.heart

import android.util.Log
import com.rpm.watch.sensor.HeartRateStatus
import com.rpm.watch.sensor.VitalReading
import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.ValueKey
import kotlin.math.sqrt

private const val TAG = "HeartRateSamsung"

object HeartRateSamsungParser {

    fun parse(dp: DataPoint): VitalReading {
        val statusCode = readInt(dp, ValueKey.HeartRateSet.HEART_RATE_STATUS) ?: 0
        val status = HeartRateStatus.fromSamsung(statusCode)
        val hr = readInt(dp, ValueKey.HeartRateSet.HEART_RATE)
        val hrvMs = readIbiList(dp)?.let { computeRmssdMs(it) }

        if (!status.isSuccessful || hr == null || hr !in 30..220) {
            Log.d(TAG, "HR skip (status=$statusCode/$status, hr=$hr)")
            return VitalReading(heartRateBpm = null, status = status, hrvMs = hrvMs)
        }

        Log.i(TAG, "HR success: $hr bpm hrv=$hrvMs")
        return VitalReading(
            heartRateBpm = hr,
            status = status,
            hrvMs = hrvMs,
            timestampMs = System.currentTimeMillis(),
        )
    }

    private fun readIbiList(dp: DataPoint): List<Int>? {
        return runCatching {
            @Suppress("UNCHECKED_CAST")
            val raw = dp.getValue(ValueKey.HeartRateSet.IBI_LIST) as? List<*> ?: return null
            raw.mapNotNull { (it as? Number)?.toInt() }
        }.getOrNull()?.filter { it in 300..2000 }
    }

    /** RMSSD in milliseconds from inter-beat intervals. */
    fun computeRmssdMs(ibiMs: List<Int>): Float? {
        if (ibiMs.size < 2) return null
        var sumSq = 0.0
        for (i in 1 until ibiMs.size) {
            val d = (ibiMs[i] - ibiMs[i - 1]).toDouble()
            sumSq += d * d
        }
        return sqrt(sumSq / (ibiMs.size - 1)).toFloat()
    }

    private fun readInt(dp: DataPoint, key: ValueKey<Int>): Int? =
        runCatching { dp.getValue(key) }.getOrNull()
}
