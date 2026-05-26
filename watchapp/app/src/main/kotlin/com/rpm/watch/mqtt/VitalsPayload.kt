package com.rpm.watch.mqtt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * JSON payload published to `vitals/{patientId}/data`.
 *
 * Field names match the backend `MqttVitalsPayload` (.NET) and the web
 * `mapVitalsPayload` helper. All sensor fields are optional so the watch
 * can mix continuous trackers (HR, accelerometer, skin temperature) with
 * on-demand trackers (SpO2, ECG, BIA) that report only when triggered.
 */
@Serializable
data class VitalsPayload(
    @SerialName("patientId")               val patientId: String,
    @SerialName("deviceId")                val deviceId: String,

    // Cardio
    @SerialName("heartRateBpm")            val heartRateBpm: Float? = null,
    @SerialName("heartRateVariabilityMs")  val heartRateVariabilityMs: Float? = null,
    @SerialName("restingHeartRateBpm")     val restingHeartRateBpm: Float? = null,
    @SerialName("maxHeartRateBpm")         val maxHeartRateBpm: Float? = null,

    // Respiratory
    @SerialName("spO2Percent")             val spO2Percent: Float? = null,
    @SerialName("respirationRateBpm")      val respirationRateBpm: Float? = null,

    // Blood pressure
    @SerialName("systolicBp")              val systolicBp: Float? = null,
    @SerialName("diastolicBp")             val diastolicBp: Float? = null,

    // Temperature
    @SerialName("temperatureC")            val temperatureC: Float? = null,
    @SerialName("skinTemperatureC")        val skinTemperatureC: Float? = null,

    // Activity & energy
    @SerialName("stepsCount")              val stepsCount: Int? = null,
    @SerialName("caloriesBurned")          val caloriesBurned: Float? = null,
    @SerialName("distanceMeters")          val distanceMeters: Float? = null,
    @SerialName("floorsClimbed")           val floorsClimbed: Int? = null,
    @SerialName("activeMinutes")           val activeMinutes: Int? = null,

    // Sleep & stress
    @SerialName("stressScore")             val stressScore: Float? = null,
    @SerialName("sleepScore")              val sleepScore: Float? = null,
    @SerialName("sleepDurationMinutes")    val sleepDurationMinutes: Int? = null,

    // Body composition (BIA)
    @SerialName("bodyFatPercent")          val bodyFatPercent: Float? = null,
    @SerialName("muscleMassKg")            val muscleMassKg: Float? = null,
    @SerialName("bodyWaterPercent")        val bodyWaterPercent: Float? = null,
    @SerialName("basalMetabolicRate")      val basalMetabolicRate: Float? = null,

    // ECG
    @SerialName("ecgAverageHeartRate")     val ecgAverageHeartRate: Float? = null,
    @SerialName("ecgClassification")       val ecgClassification: String? = null,
    @SerialName("ecgWaveformJson")         val ecgWaveformJson: String? = null,

    // Glucose (when supported)
    @SerialName("bloodGlucoseMgDl")        val bloodGlucoseMgDl: Float? = null,

    // Safety & wear status
    @SerialName("fallDetected")            val fallDetected: Boolean = false,
    @SerialName("isWearing")               val isWearing: Boolean = true,
    @SerialName("batteryLevel")            val batteryLevel: Float? = null
)
