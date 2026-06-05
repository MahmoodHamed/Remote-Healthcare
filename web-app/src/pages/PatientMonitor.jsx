import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { HubConnectionBuilder, LogLevel } from '@microsoft/signalr'
import { normalizePatientId } from '../utils/patientId'

const DEFAULT_API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000'

function normalizePayload(payload) {
  if (!payload || typeof payload !== 'object') return null
  return {
    heartRateBpm: payload.heartRateBpm ?? null,
    spO2Percent: payload.spO2Percent ?? null,
    systolicBp: payload.systolicBp ?? null,
    diastolicBp: payload.diastolicBp ?? null,
    temperatureC: payload.temperatureC ?? null,
    skinTemperatureC: payload.skinTemperatureC ?? null,
    ambientTemperatureC: payload.ambientTemperatureC ?? null,
    hrvMs: payload.hrvMs ?? null,
    stressScore: payload.stressScore ?? null,
    bodyFatPercent: payload.bodyFatPercent ?? null,
    ecgAvgHeartRateBpm: payload.ecgAvgHeartRateBpm ?? null,
    stepsCount: payload.stepsCount ?? null,
    caloriesBurned: payload.caloriesBurned ?? null,
    fallDetected: Boolean(payload.fallDetected),
    isWearing: payload.isWearing !== false,
    recordedAt: payload.recordedAt ?? new Date().toISOString(),
  }
}

function fmt(value, digits = 0) {
  if (value === null || value === undefined || Number.isNaN(value)) return '--'
  return digits > 0 ? Number(value).toFixed(digits) : String(value)
}

const METRIC_DEFS = [
  { key: 'heartRateBpm', label: 'Heart rate', unit: 'bpm', tone: 'accent', digits: 0 },
  { key: 'spO2Percent', label: 'SpO₂', unit: '%', tone: 'teal', digits: 1 },
  { key: 'bp', label: 'Blood pressure', unit: 'mmHg', tone: 'warm', computed: true, unsupported: true },
  { key: 'temperatureC', label: 'Body temp.', unit: '°C', tone: 'amber', digits: 1 },
  { key: 'skinTemperatureC', label: 'Skin temp.', unit: '°C', tone: 'blue', digits: 1 },
  { key: 'hrvMs', label: 'HRV', unit: 'ms', tone: 'violet', digits: 0 },
  { key: 'respiration', label: 'Respiration', unit: '/min', tone: 'ink', unsupported: true },
  { key: 'stressScore', label: 'Stress', unit: '/100', tone: 'violet', digits: 0 },
  { key: 'sleep', label: 'Sleep score', unit: '/100', tone: 'ink', unsupported: true },
  { key: 'stepsCount', label: 'Steps', unit: 'today', tone: 'ink', digits: 0 },
  { key: 'caloriesBurned', label: 'Calories', unit: 'kcal', tone: 'ink', digits: 0 },
  { key: 'ecgAvgHeartRateBpm', label: 'ECG', unit: 'avg HR', tone: 'ink', digits: 0, onDemand: true },
  { key: 'bodyFatPercent', label: 'Body fat', unit: '%', tone: 'ink', digits: 1, onDemand: true },
  { key: 'glucose', label: 'Blood glucose', unit: 'mg/dL', tone: 'ink', unsupported: true },
  { key: 'fallDetected', label: 'Fall detection', unit: '', tone: 'danger', boolean: true },
  { key: 'isWearing', label: 'Watch status', unit: '', tone: 'teal', wearing: true },
]

function metricValue(vitals, def) {
  if (!vitals) return '--'
  if (def.unsupported) return '--'
  if (
    vitals &&
    !vitals.isWearing &&
    (def.key === 'heartRateBpm' || def.key === 'hrvMs')
  ) {
    return '--'
  }
  if (def.computed) {
    if (vitals.systolicBp == null && vitals.diastolicBp == null) return '--'
    return `${fmt(vitals.systolicBp)}/${fmt(vitals.diastolicBp)}`
  }
  if (def.boolean) {
    return vitals.fallDetected ? 'Alert' : 'Safe'
  }
  if (def.wearing) {
    return vitals.isWearing ? 'On wrist' : 'Off-wrist'
  }
  return fmt(vitals[def.key], def.digits ?? 0)
}

function metricTone(vitals, def) {
  if (!vitals) return def.tone
  if (def.boolean) return vitals.fallDetected ? 'danger' : 'teal'
  if (def.wearing) return vitals.isWearing ? 'teal' : 'danger'
  return def.tone
}

