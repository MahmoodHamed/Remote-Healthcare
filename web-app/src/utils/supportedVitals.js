/**
 * Vitals the Galaxy Watch app can share (Samsung Health Sensor SDK + platform sensors).
 * @see https://developer.samsung.com/health/sensor/guide/data-specifications.html
 */

export const SDK_SENSOR_INFO = {
  continuous: [
    { name: 'Heart rate + HRV', tracker: 'HEART_RATE_CONTINUOUS', note: 'Galaxy Watch4+' },
    { name: 'Skin & ambient temperature', tracker: 'SKIN_TEMPERATURE_CONTINUOUS', note: 'Galaxy Watch5+' },
    { name: 'Stress (EDA)', tracker: 'EDA_CONTINUOUS', note: 'Galaxy Watch8+' },
  ],
  onDemand: [
    { name: 'SpO₂', tracker: 'SPO2_ON_DEMAND', note: 'Auto every ~3 min' },
    { name: 'Body fat (BIA)', tracker: 'BIA_ON_DEMAND', note: 'Manual on watch' },
    { name: 'ECG avg heart rate', tracker: 'ECG_ON_DEMAND', note: 'Manual on watch' },
  ],
  platform: [
    { name: 'Steps & calories', note: 'Wear OS step counter' },
    { name: 'Fall detection', note: 'Accelerometer' },
    { name: 'Watch on-wrist', note: 'Samsung off-body + live vitals' },
  ],
}

/** Metrics shown in the live grid — only what the watch can provide. */
export const SUPPORTED_METRIC_DEFS = [
  { key: 'heartRateBpm', label: 'Heart rate', unit: 'bpm', tone: 'accent', digits: 0, source: 'sdk' },
  { key: 'hrvMs', label: 'HRV', unit: 'ms', tone: 'violet', digits: 0, source: 'sdk' },
  { key: 'spO2Percent', label: 'SpO₂', unit: '%', tone: 'teal', digits: 1, source: 'sdk', onDemand: true },
  { key: 'skinTemperatureC', label: 'Skin temp.', unit: '°C', tone: 'blue', digits: 1, source: 'sdk' },
  { key: 'ambientTemperatureC', label: 'Ambient temp.', unit: '°C', tone: 'ink', digits: 1, source: 'sdk' },
  { key: 'stressScore', label: 'Stress', unit: '/100', tone: 'violet', digits: 0, source: 'sdk' },
  { key: 'stepsCount', label: 'Steps', unit: 'today', tone: 'ink', digits: 0, source: 'platform' },
  { key: 'caloriesBurned', label: 'Calories', unit: 'kcal', tone: 'ink', digits: 0, source: 'platform' },
  { key: 'fallDetected', label: 'Fall detection', unit: '', tone: 'danger', boolean: true, source: 'platform' },
  { key: 'isWearing', label: 'Watch status', unit: '', tone: 'teal', wearing: true, source: 'platform' },
  { key: 'bodyFatPercent', label: 'Body fat', unit: '%', tone: 'ink', digits: 1, source: 'sdk', onDemand: true },
  { key: 'ecgAvgHeartRateBpm', label: 'ECG avg HR', unit: 'bpm', tone: 'ink', digits: 0, source: 'sdk', onDemand: true },
]

export const HISTORY_COLUMNS = [
  { key: 'heartRateBpm', label: 'HR', digits: 0 },
  { key: 'spO2Percent', label: 'SpO₂', digits: 1 },
  { key: 'skinTemperatureC', label: 'Skin °C', digits: 1 },
  { key: 'ambientTemperatureC', label: 'Amb. °C', digits: 1 },
  { key: 'hrvMs', label: 'HRV', digits: 0 },
  { key: 'stressScore', label: 'Stress', digits: 0 },
  { key: 'stepsCount', label: 'Steps', digits: 0 },
]
