import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, NavLink, Navigate, useNavigate } from 'react-router-dom'
import { HubConnectionBuilder, LogLevel } from '@microsoft/signalr'
import { apiFetch } from '../api/client'
import { normalizeGuid, resolveHubPatientId } from '../utils/patientId'
import { SUPPORTED_METRIC_DEFS } from '../utils/supportedVitals'
import { inferWearing, mergeVitals, normalizePayload } from '../utils/vitalsUtils'

const DEFAULT_API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000'

function fmt(value, digits = 0) {
  if (value === null || value === undefined || Number.isNaN(value)) return '--'
  return digits > 0 ? Number(value).toFixed(digits) : String(value)
}

function metricValue(vitals, def) {
  if (!vitals) return '--'
  if (def.boolean) return vitals.fallDetected ? 'Alert' : 'Safe'
  if (def.wearing) return inferWearing(vitals) ? 'On wrist' : 'Off-wrist'
  return fmt(vitals[def.key], def.digits ?? 0)
}

function metricTone(vitals, def) {
  if (!vitals) return def.tone
  if (def.boolean) return vitals.fallDetected ? 'danger' : 'teal'
  if (def.wearing) return inferWearing(vitals) ? 'teal' : 'danger'
  return def.tone
}

function PatientSidebar({ authProfile, onLogout }) {
  return (
    <aside className="patient-sidebar">
      <Link className="brand compact" to="/hub">
        <span className="brand-mark">RC</span>
        My health hub
      </Link>
      <nav className="patient-nav">
        <NavLink to="/hub/vitals" className={({ isActive }) => (isActive ? 'active' : '')}>My vitals</NavLink>
        <NavLink to="/hub/watch" className={({ isActive }) => (isActive ? 'active' : '')}>My watch</NavLink>
      </nav>
      <div className="patient-sidebar-footer">
        <strong>{authProfile?.fullName || 'Patient'}</strong>
        <span className="muted">{authProfile?.email}</span>
        <span className="role-pill">{authProfile?.role}</span>
        <button className="btn btn-outline btn-sm" type="button" onClick={onLogout}>Sign out</button>
      </div>
    </aside>
  )
}

function PatientVitalsPanel({ authProfile, accessToken }) {
  const [connectionStatus, setConnectionStatus] = useState('disconnected')
  const [connectionError, setConnectionError] = useState('')
  const [latestVitals, setLatestVitals] = useState(null)
  const connectionRef = useRef(null)
  const streamingId = normalizeGuid(authProfile?.id)

  const connect = useCallback(async () => {
    if (!streamingId || !accessToken) return

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
      setLatestVitals((prev) => mergeVitals(prev, vitals))
    })

    connection.onreconnecting(() => setConnectionStatus('connecting'))
    connection.onreconnected(async () => {
      setConnectionStatus('connected')
      try {
        await connection.invoke('SubscribeToPatient', streamingId)
      } catch (err) {
        setConnectionError(err?.message || 'Failed to re-subscribe.')
      }
    })
    connection.onclose(() => setConnectionStatus('disconnected'))
    connectionRef.current = connection

    try {
      await connection.start()
      await connection.invoke('SubscribeToPatient', streamingId)
      setConnectionStatus('connected')
    } catch (err) {
      setConnectionStatus('error')
      setConnectionError(err?.message || 'Failed to connect.')
      await connection.stop().catch(() => {})
      connectionRef.current = null
    }
  }, [accessToken, streamingId])

  useEffect(() => {
    connect()
    return () => {
      connectionRef.current?.stop().catch(() => {})
      connectionRef.current = null
    }
  }, [connect])

  const lastUpdate = latestVitals?.recordedAt
    ? new Date(latestVitals.recordedAt).toLocaleTimeString()
    : '--'

  return (
    <div className="patient-hub-content">
      <div className="dashboard-header">
        <div>
          <p className="eyebrow">My vitals</p>
          <h2>Live sensor feed</h2>
          <p className="muted">Galaxy Watch 8 — Samsung Health Sensor SDK</p>
        </div>
        <div className={`live-status status-${connectionStatus}`}>
          <span className={`status-dot ${connectionStatus}`} aria-hidden="true" />
          <div>
            <strong>{connectionStatus === 'connected' ? 'Live' : connectionStatus === 'connecting' ? 'Connecting' : 'Offline'}</strong>
            <span className="live-status-note">Last update: {lastUpdate}</span>
          </div>
        </div>
      </div>
      {connectionError ? <p className="live-error">{connectionError}</p> : null}
      <div className="sensor-grid sensor-grid-dense">
        {SUPPORTED_METRIC_DEFS.map((def) => (
          <div key={def.label} className={`sensor-card tone-${metricTone(latestVitals, def)}`}>
            <span className="sensor-label">{def.label}</span>
            <strong className="sensor-value">
              {metricValue(latestVitals, def)}
              {def.unit ? <small>{def.unit}</small> : null}
            </strong>
          </div>
        ))}
      </div>
    </div>
  )
}

