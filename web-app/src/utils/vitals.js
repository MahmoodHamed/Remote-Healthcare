import { getApiBase } from './apiBase'
import { getAccessToken } from './authSession'

export const mapVitalsPayload = (payload) => {
  if (!payload || typeof payload !== 'object') return null
  return {
    heartRateBpm: payload.heartRateBpm ?? null,
    spO2Percent: payload.spO2Percent ?? null,
    systolicBp: payload.systolicBp ?? null,
    diastolicBp: payload.diastolicBp ?? null,
    temperatureC: payload.temperatureC ?? null,
    skinTemperatureC: payload.skinTemperatureC ?? null,
    heartRateVariabilityMs: payload.heartRateVariabilityMs ?? null,
    restingHeartRateBpm: payload.restingHeartRateBpm ?? null,
    maxHeartRateBpm: payload.maxHeartRateBpm ?? null,
    respirationRateBpm: payload.respirationRateBpm ?? null,
    stepsCount: payload.stepsCount ?? null,
    caloriesBurned: payload.caloriesBurned ?? null,
    distanceMeters: payload.distanceMeters ?? null,
    floorsClimbed: payload.floorsClimbed ?? null,
    activeMinutes: payload.activeMinutes ?? null,
    stressScore: payload.stressScore ?? null,
    sleepScore: payload.sleepScore ?? null,
    sleepDurationMinutes: payload.sleepDurationMinutes ?? null,
    bodyFatPercent: payload.bodyFatPercent ?? null,
    muscleMassKg: payload.muscleMassKg ?? null,
    bodyWaterPercent: payload.bodyWaterPercent ?? null,
    basalMetabolicRate: payload.basalMetabolicRate ?? null,
    ecgAverageHeartRate: payload.ecgAverageHeartRate ?? null,
    ecgClassification: payload.ecgClassification ?? null,
    ecgWaveformJson: payload.ecgWaveformJson ?? null,
    bloodGlucoseMgDl: payload.bloodGlucoseMgDl ?? null,
    batteryLevel: payload.batteryLevel ?? null,
    fallDetected: Boolean(payload.fallDetected),
    isWearing: payload.isWearing !== false,
    recordedAt: payload.recordedAt ?? new Date().toISOString(),
  }
}

export const fetchLatestVitals = async (patientId) => {
  const url = new URL(`/api/patients/${patientId}/vitals/latest`, getApiBase()).toString()
  const response = await fetch(url, {
    headers: { Authorization: `Bearer ${getAccessToken()}` },
  })
  if (response.status === 401) {
    const err = new Error('Unauthorized')
    err.status = 401
    throw err
  }
  if (response.status === 404) return null
  if (!response.ok) throw new Error(`Failed to load vitals (${response.status}).`)
  const data = await response.json()
  return mapVitalsPayload(data)
}
