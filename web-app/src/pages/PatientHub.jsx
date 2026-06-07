import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, NavLink, Navigate, useNavigate } from 'react-router-dom'
import { HubConnectionBuilder, LogLevel } from '@microsoft/signalr'
import { apiFetch } from '../api/client'
import { accountUserId, normalizeGuid } from '../utils/patientId'
import { SUPPORTED_METRIC_DEFS } from '../utils/supportedVitals'
import { inferWearing, mergeVitals, normalizePayload } from '../utils/vitalsUtils'

const DEFAULT_API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000'
const DEFAULT_WATCH_CODE = 'ABC123'
const SHORT_ID_PATTERN = /^[A-Za-z0-9]{6}$/

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
  const streamingId = accountUserId(authProfile)

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
  const [shortCodeInput, setShortCodeInput] = useState(DEFAULT_WATCH_CODE)
  const [mqttHost, setMqttHost] = useState('')
  const [mqttPort, setMqttPort] = useState('')
  const [devices, setDevices] = useState([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [saved, setSaved] = useState(false)
  const [streamingId, setStreamingId] = useState(() => accountUserId(authProfile))

  const loadPairing = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [pairRes, devRes] = await Promise.all([
        apiFetch('/api/devices/pairing-info'),
        apiFetch('/api/devices'),
      ])
      if (!pairRes.ok) throw new Error('Could not load pairing info from the server.')
      const pairData = await pairRes.json()
      const code = pairData?.patientId ?? pairData?.PatientId ?? ''
      const fromApi = pairData?.streamingPatientId ?? pairData?.StreamingPatientId ?? ''
      setShortCodeInput(code || DEFAULT_WATCH_CODE)
      setStreamingId(normalizeGuid(fromApi) || accountUserId(authProfile))
      setMqttHost(pairData?.mqttHost ?? pairData?.MqttHost ?? '')
      setMqttPort(String(pairData?.mqttPort ?? pairData?.MqttPort ?? ''))
      if (devRes.ok) setDevices(await devRes.json())
    } catch (err) {
      setStreamingId(accountUserId(authProfile))
      setMqttHost((prev) => prev || 'remote-care.tech')
      setMqttPort((prev) => prev || '1883')
      setShortCodeInput((prev) => prev || DEFAULT_WATCH_CODE)
      setError(err?.message || 'Failed to load watch pairing info.')
    } finally {
      setLoading(false)
    }
  }, [authProfile])

  useEffect(() => {
    loadPairing()
  }, [loadPairing, accessToken])

  const savePairing = async () => {
    const code = shortCodeInput.trim().toUpperCase()
    if (!SHORT_ID_PATTERN.test(code)) {
      setError('Patient short ID must be exactly 6 letters or digits (e.g. ABC123).')
      return
    }
    setSaving(true)
    setError('')
    try {
      const res = await apiFetch('/api/devices/pairing-info', {
        method: 'PUT',
        body: JSON.stringify({ patientId: code }),
      })
      if (!res.ok) {
        const data = await res.json().catch(() => null)
        throw new Error(data?.message || data?.title || `Save failed (${res.status}).`)
      }
      const pairData = await res.json()
      const fromApi = pairData?.streamingPatientId ?? pairData?.StreamingPatientId ?? ''
      setShortCodeInput(pairData?.patientId ?? pairData?.PatientId ?? code)
      setStreamingId(normalizeGuid(fromApi) || accountUserId(authProfile))
      setMqttHost(pairData?.mqttHost ?? pairData?.MqttHost ?? mqttHost)
      setMqttPort(String(pairData?.mqttPort ?? pairData?.MqttPort ?? mqttPort))
      setSaved(true)
      setTimeout(() => setSaved(false), 2500)
    } catch (err) {
      setError(err?.message || 'Failed to save pairing details.')
    } finally {
      setSaving(false)
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
          <p className="muted">Choose a 6-character code here, save it, then enter the same code on the watch.</p>
        </div>
      </div>

      {error ? <p className="live-error">{error}</p> : null}

      <div className="watch-steps">
        <div className="watch-step-card">
          <span className="step-num">1</span>
          <div>
            <strong>Choose your patient ID</strong>
            <p className="muted">Type a 6-character code below (e.g. ABC123) and tap Save pairing details.</p>
          </div>
        </div>
        <div className="watch-step-card">
          <span className="step-num">2</span>
          <div>
            <strong>Enter it on the watch</strong>
            <p className="muted">Open the watch app → Settings → Patient ID and type the same code.</p>
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
          <input
            type="text"
            value={shortCodeInput}
            onChange={(e) => {
              setShortCodeInput(e.target.value.replace(/[^A-Za-z0-9]/g, '').slice(0, 6).toUpperCase())
              setSaved(false)
            }}
            placeholder="ABC123"
            maxLength={6}
            className="mono-input"
          />
          <span className="field-hint">Must match the watch exactly. Your dashboard listens for this code after you save.</span>
        </label>
        <label>
          Streaming patient ID
          <input type="text" value={streamingId} readOnly className="mono-input" />
          <span className="field-hint">Your account GUID — same on web and mobile. The watch uses the short code only.</span>
        </label>
        <label>
          MQTT host
          <input type="text" value={mqttHost} readOnly className="mono-input" />
        </label>
        <label>
          MQTT port
          <input type="text" value={mqttPort} readOnly className="mono-input" />
        </label>
        <button className="btn btn-primary" type="button" onClick={savePairing} disabled={saving}>
          {saving ? 'Saving…' : saved ? 'Saved' : 'Save pairing details'}
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
