package com.rpm.app.data.signalr

import android.util.Log
import com.rpm.app.data.remote.dto.VitalRecordDto
import kotlinx.serialization.json.Json

private const val TAG = "VitalsPayloadParser"

object VitalsPayloadParser {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    fun parse(raw: Any?): RealTimeVitals? = runCatching {
        when (raw) {
            null -> null
            is RealTimeVitals -> raw
            is VitalRecordDto -> raw.toRealTime()
            is String -> json.decodeFromString<VitalRecordDto>(raw).toRealTime()
            is Map<*, *> -> parseMap(raw)
            else -> {
                // Gson LinkedTreeMap from SignalR Object callback
                val asString = raw.toString()
                if (asString.startsWith("{")) {
                    json.decodeFromString<VitalRecordDto>(asString).toRealTime()
                } else {
                    Log.w(TAG, "Unsupported vitals payload type: ${raw.javaClass.name}")
                    null
                }
            }
        }
    }.getOrElse {
        Log.e(TAG, "Failed to parse vitals payload: ${it.message}", it)
        null
    }

    private fun parseMap(map: Map<*, *>): RealTimeVitals {
        val vitals = RealTimeVitals(
            patientId            = pickString(map, "patientId", "PatientId") ?: "",
            heartRateBpm         = pickFloat(map, "heartRateBpm", "HeartRateBpm"),
            spO2Percent          = pickFloat(map, "spO2Percent", "SpO2Percent"),
            systolicBp           = pickFloat(map, "systolicBp", "SystolicBp"),
            diastolicBp          = pickFloat(map, "diastolicBp", "DiastolicBp"),
            temperatureC         = pickFloat(map, "temperatureC", "TemperatureC"),
            skinTemperatureC     = pickFloat(map, "skinTemperatureC", "SkinTemperatureC"),
            ambientTemperatureC  = pickFloat(map, "ambientTemperatureC", "AmbientTemperatureC"),
            hrvMs                = pickFloat(map, "hrvMs", "HrvMs"),
            stressScore          = pickFloat(map, "stressScore", "StressScore"),
            bodyFatPercent       = pickFloat(map, "bodyFatPercent", "BodyFatPercent"),
            ecgAvgHeartRateBpm   = pickFloat(map, "ecgAvgHeartRateBpm", "EcgAvgHeartRateBpm"),
            stepsCount           = pickInt(map, "stepsCount", "StepsCount"),
            caloriesBurned       = pickFloat(map, "caloriesBurned", "CaloriesBurned"),
            fallDetected         = pickBool(map, "fallDetected", "FallDetected") ?: false,
            isWearing            = pickBool(map, "isWearing", "IsWearing") ?: true,
            recordedAt           = pickString(map, "recordedAt", "RecordedAt") ?: "",
        )
        return vitals.withNormalizedTemperatures().withInferredWearing()
    }

    private fun pick(map: Map<*, *>, camel: String, pascal: String): Any? =
        map[camel] ?: map[pascal]

    private fun pickString(map: Map<*, *>, camel: String, pascal: String): String? =
        pick(map, camel, pascal)?.toString()?.takeIf { it.isNotBlank() }

    private fun pickFloat(map: Map<*, *>, camel: String, pascal: String): Float? =
        when (val v = pick(map, camel, pascal)) {
            null -> null
            is Number -> v.toFloat()
            is String -> v.toFloatOrNull()
            else -> null
        }

    private fun pickInt(map: Map<*, *>, camel: String, pascal: String): Int? =
        when (val v = pick(map, camel, pascal)) {
            null -> null
            is Number -> v.toInt()
            is String -> v.toIntOrNull()
            else -> null
        }

    private fun pickBool(map: Map<*, *>, camel: String, pascal: String): Boolean? =
        when (val v = pick(map, camel, pascal)) {
            null -> null
            is Boolean -> v
            is String -> v.equals("true", ignoreCase = true)
            is Number -> v.toInt() != 0
            else -> null
        }
}

