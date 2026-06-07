package com.rpm.app.ui.feature.patients

import com.rpm.app.data.remote.dto.VitalRecordDto
import com.rpm.app.data.remote.dto.VitalRecordLatestDto
import com.rpm.app.data.signalr.RealTimeVitals

/**
 * Vitals the Galaxy Watch can share (Samsung Health Sensor SDK + platform sensors).
 * @see https://developer.samsung.com/health/sensor/guide/data-specifications.html
 */
object SupportedVitals {

    data class MetricDef(
        val label: String,
        val unit: String = "",
        val hint: String? = null,
        val getValue: (RealTimeVitals?) -> String,
    )

    val liveMetrics: List<MetricDef> = listOf(
        MetricDef("Heart Rate", "bpm") { v -> v?.heartRateBpm?.toInt()?.toString() ?: "--" },
        MetricDef("HRV", "ms") { v -> v?.hrvMs?.toInt()?.toString() ?: "--" },
        MetricDef("SpO₂", "%", hint = "On-demand") { v -> v?.spO2Percent?.let { "%.1f".format(it) } ?: "--" },
        MetricDef("Skin Temp.", "°C") { v -> v?.skinTemperatureC?.let { "%.1f".format(it) } ?: "--" },
        MetricDef("Ambient Temp.", "°C") { v -> v?.ambientTemperatureC?.let { "%.1f".format(it) } ?: "--" },
        MetricDef("Stress", "/100") { v -> v?.stressScore?.toInt()?.toString() ?: "--" },
        MetricDef("Steps", "today") { v -> v?.stepsCount?.toString() ?: "--" },
        MetricDef("Calories", "kcal") { v -> v?.caloriesBurned?.let { "%.0f".format(it) } ?: "--" },
        MetricDef("Fall Detection", "") { v ->
            when (v?.fallDetected) { true -> "Alert!"; false -> "Safe"; null -> "--" }
        },
        MetricDef("Watch Status", "") { v ->
            when {
                v == null -> "--"
                isWearing(v) -> "On Wrist"
                else -> "Off Wrist"
            }
        },
        MetricDef("Body Fat", "%", hint = "On-demand") { v -> v?.bodyFatPercent?.let { "%.1f".format(it) } ?: "--" },
        MetricDef("ECG Avg HR", "bpm", hint = "On-demand") { v -> v?.ecgAvgHeartRateBpm?.toInt()?.toString() ?: "--" },
    )

    fun isWearing(v: RealTimeVitals): Boolean =
        v.isWearing || v.heartRateBpm != null || v.spO2Percent != null
            || v.skinTemperatureC != null || v.stressScore != null || v.hrvMs != null

    fun isWearing(record: VitalRecordDto): Boolean =
        record.isWearing || record.heartRateBpm != null || record.spO2Percent != null
            || record.skinTemperatureC != null || record.stressScore != null || record.hrvMs != null

    fun buildSummary(v: VitalRecordLatestDto): String = buildString {
        v.heartRateBpm?.let { append("HR: ${it.toInt()} bpm") }
        v.spO2Percent?.let {
            if (isNotEmpty()) append("  •  ")
            append("SpO₂: ${it.toInt()}%")
        }
        v.skinTemperatureC?.let {
            if (isNotEmpty()) append("  •  ")
            append("Skin: %.1f °C".format(it))
        } ?: v.temperatureC?.let {
            if (isNotEmpty()) append("  •  ")
            append("%.1f °C".format(it))
        }
        v.stressScore?.let {
            if (isNotEmpty()) append("  •  ")
            append("Stress: ${it.toInt()}")
        }
        if (isEmpty()) append("No vitals recorded yet")
    }

    /** Rows for history / detail — only non-null supported fields. */
    fun historyRows(record: VitalRecordDto): List<Pair<String, String>> = buildList {
        record.heartRateBpm?.let { add("Heart Rate" to "${it.toInt()} bpm") }
        record.spO2Percent?.let { add("SpO₂" to "${it.toInt()}%") }
        record.skinTemperatureC?.let { add("Skin Temp." to "%.1f °C".format(it)) }
        record.ambientTemperatureC?.let { add("Ambient Temp." to "%.1f °C".format(it)) }
        record.hrvMs?.let { add("HRV" to "${it.toInt()} ms") }
        record.stressScore?.let { add("Stress" to "${it.toInt()} / 100") }
        record.stepsCount?.let { add("Steps" to "$it") }
        record.caloriesBurned?.let { add("Calories" to "%.0f kcal".format(it)) }
        record.bodyFatPercent?.let { add("Body Fat" to "%.1f%%".format(it)) }
        record.ecgAvgHeartRateBpm?.let { add("ECG Avg HR" to "${it.toInt()} bpm") }
        add("Watch" to if (isWearing(record)) "On wrist" else "Off wrist")
    }
}
