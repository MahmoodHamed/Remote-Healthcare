package com.rpm.watch.mqtt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VitalsPayload(
    @SerialName("patientId") val patientId: String,
    @SerialName("deviceId") val deviceId: String,
    @SerialName("heartRateBpm") val heartRateBpm: Float? = null,
    @SerialName("spO2Percent") val spO2Percent: Float? = null,
    @SerialName("systolicBp") val systolicBp: Float? = null,
    @SerialName("diastolicBp") val diastolicBp: Float? = null,
    @SerialName("temperatureC") val temperatureC: Float? = null,
    @SerialName("skinTemperatureC") val skinTemperatureC: Float? = null,
    @SerialName("ambientTemperatureC") val ambientTemperatureC: Float? = null,
    @SerialName("hrvMs") val hrvMs: Float? = null,
    @SerialName("stepsCount") val stepsCount: Int? = null,
    @SerialName("caloriesBurned") val caloriesBurned: Float? = null,
    @SerialName("fallDetected") val fallDetected: Boolean = false,
    @SerialName("isWearing") val isWearing: Boolean = true,
)