fun VitalRecordDto.toRealTime(): RealTimeVitals = RealTimeVitals(
    patientId            = patientId,
    heartRateBpm         = heartRateBpm,
    spO2Percent          = spO2Percent,
    systolicBp           = systolicBp,
    diastolicBp          = diastolicBp,
    temperatureC         = temperatureC,
    skinTemperatureC     = skinTemperatureC,
    ambientTemperatureC  = ambientTemperatureC ?: temperatureC,
    hrvMs                = hrvMs,
    stressScore          = stressScore,
    bodyFatPercent       = bodyFatPercent,
    ecgAvgHeartRateBpm   = ecgAvgHeartRateBpm,
    stepsCount           = stepsCount,
    caloriesBurned       = caloriesBurned,
    fallDetected         = fallDetected,
    isWearing            = isWearing,
    recordedAt           = recordedAt,
).withNormalizedTemperatures().withInferredWearing()

/** Server stores ambient in temperatureC; watch sends skin + ambient explicitly. */
fun RealTimeVitals.withNormalizedTemperatures(): RealTimeVitals {
    var skin = skinTemperatureC
    var ambient = ambientTemperatureC
    val legacy = temperatureC

    when {
        ambient == null && legacy != null && skin != null ->
            ambient = legacy
        ambient == null && legacy != null && skin == null ->
            skin = legacy
        skin == null && legacy != null && ambient != null &&
            kotlin.math.abs(legacy - ambient) > 0.05f ->
            skin = legacy
    }

    return copy(
        skinTemperatureC = skin,
        ambientTemperatureC = ambient,
        temperatureC = null,
    )
}

/** Samsung off-body sensor often reports false while vitals are still streaming. */
fun RealTimeVitals.withInferredWearing(): RealTimeVitals {
    if (hasLiveVitalsEvidence()) return copy(isWearing = true)
    return this
}

private fun RealTimeVitals.hasLiveVitalsEvidence(): Boolean =
    (heartRateBpm != null && heartRateBpm >= 30f)
        || spO2Percent != null
        || temperatureC != null
        || skinTemperatureC != null
        || ambientTemperatureC != null
        || hrvMs != null
        || stressScore != null
        || bodyFatPercent != null
        || ecgAvgHeartRateBpm != null
        || (stepsCount != null && stepsCount > 0)
        || (caloriesBurned != null && caloriesBurned > 0f)

fun RealTimeVitals.mergeWith(previous: RealTimeVitals?): RealTimeVitals {
    if (previous == null) return withInferredWearing()
    return copy(
        heartRateBpm        = heartRateBpm        ?: previous.heartRateBpm,
        spO2Percent         = spO2Percent         ?: previous.spO2Percent,
        systolicBp          = systolicBp          ?: previous.systolicBp,
        diastolicBp         = diastolicBp         ?: previous.diastolicBp,
        temperatureC        = temperatureC        ?: previous.temperatureC,
        skinTemperatureC    = skinTemperatureC    ?: previous.skinTemperatureC,
        ambientTemperatureC = ambientTemperatureC ?: previous.ambientTemperatureC,
        hrvMs               = hrvMs               ?: previous.hrvMs,
        stressScore         = stressScore         ?: previous.stressScore,
        bodyFatPercent      = bodyFatPercent      ?: previous.bodyFatPercent,
        ecgAvgHeartRateBpm  = ecgAvgHeartRateBpm  ?: previous.ecgAvgHeartRateBpm,
        stepsCount          = stepsCount          ?: previous.stepsCount,
        caloriesBurned      = caloriesBurned      ?: previous.caloriesBurned,
        fallDetected        = fallDetected || previous.fallDetected,
        isWearing           = isWearing || previous.isWearing,
        recordedAt          = recordedAt.ifBlank { previous.recordedAt },
        patientId           = patientId.ifBlank { previous.patientId },
    ).withNormalizedTemperatures().withInferredWearing()
}