export default function PatientMonitor({ authProfile, accessToken, onLogout }) {
  const navigate = useNavigate()
  const [patientIdInput, setPatientIdInput] = useState('')
  const [connectionStatus, setConnectionStatus] = useState('disconnected')
  const [connectionError, setConnectionError] = useState('')
  const [latestVitals, setLatestVitals] = useState(null)
  const [timeline, setTimeline] = useState([])
  const connectionRef = useRef(null)

  useEffect(() => {
    if (!accessToken || !authProfile) navigate('/login')
  }, [accessToken, authProfile, navigate])

  const handleLogout = () => {
    connectionRef.current?.stop().catch(() => {})
    connectionRef.current = null
    onLogout?.()
    navigate('/login')
  }

  const connect = async () => {
    const patientId = normalizePatientId(patientIdInput)
    if (!patientId) {
      setConnectionError('Patient ID must be 6 characters (A-Z, 0-9), matching the watch (default ABC123).')
      return
    }

    const hubUrl = new URL('/hubs/vitals', DEFAULT_API_BASE).toString()
    setConnectionStatus('connecting')
    setConnectionError('')

    if (connectionRef.current) {
      await connectionRef.current.stop().catch(() => {})
      connectionRef.current = null
    }

    const connection = new HubConnectionBuilder()
      .withUrl(hubUrl, { accessTokenFactory: () => accessToken })
      .withAutomaticReconnect()
      .configureLogging(LogLevel.Warning)
      .build()

    connection.on('ReceiveVitals', (payload) => {
      const vitals = normalizePayload(payload)
      if (!vitals) return
      setLatestVitals(vitals)
      const stamp = new Date(vitals.recordedAt).toLocaleTimeString()
      setTimeline((prev) => [{ stamp, ...vitals }, ...prev].slice(0, 12))
    })

    connection.onreconnecting(() => setConnectionStatus('connecting'))
    connection.onreconnected(async () => {
      setConnectionStatus('connected')
      try {
        await connection.invoke('SubscribeToPatient', patientId)
      } catch (err) {
        setConnectionError(err?.message || 'Failed to re-subscribe.')
      }
    })
    connection.onclose(() => setConnectionStatus('disconnected'))
    connectionRef.current = connection

    try {
      await connection.start()
      await connection.invoke('SubscribeToPatient', patientId)
      setConnectionStatus('connected')
    } catch (err) {
      setConnectionStatus('error')
      setConnectionError(err?.message || 'Failed to connect to the patient monitor.')
      await connection.stop().catch(() => {})
      connectionRef.current = null
    }
  }

  const lastUpdate = latestVitals?.recordedAt
    ? new Date(latestVitals.recordedAt).toLocaleTimeString()
    : '--'

  return (
    <main>
      <section className="section">
        <div className="container">
          <div className="dashboard-header">
            <div>
              <p className="eyebrow">Patient Monitor</p>
              <h2>Live vitals · {patientIdInput || 'patient'}</h2>
              <p className="muted">Galaxy Watch 8 — Samsung Health Sensor SDK</p>
            </div>
            <div className="live-actions">
              <a className="btn btn-outline" href="/dashboard">Dashboard</a>
              <button className="btn btn-outline" type="button" onClick={handleLogout}>Sign out</button>
            </div>
          </div>
        </div>
      </section>

      <section className="section live">
        <div className="container live-grid">
          <div className="live-copy">
            <p className="eyebrow">Live sensor feed</p>
            <h2>All vitals from the watch</h2>
            <p>
              Metrics marked with SDK are streamed from the watch. Others need Samsung Health Platform or are not available on-device.
            </p>
            <div className={`live-status status-${connectionStatus}`}>
              <span className={`status-dot ${connectionStatus}`} aria-hidden="true" />
              <div>
                <strong>
                  {connectionStatus === 'connected' ? 'Live' : connectionStatus === 'connecting' ? 'Connecting' : 'Offline'}
                </strong>
                <span className="live-status-note">Last update: {lastUpdate}</span>
              </div>
            </div>
            {connectionError ? <p className="live-error">{connectionError}</p> : null}
          </div>

          <div className="live-panel">
            <div className="live-form">
              <label>
                Patient ID
                <input
                  type="text"
                  value={patientIdInput}
                  onChange={(e) => setPatientIdInput(e.target.value)}
                  placeholder="ABC123"
                  maxLength="6"
                />
              </label>
              <button className="btn btn-primary" type="button" onClick={connect}>
                {connectionStatus === 'connected' ? 'Reconnect' : 'Connect'}
              </button>
              <div className="sensor-grid sensor-grid-dense">
                {METRIC_DEFS.map((def) => (
                  <div
                    key={def.label}
                    className={`sensor-card tone-${metricTone(latestVitals, def)}${def.unsupported ? ' sensor-card-muted' : ''}`}
                  >
                    <span className="sensor-label">{def.label}</span>
                    <strong className="sensor-value">
                      {metricValue(latestVitals, def)}
                      {def.unit && metricValue(latestVitals, def) !== '--' ? (
                        <small>{def.unit}</small>
                      ) : def.unit ? (
                        <small>{def.unit}</small>
                      ) : null}
                    </strong>
                    {def.unsupported ? <span className="sensor-hint">Not on SDK</span> : null}
                    {def.onDemand && !def.unsupported ? (
                      <span className="sensor-hint">On-demand on watch</span>
                    ) : null}
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      {timeline.length > 0 ? (
        <section className="section alt">
          <div className="container">
            <div className="section-head">
              <p className="eyebrow">Recent readings</p>
              <h2>Live history</h2>
            </div>
            <table className="vitals-table">
              <thead>
                <tr>
                  <th>Time</th>
                  <th>HR</th>
                  <th>SpO₂</th>
                  <th>Body °C</th>
                  <th>Skin °C</th>
                  <th>HRV</th>
                  <th>Steps</th>
                  <th>Fall</th>
                  <th>Wear</th>
                </tr>
              </thead>
              <tbody>
                {timeline.map((row, index) => (
                  <tr key={`${row.stamp}-${index}`}>
                    <td>{row.stamp}</td>
                    <td>{fmt(row.heartRateBpm)}</td>
                    <td>{fmt(row.spO2Percent, 1)}</td>
                    <td>{fmt(row.temperatureC, 1)}</td>
                    <td>{fmt(row.skinTemperatureC, 1)}</td>
                    <td>{fmt(row.hrvMs)}</td>
                    <td>{fmt(row.stepsCount)}</td>
                    <td>{row.fallDetected ? 'Yes' : 'No'}</td>
                    <td>{row.isWearing ? 'On' : 'Off'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      ) : null}
    </main>
  )
}

