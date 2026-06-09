/** Pick a field supporting camelCase and PascalCase from SignalR / REST payloads. */
export function pick(payload, camel, pascal) {
  if (payload[camel] !== undefined && payload[camel] !== null) return payload[camel]
  if (payload[pascal] !== undefined && payload[pascal] !== null) return payload[pascal]
  return null
}

export function pickBool(payload, camel, pascal) {
  const value = pick(payload, camel, pascal)
  if (value === true || value === 'true' || value === 1) return true
  if (value === false || value === 'false' || value === 0) return false
  return null
}

/** True when the payload contains signs the watch is on-wrist and measuring. */
export function hasLiveVitalsEvidence(vitals) {
  if (!vitals) return false
  const hr = vitals.heartRateBpm
  if (hr != null && !Number.isNaN(hr) && hr >= 30) return true
  if (vitals.spO2Percent != null) return true
  if (vitals.temperatureC != null || vitals.skinTemperatureC != null) return true
  if (vitals.stressScore != null) return true
  if (vitals.hrvMs != null) return true
  if (vitals.bodyFatPercent != null) return true
  if (vitals.ecgAvgHeartRateBpm != null) return true
  if (vitals.ambientTemperatureC != null) return true
  if (vitals.stepsCount != null && vitals.stepsCount > 0) return true
  if (vitals.caloriesBurned != null && vitals.caloriesBurned > 0) return true
  return false
}

/**
 * Samsung off-body sensor often reports false while vitals are streaming.
 * Prefer live sensor evidence over the explicit isWearing flag.
 */
export function inferWearing(vitals) {
  if (!vitals) return true
  if (hasLiveVitalsEvidence(vitals)) return true
  if (vitals.isWearing === true) return true
  if (vitals.isWearing === false) return false
  return true
}

/** Split Samsung wrist (OBJECT) vs room (AMBIENT) — legacy temperatureC duplicated skin. */
export function normalizeTemperatureFields(vitals) {
  if (!vitals) return vitals
  const ambient = vitals.ambientTemperatureC ?? null
  let skin = vitals.skinTemperatureC ?? null
  const legacy = vitals.temperatureC ?? null
  if (skin == null && legacy != null) {
    if (ambient == null || Math.abs(legacy - ambient) > 0.05) skin = legacy
  }
  return {
    ...vitals,
    skinTemperatureC: skin,
    ambientTemperatureC: ambient,
    temperatureC: null,
  }
}

export function normalizePayload(payload) {
  if (!payload || typeof payload !== 'object') return null
  const vitals = normalizeTemperatureFields({
    heartRateBpm: pick(payload, 'heartRateBpm', 'HeartRateBpm'),
    spO2Percent: pick(payload, 'spO2Percent', 'SpO2Percent'),
    systolicBp: pick(payload, 'systolicBp', 'SystolicBp'),
    diastolicBp: pick(payload, 'diastolicBp', 'DiastolicBp'),
    temperatureC: pick(payload, 'temperatureC', 'TemperatureC'),
    skinTemperatureC: pick(payload, 'skinTemperatureC', 'SkinTemperatureC'),
    ambientTemperatureC: pick(payload, 'ambientTemperatureC', 'AmbientTemperatureC'),
    hrvMs: pick(payload, 'hrvMs', 'HrvMs'),
    stressScore: pick(payload, 'stressScore', 'StressScore'),
    bodyFatPercent: pick(payload, 'bodyFatPercent', 'BodyFatPercent'),
    ecgAvgHeartRateBpm: pick(payload, 'ecgAvgHeartRateBpm', 'EcgAvgHeartRateBpm'),
    stepsCount: pick(payload, 'stepsCount', 'StepsCount'),
    caloriesBurned: pick(payload, 'caloriesBurned', 'CaloriesBurned'),
    fallDetected: Boolean(pickBool(payload, 'fallDetected', 'FallDetected')),
    isWearing: pickBool(payload, 'isWearing', 'IsWearing'),
    recordedAt: pick(payload, 'recordedAt', 'RecordedAt') ?? new Date().toISOString(),
  })
  vitals.isWearing = inferWearing(vitals)
  return vitals
}

const MERGE_KEYS = [
  'heartRateBpm', 'spO2Percent', 'systolicBp', 'diastolicBp',
  'skinTemperatureC', 'ambientTemperatureC', 'hrvMs', 'stressScore', 'bodyFatPercent',
  'ecgAvgHeartRateBpm', 'stepsCount', 'caloriesBurned',
]

export function mergeVitals(previous, incoming) {
  if (!previous) return incoming
  const merged = { ...incoming }
  for (const key of MERGE_KEYS) {
    if (merged[key] == null && previous[key] != null) merged[key] = previous[key]
  }
  merged.fallDetected = incoming.fallDetected || previous.fallDetected
  merged.isWearing = inferWearing(merged)
  return merged
}
