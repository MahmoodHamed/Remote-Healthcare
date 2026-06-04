package com.rpm.watch.sensor.samsung

import android.util.Log
import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.ValueKey
import java.lang.reflect.Modifier

private const val TAG = "SamsungValueKey"

internal fun readKeyValue(
    dp: DataPoint,
    nestedClassName: String,
    valueCandidates: List<String>,
    statusCandidates: List<String>,
): Number? {
    val nestedClass = runCatching {
        Class.forName("com.samsung.android.service.health.tracking.data.ValueKey\$$nestedClassName")
    }.getOrNull() ?: return null

    val valueKey = resolveValueKeyField(nestedClass, valueCandidates, excludeStatus = true)
    if (valueKey != null) {
        runCatching { return dp.getValue(valueKey) as? Number }.getOrNull()
    }

    val statusKey = resolveValueKeyField(nestedClass, statusCandidates, excludeStatus = false)
    return statusKey?.let { dp.getValue(it) as? Number }
}

private fun resolveValueKeyField(
    nestedClass: Class<*>,
    preferredNames: List<String>,
    excludeStatus: Boolean,
): ValueKey<*>? {
    val fields = nestedClass.fields.filter { field ->
        ValueKey::class.java.isAssignableFrom(field.type) && Modifier.isStatic(field.modifiers)
    }

    fun score(fieldName: String): Int {
        val upper = fieldName.uppercase()
        var score = 0
        if (preferredNames.any { upper == it.uppercase() }) score += 100
        if (preferredNames.any { upper.contains(it.uppercase()) }) score += 50
        if (excludeStatus && upper.contains("STATUS")) score -= 100
        if (upper.contains("VALUE")) score += 10
        return score
    }

    return fields.maxByOrNull { score(it.name) }?.let { field ->
        runCatching { field.get(null) as? ValueKey<*> }.getOrNull()
    }
}

internal fun logDataPointContents(dp: DataPoint) {
    try {
        val nestedNames = listOf("HeartRateSet", "SpO2Set", "SkinTemperatureSet", "PpgSet")
        for (n in nestedNames) {
            val cls = runCatching {
                Class.forName("com.samsung.android.service.health.tracking.data.ValueKey\$$n")
            }.getOrNull() ?: continue

            val fields = cls.fields.filter { field ->
                ValueKey::class.java.isAssignableFrom(field.type) && Modifier.isStatic(field.modifiers)
            }

            for (f in fields) {
                val vk = runCatching { f.get(null) as? ValueKey<*> }.getOrNull() ?: continue
                val v = runCatching { dp.getValue(vk) }.getOrNull()
                if (v != null) Log.d(TAG, "DataPoint ${n}.${f.name} = $v")
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, "Failed to dump DataPoint: ${e.message}")
    }
}
