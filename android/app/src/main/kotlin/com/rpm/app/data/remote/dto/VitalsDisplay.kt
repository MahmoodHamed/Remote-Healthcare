package com.rpm.app.data.remote.dto

/** Hide sensor values when the watch is off-wrist (matches web dashboard behaviour). */
fun VitalRecordDto.forDisplay(): VitalRecordDto {
    if (isWearing) return this
    return copy(
        heartRateBpm = null,
        heartRateVariabilityMs = null,
        restingHeartRateBpm = null,
        maxHeartRateBpm = null,
        spO2Percent = null,
        respirationRateBpm = null,
        systolicBp = null,
        diastolicBp = null,
        temperatureC = null,
        skinTemperatureC = null,
        stepsCount = null,
        caloriesBurned = null,
        distanceMeters = null,
        floorsClimbed = null,
        activeMinutes = null,
        stressScore = null,
        sleepScore = null,
        sleepDurationMinutes = null,
        bodyFatPercent = null,
        muscleMassKg = null,
        bodyWaterPercent = null,
        basalMetabolicRate = null,
        ecgAverageHeartRate = null,
        ecgClassification = null,
        ecgWaveformJson = null,
        bloodGlucoseMgDl = null,
        fallDetected = false,
    )
}

fun VitalRecordLatestDto.forDisplay(): VitalRecordLatestDto {
    return this
}
