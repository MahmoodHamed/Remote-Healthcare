import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { HubConnectionBuilder, LogLevel } from '@microsoft/signalr'
import { resolveConnectPatientId } from '../utils/patientId'
import { HISTORY_COLUMNS, SDK_SENSOR_INFO, SUPPORTED_METRIC_DEFS } from '../utils/supportedVitals'
import { inferWearing, mergeVitals, normalizePayload } from '../utils/vitalsUtils'

const DEFAULT_API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000'

function fmt(value, digits = 0) {
  if (value === null || value === undefined || Number.isNaN(value)) return '--'
  return digits > 0 ? Number(value).toFixed(digits) : String(value)
}

function metricValue(vitals, def) {
  if (!vitals) return '--'
  if (def.boolean) {
    return vitals.fallDetected ? 'Alert' : 'Safe'
  }
  if (def.wearing) {
    return inferWearing(vitals) ? 'On wrist' : 'Off-wrist'
  }
  return fmt(vitals[def.key], def.digits ?? 0)
}

function metricTone(vitals, def) {
  if (!vitals) return def.tone
  if (def.boolean) return vitals.fallDetected ? 'danger' : 'teal'
  if (def.wearing) return inferWearing(vitals) ? 'teal' : 'danger'
  return def.tone
}

export default function PatientMonitor({ authProfile, accessToken, onLogout }) {
  const navigate = useNavigate()
  const [patientIdInput, setPatientIdInput] = useState('')
  const [connectionStatus, setConnectionStatus] = useState('disconnected')
  const [connectionError, setConnectionError] = useState('')
  const [latestVitals, setLatestVitals] = useState(null)
  const [timeline, setTimeline] = useState([])
  const [devices, setDevices] = useState([])
  const connectionRef = useRef(null)
  const patientGuidRef = useRef('')

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
    const patientId = await resolveConnectPatientId(patientIdInput, authProfile, {
      apiBase: DEFAULT_API_BASE,
      accessToken,
    })
    if (!patientId) {
      setConnectionError('Enter a 6-character watch code or patient GUID. Patients can leave blank to use their account.')
      return
    }
    patientGuidRef.current = patientId

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
      setLatestVitals((prev) => {
        const merged = mergeVitals(prev, vitals)
        const stamp = new Date(merged.recordedAt).toLocaleTimeString()
        setTimeline((rows) => [{ stamp, ...merged }, ...rows].slice(0, 12))
        return merged
      })
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

      // Fetch linked devices for this patient
      try {
        const res = await fetch(
          new URL(`/api/patients/${patientId}/devices`, DEFAULT_API_BASE).toString(),
          { headers: { Authorization: `Bearer ${accessToken}` } }
        )
        if (res.ok) setDevices(await res.json())
      } catch { /* non-critical */ }
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
              Only vitals the Galaxy Watch can measure and share are shown — Samsung Health Sensor SDK (continuous &amp; on-demand) plus steps, calories, and fall detection.
            </p>
            <div className="sdk-sensor-list">
              <p className="eyebrow" style={{ marginTop: '1rem' }}>Samsung SDK sensors</p>
              <ul className="sdk-list">
                {SDK_SENSOR_INFO.continuous.map((s) => (
                  <li key={s.tracker}><strong>{s.name}</strong> — continuous · {s.note}</li>
                ))}
                {SDK_SENSOR_INFO.onDemand.map((s) => (
                  <li key={s.tracker}><strong>{s.name}</strong> — on-demand · {s.note}</li>
                ))}
              </ul>
              <p className="eyebrow" style={{ marginTop: '0.75rem' }}>Platform sensors</p>
              <ul className="sdk-list">
                {SDK_SENSOR_INFO.platform.map((s) => (
                  <li key={s.name}><strong>{s.name}</strong> — {s.note}</li>
                ))}
              </ul>
            </div>
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

            {/* Linked devices panel */}
            {devices.length > 0 && (
              <div className="device-panel">
                <p className="eyebrow" style={{marginBottom: '0.5rem', marginTop: '1.5rem'}}>Linked Watch</p>
                {devices.map((d) => (
                  <div key={d.id} className="device-card">
                    <div className="device-status-dot" data-status={d.status?.toLowerCase()} />
                    <div className="device-info">
                      <strong>{d.deviceName !== 'unknown' ? d.deviceName : d.deviceModel}</strong>
                      <span className={`device-badge badge-${d.status?.toLowerCase()}`}>{d.status}</span>
                      {d.batteryLevel != null && (
                        <span className="device-meta">🔋 {Math.round(d.batteryLevel)}%</span>
                      )}
                      {d.lastSeenAt && (
                        <span className="device-meta">Last seen: {new Date(d.lastSeenAt).toLocaleTimeString()}</span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
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
                {SUPPORTED_METRIC_DEFS.map((def) => (
                  <div
                    key={def.label}
                    className={`sensor-card tone-${metricTone(latestVitals, def)}`}
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
                    {def.onDemand ? (
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
                  {HISTORY_COLUMNS.map((col) => (
                    <th key={col.key}>{col.label}</th>
                  ))}
                  <th>Fall</th>
                  <th>Wear</th>
                </tr>
              </thead>
              <tbody>
                {timeline.map((row, index) => (
                  <tr key={`${row.stamp}-${index}`}>
                    <td>{row.stamp}</td>
                    {HISTORY_COLUMNS.map((col) => (
                      <td key={col.key}>{fmt(row[col.key], col.digits ?? 0)}</td>
                    ))}
                    <td>{row.fallDetected ? 'Yes' : 'No'}</td>
                    <td>{inferWearing(row) ? 'On' : 'Off'}</td>
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

