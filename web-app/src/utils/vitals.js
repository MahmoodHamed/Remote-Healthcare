export const mapVitalsPayload = (payload) => {
  if (!payload || typeof payload !== 'object') return null
  return {
    heartRateBpm: payload.heartRateBpm ?? null,
    spO2Percent: payload.spO2Percent ?? null,
    systolicBp: payload.systolicBp ?? null,
    diastolicBp: payload.diastolicBp ?? null,
    temperatureC: payload.temperatureC ?? null,
    stepsCount: payload.stepsCount ?? null,
    caloriesBurned: payload.caloriesBurned ?? null,
    fallDetected: Boolean(payload.fallDetected),
    isWearing: payload.isWearing !== false,
    recordedAt: payload.recordedAt ?? new Date().toISOString(),
  }
}

import { getApiBase } from './apiBase'

export const fetchLatestVitals = async (patientId, token) => {
  const url = new URL(`/api/patients/${patientId}/vitals/latest`, getApiBase()).toString()
  const response = await fetch(url, {
    headers: { Authorization: `Bearer ${token}` },
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
