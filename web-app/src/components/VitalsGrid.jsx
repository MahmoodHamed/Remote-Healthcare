import { useMemo } from 'react'

const HR_LIMITS = { min: 50, max: 120 }
const SPO2_LIMITS = { min: 92 }
const TEMP_LIMITS = { max: 38 }
const STRESS_LIMITS = { max: 75 }

const format = (value, digits = 0) => {
  if (value === null || value === undefined || Number.isNaN(value)) return '—'
  if (typeof value !== 'number') return value
  return digits ? value.toFixed(digits) : Math.round(value).toString()
}

export default function VitalsGrid({ vitals }) {
  const cards = useMemo(() => {
    const v = vitals || {}
    const items = [
      {
        key: 'hr',
        label: 'Heart rate',
        value: format(v.heartRateBpm),
        unit: 'bpm',
        tone: 'tone-hr',
        meta: v.maxHeartRateBpm ? `Max ${format(v.maxHeartRateBpm)} bpm` : null,
        danger: v.heartRateBpm > HR_LIMITS.max || (v.heartRateBpm !== null && v.heartRateBpm < HR_LIMITS.min),
        icon: '❤️',
      },
      {
        key: 'spo2',
        label: 'SpO₂',
        value: format(v.spO2Percent, 1),
        unit: '%',
        tone: 'tone-spo2',
        danger: v.spO2Percent !== null && v.spO2Percent < SPO2_LIMITS.min,
        icon: '🫁',
      },
      {
        key: 'bp',
        label: 'Blood pressure',
        value: v.systolicBp != null && v.diastolicBp != null
          ? `${format(v.systolicBp)}/${format(v.diastolicBp)}`
          : '—',
        unit: 'mmHg',
        tone: 'tone-bp',
        icon: '🩺',
      },
      {
        key: 'skin',
        label: 'Skin temp. (wrist)',
        value: format(v.skinTemperatureC, 1),
        unit: '°C',
        tone: 'tone-temp',
        meta: 'Samsung OBJECT_TEMPERATURE',
        icon: '✋',
      },
      {
        key: 'ambient',
        label: 'Ambient temp. (room)',
        value: format(v.ambientTemperatureC, 1),
        unit: '°C',
        tone: 'tone-temp',
        meta: 'Samsung AMBIENT_TEMPERATURE',
        danger: v.ambientTemperatureC != null && v.ambientTemperatureC > TEMP_LIMITS.max,
        icon: '🌡️',
      },
      {
        key: 'resp',
        label: 'Respiration',
        value: format(v.respirationRateBpm),
        unit: '/min',
        tone: 'tone-spo2',
        icon: '🌬️',
      },
      {
        key: 'hrv',
        label: 'HRV',
        value: format(v.heartRateVariabilityMs ?? v.hrvMs),
        unit: 'ms',
        tone: 'tone-ecg',
        icon: '🫀',
      },
      {
        key: 'stress',
        label: 'Stress',
        value: format(v.stressScore),
        unit: '/100',
        tone: 'tone-stress',
        danger: v.stressScore > STRESS_LIMITS.max,
        icon: '😟',
      },
      {
        key: 'sleep',
        label: 'Sleep score',
        value: format(v.sleepScore),
        unit: '/100',
        tone: 'tone-sleep',
        meta: v.sleepDurationMinutes ? `${Math.floor(v.sleepDurationMinutes / 60)}h ${v.sleepDurationMinutes % 60}m` : null,
        icon: '🛌',
      },
      {
        key: 'steps',
        label: 'Steps',
        value: format(v.stepsCount),
        unit: 'today',
        tone: 'tone-steps',
        meta: v.distanceMeters ? `${(v.distanceMeters / 1000).toFixed(2)} km` : null,
        icon: '👟',
      },
      {
        key: 'calories',
        label: 'Calories',
        value: format(v.caloriesBurned),
        unit: 'kcal',
        tone: 'tone-calories',
        meta: v.basalMetabolicRate ? `BMR ${format(v.basalMetabolicRate)} kcal` : null,
        icon: '🔥',
      },
      {
        key: 'ecg',
        label: 'ECG',
        value: v.ecgClassification || (v.ecgAverageHeartRate ?? v.ecgAvgHeartRateBpm ? `${format(v.ecgAverageHeartRate ?? v.ecgAvgHeartRateBpm)} bpm` : '—'),
        unit: v.ecgClassification ? '' : 'avg HR',
        tone: 'tone-ecg',
        danger: v.ecgClassification && !/normal|sinus/i.test(v.ecgClassification),
        icon: '📈',
      },
      {
        key: 'bia',
        label: 'Body fat',
        value: format(v.bodyFatPercent, 1),
        unit: '%',
        tone: 'tone-bia',
        meta: v.muscleMassKg ? `Muscle ${format(v.muscleMassKg, 1)} kg` : null,
        icon: '⚖️',
      },
      {
        key: 'glucose',
        label: 'Blood glucose',
        value: format(v.bloodGlucoseMgDl),
        unit: 'mg/dL',
        tone: 'tone-glucose',
        icon: '🩸',
      },
      {
        key: 'fall',
        label: 'Fall detection',
        value: v.fallDetected ? 'Detected' : 'Safe',
        unit: '',
        tone: v.fallDetected ? '' : 'tone-steps',
        danger: v.fallDetected,
        icon: '🚨',
      },
      {
        key: 'wear',
        label: 'Watch status',
        value: v.isWearing === false ? 'Off-wrist' : 'On-wrist',
        unit: '',
        tone: v.isWearing === false ? '' : 'tone-spo2',
        danger: v.isWearing === false,
        meta: v.batteryLevel != null ? `Battery ${format(v.batteryLevel)}%` : null,
        icon: '⌚',
      },
    ]
    return items
  }, [vitals])

  return (
    <div className="vitals-grid">
      {cards.map((card) => (
        <article
          key={card.key}
          className={`vital-card ${card.tone || ''} ${card.danger ? 'danger' : ''}`}
        >
          <span className="vital-icon" aria-hidden="true">{card.icon}</span>
          <span className="vital-label">{card.label}</span>
          <span>
            <span className="vital-value">{card.value}</span>
            {card.unit && <span className="vital-unit">{card.unit}</span>}
          </span>
          {card.meta && <span className="vital-meta">{card.meta}</span>}
        </article>
      ))}
    </div>
  )
}
