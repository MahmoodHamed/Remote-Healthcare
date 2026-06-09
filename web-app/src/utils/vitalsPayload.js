const VITAL_FIELDS = [
  'heartRateBpm',
  'spO2Percent',
  'systolicBp',
  'diastolicBp',
  'skinTemperatureC',
  'ambientTemperatureC',
  'heartRateVariabilityMs',
  'hrvMs',
  'restingHeartRateBpm',
  'maxHeartRateBpm',
  'respirationRateBpm',
  'stepsCount',
  'caloriesBurned',
  'distanceMeters',
  'floorsClimbed',
  'activeMinutes',
  'stressScore',
  'sleepScore',
  'sleepDurationMinutes',
  'bodyFatPercent',
  'muscleMassKg',
  'bodyWaterPercent',
  'basalMetabolicRate',
  'ecgAverageHeartRate',
  'ecgAvgHeartRateBpm',
  'ecgClassification',
  'ecgWaveformJson',
  'bloodGlucoseMgDl',
  'batteryLevel',
]

const hasValue = (value) => value !== null && value !== undefined && value !== ''

const toCamelCase = (key) => {
  if (!key || key.length === 0) return key
  return key.charAt(0).toLowerCase() + key.slice(1)
}

/** Separate Samsung wrist (OBJECT) vs room (AMBIENT) temperatures — never duplicate legacy temperatureC. */
export function normalizeTemperatureFields(source) {
  if (!source || typeof source !== 'object') return source

  const skin = source.skinTemperatureC ?? null
  const ambient = source.ambientTemperatureC ?? null
  const legacy = source.temperatureC ?? null

  let resolvedSkin = skin
  if (resolvedSkin == null && legacy != null) {
    // Legacy temperatureC was an alias for wrist skin — not ambient room air.
    if (ambient == null || Math.abs(legacy - ambient) > 0.05) {
      resolvedSkin = legacy
    }
  }

  return {
    ...source,
    skinTemperatureC: resolvedSkin,
    ambientTemperatureC: ambient,
    temperatureC: null,
  }
}

/** Normalize REST/SignalR/MQTT field names (PascalCase + watch aliases). */
export const normalizeVitalsPayload = (payload) => {
  if (!payload || typeof payload !== 'object') return null

  const normalized = {}
  for (const [key, value] of Object.entries(payload)) {
    const camel = toCamelCase(key)
    if (normalized[camel] === undefined || normalized[camel] === null) {
      normalized[camel] = value
    }
  }

  if (normalized.hrvMs != null && normalized.heartRateVariabilityMs == null) {
    normalized.heartRateVariabilityMs = normalized.hrvMs
  }
  if (normalized.ecgAvgHeartRateBpm != null && normalized.ecgAverageHeartRate == null) {
    normalized.ecgAverageHeartRate = normalized.ecgAvgHeartRateBpm
  }
  if (normalized.spo2Percent != null && normalized.spO2Percent == null) {
    normalized.spO2Percent = normalized.spo2Percent
  }
  if (normalized.bodyFatPct != null && normalized.bodyFatPercent == null) {
    normalized.bodyFatPercent = normalized.bodyFatPct
  }

  return normalizeTemperatureFields(normalized)
}

/** Hide sensor readings when the watch is off-wrist. */
export const applyWearState = (vitals) => {
  if (!vitals || vitals.isWearing !== false) return vitals
  const cleared = { ...vitals }
  for (const key of VITAL_FIELDS) cleared[key] = null
  cleared.fallDetected = false
  cleared.ecgClassification = null
  return cleared
}

export const mapVitalsPayload = (payload) => {
  const source = normalizeVitalsPayload(payload)
  if (!source) return null
  const mapped = {
    heartRateBpm: source.heartRateBpm ?? null,
    spO2Percent: source.spO2Percent ?? null,
    systolicBp: source.systolicBp ?? null,
    diastolicBp: source.diastolicBp ?? null,
    skinTemperatureC: source.skinTemperatureC ?? null,
    ambientTemperatureC: source.ambientTemperatureC ?? null,
    heartRateVariabilityMs: source.heartRateVariabilityMs ?? source.hrvMs ?? null,
    hrvMs: source.hrvMs ?? source.heartRateVariabilityMs ?? null,
    restingHeartRateBpm: source.restingHeartRateBpm ?? null,
    maxHeartRateBpm: source.maxHeartRateBpm ?? null,
    respirationRateBpm: source.respirationRateBpm ?? null,
    stepsCount: source.stepsCount ?? null,
    caloriesBurned: source.caloriesBurned ?? null,
    distanceMeters: source.distanceMeters ?? null,
    floorsClimbed: source.floorsClimbed ?? null,
    activeMinutes: source.activeMinutes ?? null,
    stressScore: source.stressScore ?? null,
    sleepScore: source.sleepScore ?? null,
    sleepDurationMinutes: source.sleepDurationMinutes ?? null,
    bodyFatPercent: source.bodyFatPercent ?? null,
    muscleMassKg: source.muscleMassKg ?? null,
    bodyWaterPercent: source.bodyWaterPercent ?? null,
    basalMetabolicRate: source.basalMetabolicRate ?? null,
    ecgAverageHeartRate: source.ecgAverageHeartRate ?? source.ecgAvgHeartRateBpm ?? null,
    ecgAvgHeartRateBpm: source.ecgAvgHeartRateBpm ?? source.ecgAverageHeartRate ?? null,
    ecgClassification: source.ecgClassification ?? null,
    ecgWaveformJson: source.ecgWaveformJson ?? null,
    bloodGlucoseMgDl: source.bloodGlucoseMgDl ?? null,
    batteryLevel: source.batteryLevel ?? null,
    fallDetected: source.fallDetected == null ? null : Boolean(source.fallDetected),
    isWearing: source.isWearing == null ? null : source.isWearing !== false,
    recordedAt: source.recordedAt ?? new Date().toISOString(),
  }
  return applyWearState(mapped)
}

/** Keep last-known values when a partial live update omits fields. */
export const mergeVitalsPayload = (previous, incoming) => {
  const next = mapVitalsPayload(incoming)
  if (!next) return previous ?? null

  if (next.isWearing === false) return applyWearState(next)

  if (!previous) return next

  const merged = { ...next }
  for (const key of VITAL_FIELDS) {
    if (!hasValue(merged[key]) && hasValue(previous[key])) merged[key] = previous[key]
  }

  merged.fallDetected = next.fallDetected ?? previous.fallDetected ?? false
  merged.isWearing = next.isWearing ?? previous.isWearing ?? true
  merged.recordedAt = next.recordedAt || previous.recordedAt || new Date().toISOString()
  return applyWearState(merged)
}
