package com.rpm.watch.ui

import com.rpm.watch.WatchUiState
import com.rpm.watch.sensor.HeartRateStatus
import java.util.Locale

/** Metrics the watch can measure and share (Samsung SDK + platform). */
enum class WatchViewMetric(
    val chipLabel: String,
    val fullLabel: String,
    val unit: String,
    val onDemand: Boolean = false,
    val sdkSource: String = "sdk",
) {
    HEART_RATE("HR", "Heart rate", "bpm"),
    HRV("HRV", "HRV", "ms"),
    SPO2("SpO₂", "SpO₂", "%", onDemand = true),
    SKIN_TEMP("Skin", "Skin temp.", "°C"),
    AMBIENT_TEMP("Amb.", "Ambient temp.", "°C"),
    STRESS("Stress", "Stress", "/100"),
    STEPS("Steps", "Steps", "today", sdkSource = "platform"),
    CALORIES("Cal", "Calories", "kcal", sdkSource = "platform"),
    FALL("Fall", "Fall detection", "", sdkSource = "platform"),
    WEAR("Watch", "Watch status", "", sdkSource = "platform"),
    BODY_FAT("Fat", "Body fat", "%", onDemand = true),
    ECG("ECG", "ECG avg HR", "bpm", onDemand = true),
}

object SupportedWatchVitals {

    val viewableMetrics: List<WatchViewMetric> = WatchViewMetric.entries

    val sdkContinuous = listOf(
        "Heart rate + HRV (Watch4+)",
        "Skin & ambient temp (Watch5+)",
        "Stress / EDA (Watch8+)",
    )

    val sdkOnDemand = listOf(
        "SpO₂ — auto ~3 min",
        "Body fat (BIA) — manual",
        "ECG — manual",
    )

    val platformSensors = listOf(
        "Steps & calories",
        "Fall detection",
        "On-wrist status",
    )

    fun formatValue(metric: WatchViewMetric, state: WatchUiState): String = when (metric) {
        WatchViewMetric.HEART_RATE ->
            if (state.heartRate > 0) state.heartRate.toString() else "--"
        WatchViewMetric.HRV ->
            state.hrvMs?.toInt()?.toString() ?: "--"
        WatchViewMetric.SPO2 ->
            state.spO2Percent?.let { String.format(Locale.US, "%.0f", it) } ?: "--"
        WatchViewMetric.SKIN_TEMP ->
            state.skinTemperatureC?.let { String.format(Locale.US, "%.1f", it) } ?: "--"
        WatchViewMetric.AMBIENT_TEMP ->
            state.ambientTemperatureC?.let { String.format(Locale.US, "%.1f", it) } ?: "--"
        WatchViewMetric.STRESS ->
            state.stressScore?.toInt()?.toString() ?: "--"
        WatchViewMetric.STEPS ->
            state.stepsCount?.toString() ?: "--"
        WatchViewMetric.CALORIES ->
            state.caloriesBurned?.let { String.format(Locale.US, "%.0f", it) } ?: "--"
        WatchViewMetric.FALL ->
            if (state.fallDetected) "Alert!" else "Safe"
        WatchViewMetric.WEAR ->
            when {
                !state.isMonitoring -> "--"
                state.isWearing -> "On"
                else -> "Off"
            }
        WatchViewMetric.BODY_FAT ->
            state.bodyFatPercent?.let { String.format(Locale.US, "%.1f", it) } ?: "--"
        WatchViewMetric.ECG ->
            state.ecgAvgHeartRateBpm?.toInt()?.toString() ?: "--"
    }

    fun statusText(metric: WatchViewMetric, state: WatchUiState): String = when (metric) {
        WatchViewMetric.HEART_RATE -> when {
            !state.isMonitoring -> "Tap Start"
            state.heartRate > 0 -> "Measuring ${state.heartRate} bpm"
            state.heartRateStatus == HeartRateStatus.DETACHED -> "Put watch on wrist"
            state.heartRateStatus == HeartRateStatus.MOVEMENT -> "Fasten watch snugly"
            state.heartRateStatus == HeartRateStatus.WEAK_SIGNAL -> "Hold still, screen on"
            else -> "Measuring…"
        }
        WatchViewMetric.HRV -> if (state.hrvMs != null) "From heart rate IBI" else "Waiting for HR…"
        WatchViewMetric.SPO2 -> if (state.spO2Percent != null) "On-demand · ~3 min" else "Measuring SpO₂…"
        WatchViewMetric.SKIN_TEMP -> if (state.skinTemperatureC != null) "Skin temperature" else "Measuring skin…"
        WatchViewMetric.AMBIENT_TEMP -> if (state.ambientTemperatureC != null) "Ambient temperature" else "Measuring ambient…"
        WatchViewMetric.STRESS -> if (state.stressScore != null) "EDA continuous (Watch8+)" else "Stress in background…"
        WatchViewMetric.STEPS -> "Shared with server"
        WatchViewMetric.CALORIES -> "Estimated from steps"
        WatchViewMetric.FALL -> if (state.fallDetected == true) "Fall detected!" else "Accelerometer"
        WatchViewMetric.WEAR -> if (state.isWearing) "On wrist" else "Off wrist"
        WatchViewMetric.BODY_FAT -> if (state.biaMeasuring) "Follow BIA prompt…" else "Tap Measure Body Fat"
        WatchViewMetric.ECG -> if (state.ecgMeasuring) "Follow ECG prompt…" else "Tap Measure ECG"
    }

    fun sharingSummary(state: WatchUiState): List<Pair<String, String>> =
        viewableMetrics.map { it.fullLabel to formatValue(it, state) }
}