function PatientWatchPanel({ authProfile, accessToken }) {
  const [pairing, setPairing] = useState(null)
  const [devices, setDevices] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [saved, setSaved] = useState(false)

  const streamingId = normalizeGuid(
    pairing?.streamingPatientId ?? pairing?.StreamingPatientId ?? authProfile?.id,
  )

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      setLoading(true)
      setError('')
      try {
        const [pairRes, devRes] = await Promise.all([
          apiFetch('/api/devices/pairing-info'),
          apiFetch('/api/devices'),
        ])
        if (!pairRes.ok) throw new Error('Could not load pairing info from the server.')
        const pairData = await pairRes.json()
        if (!cancelled) setPairing(pairData)
        if (devRes.ok && !cancelled) setDevices(await devRes.json())
      } catch (err) {
        if (!cancelled) setError(err?.message || 'Failed to load watch pairing info.')
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()
    return () => { cancelled = true }
  }, [accessToken])

  const shortCode = pairing?.patientId ?? pairing?.PatientId ?? ''

  const reloadPairing = async () => {
    setLoading(true)
    setError('')
    try {
      const [pairRes, devRes] = await Promise.all([
        apiFetch('/api/devices/pairing-info'),
        apiFetch('/api/devices'),
      ])
      if (!pairRes.ok) throw new Error('Could not load pairing info from the server.')
      setPairing(await pairRes.json())
      if (devRes.ok) setDevices(await devRes.json())
      setSaved(true)
      setTimeout(() => setSaved(false), 2500)
    } catch (err) {
      setError(err?.message || 'Failed to refresh pairing info.')
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="patient-hub-content">
        <p className="muted">Loading pairing details…</p>
      </div>
    )
  }

  return (
    <div className="patient-hub-content">
      <div className="dashboard-header">
        <div>
          <p className="eyebrow">My watch</p>
          <h2>Pair your Galaxy Watch</h2>
          <p className="muted">Use the code below on the watch — it is unique to your account.</p>
        </div>
      </div>

      {error ? <p className="live-error">{error}</p> : null}

      <div className="watch-steps">
        <div className="watch-step-card">
          <span className="step-num">1</span>
          <div>
            <strong>Install the watch app</strong>
            <p className="muted">Side-load the Remote Care Wear OS APK on your Galaxy Watch.</p>
          </div>
        </div>
        <div className="watch-step-card">
          <span className="step-num">2</span>
          <div>
            <strong>Enter your patient ID</strong>
            <p className="muted">Open the watch app Settings and type the 6-character ID shown below.</p>
          </div>
        </div>
        <div className="watch-step-card">
          <span className="step-num">3</span>
          <div>
            <strong>Press start</strong>
            <p className="muted">The watch streams HR, SpO₂, ECG, and more to this dashboard.</p>
          </div>
        </div>
      </div>

      <div className="pairing-form card-panel">
        <h3>Pairing details</h3>
        <label>
          Patient short ID
          <input type="text" value={shortCode} readOnly className="mono-input" />
          <span className="field-hint">Enter this exact code on the watch. Assigned by the server for your account.</span>
        </label>
        <label>
          Streaming patient ID
          <input type="text" value={streamingId} readOnly className="mono-input" />
          <span className="field-hint">Your account GUID — used internally by the server after the short code is resolved.</span>
        </label>
        <label>
          MQTT host
          <input type="text" value={pairing?.mqttHost ?? pairing?.MqttHost ?? ''} readOnly className="mono-input" />
        </label>
        <label>
          MQTT port
          <input type="text" value={String(pairing?.mqttPort ?? pairing?.MqttPort ?? '')} readOnly className="mono-input" />
        </label>
        <button className="btn btn-primary" type="button" onClick={reloadPairing}>
          {saved ? 'Refreshed' : 'Refresh pairing details'}
        </button>
      </div>

      {devices.length > 0 && (
        <div className="device-panel" style={{ marginTop: '1.5rem' }}>
          <p className="eyebrow">Linked watch</p>
          {devices.map((d) => (
            <div key={d.id} className="device-card">
              <div className="device-status-dot" data-status={d.status?.toLowerCase()} />
              <div className="device-info">
                <strong>{d.deviceName !== 'unknown' ? d.deviceName : d.deviceModel}</strong>
                <span className={`device-badge badge-${d.status?.toLowerCase()}`}>{d.status}</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

export default function PatientHub({ authProfile, accessToken, onLogout, section = 'vitals' }) {
  const navigate = useNavigate()

  if (!accessToken || !authProfile) return <Navigate to="/login" replace />
  if (authProfile.role !== 'Patient') return <Navigate to="/monitor" replace />

  const handleLogout = () => {
    onLogout?.()
    navigate('/login')
  }

  return (
    <div className="patient-hub-layout">
      <PatientSidebar authProfile={authProfile} onLogout={handleLogout} />
      <main className="patient-hub-main">
        {section === 'watch' ? (
          <PatientWatchPanel authProfile={authProfile} accessToken={accessToken} />
        ) : (
          <PatientVitalsPanel authProfile={authProfile} accessToken={accessToken} />
        )}
      </main>
    </div>
  )
}
