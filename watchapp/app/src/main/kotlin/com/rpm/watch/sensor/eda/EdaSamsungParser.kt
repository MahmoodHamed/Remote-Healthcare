package com.rpm.watch.sensor.eda

import android.util.Log
import com.rpm.watch.sensor.VitalReading
import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.ValueKey.EdaSet
import kotlin.math.roundToInt

private const val TAG = "EdaSamsung"

/** Samsung EDA status: 0 = normal. */
private const val STATUS_NORMAL = 0

/**
 * Maps skin conductance (µS) to a 0–100 stress index for the dashboard.
 * Higher conductance generally indicates higher arousal/stress.
 */
object EdaSamsungParser {

    fun parse(dp: DataPoint): VitalReading? {
        val status = runCatching { dp.getValue(EdaSet.STATUS) }.getOrNull() ?: return null
        if (status != STATUS_NORMAL) {
            Log.d(TAG, "EDA skip status=$status")
            return null
        }
        val conductance = runCatching { dp.getValue(EdaSet.SKIN_CONDUCTANCE) }.getOrNull()
            ?: return null
        if (conductance <= 0f) return null

        val stress = (conductance.coerceIn(0.2f, 20f) / 20f * 100f).roundToInt().toFloat()
        Log.d(TAG, "EDA conductance=$conductance µS → stress=$stress")
        return VitalReading(stressScore = stress)
    }
}
