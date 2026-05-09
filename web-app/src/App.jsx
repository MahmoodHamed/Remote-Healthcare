import { useEffect, useRef, useState } from 'react'
import { HubConnectionBuilder, LogLevel } from '@microsoft/signalr'
import './App.css'

const roles = [
  {
    id: 'patient',
    title: 'Patient',
    description: 'Pair your watch and review your own vitals.',
  },
  {
    id: 'doctor',
    title: 'Doctor',
    description: 'Monitor assigned patients and receive live alerts.',
  },
  {
    id: 'family',
    title: 'Family',
    description: 'Follow live vitals for your loved one.',
  },
]

const DEFAULT_API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000'

const normalizeVitals = (payload) => {
  if (!payload || typeof payload !== 'object') return null
  const lowered = {}
  Object.entries(payload).forEach(([key, value]) => {
    lowered[key.toLowerCase()] = value
  })

  return {
    id: lowered.id ?? null,
    patientId: lowered.patientid ?? null,
    deviceId: lowered.deviceid ?? null,
    heartRateBpm: lowered.heartratebpm ?? null,
    spO2Percent: lowered.spo2percent ?? null,
    systolicBp: lowered.systolicbp ?? null,
    diastolicBp: lowered.diastolicbp ?? null,
    temperatureC: lowered.temperaturec ?? null,
    stepsCount: lowered.stepscount ?? lowered.steps ?? null,
    caloriesBurned: lowered.caloriesburned ?? lowered.calories ?? null,
    fallDetected: lowered.falldetected ?? false,
    isWearing: lowered.iswearing ?? true,
    recordedAt: lowered.recordedat ?? null,
  }
}

const formatMetric = (value, unit) => {
  if (value === null || value === undefined || Number.isNaN(value)) return '--'
  const num = typeof value === 'number' ? value : Number(value)
  const safe = Number.isFinite(num) ? num : value
  return unit ? `${safe} ${unit}` : `${safe}`
}

const formatTimestamp = (value) => {
  if (!value) return 'n/a'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'n/a'
  return date.toLocaleString()
}

const compactId = (value) => {
  if (!value) return 'n/a'
  const text = String(value)
  if (text.length <= 12) return text
  return `${text.slice(0, 8)}...${text.slice(-4)}`
}

const guidPattern = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/
const shortIdPattern = /^[A-Za-z0-9]{6}$/

const md5Bytes = (message) => {
  const encoder = new TextEncoder()
  const msg = encoder.encode(message)
  const msgLen = msg.length
  const bitLen = msgLen * 8
  const paddedLen = ((msgLen + 9 + 63) >> 6) << 6
  const buffer = new Uint8Array(paddedLen)
  buffer.set(msg)
  buffer[msgLen] = 0x80

  const view = new DataView(buffer.buffer)
  view.setUint32(paddedLen - 8, bitLen >>> 0, true)
  view.setUint32(paddedLen - 4, Math.floor(bitLen / 0x100000000), true)

  let a = 0x67452301
  let b = 0xefcdab89
  let c = 0x98badcfe
  let d = 0x10325476

  const r = [
    7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
    5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
    4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
    6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21,
  ]
  const k = new Uint32Array(64)
  for (let i = 0; i < 64; i += 1) {
    k[i] = Math.floor(Math.abs(Math.sin(i + 1)) * 0x100000000)
  }

  const chunk = new Uint32Array(16)

  for (let offset = 0; offset < buffer.length; offset += 64) {
    for (let i = 0; i < 16; i += 1) {
      chunk[i] = view.getUint32(offset + i * 4, true)
    }

    let aa = a
    let bb = b
    let cc = c
    let dd = d

    for (let i = 0; i < 64; i += 1) {
      let f = 0
      let g = 0

      if (i < 16) {
        f = (bb & cc) | (~bb & dd)
        g = i
      } else if (i < 32) {
        f = (dd & bb) | (~dd & cc)
        g = (5 * i + 1) % 16
      } else if (i < 48) {
        f = bb ^ cc ^ dd
        g = (3 * i + 5) % 16
      } else {
        f = cc ^ (bb | ~dd)
        g = (7 * i) % 16
      }

      const temp = dd
      dd = cc
      cc = bb
      const sum = (aa + f + k[i] + chunk[g]) >>> 0
      bb = (bb + ((sum << r[i]) | (sum >>> (32 - r[i])))) >>> 0
      aa = temp
    }

    a = (a + aa) >>> 0
    b = (b + bb) >>> 0
    c = (c + cc) >>> 0
    d = (d + dd) >>> 0
  }

  const out = new Uint8Array(16)
  const outView = new DataView(out.buffer)
  outView.setUint32(0, a, true)
  outView.setUint32(4, b, true)
  outView.setUint32(8, c, true)
  outView.setUint32(12, d, true)
  return out
}

const bytesToUuid = (bytes) => {
  const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('')
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}

const normalizePatientId = (value) => {
  const trimmed = value.trim()
  if (!trimmed) return ''
  if (guidPattern.test(trimmed)) return trimmed.toLowerCase()
  if (!shortIdPattern.test(trimmed)) return ''

  const bytes = md5Bytes(trimmed)
  bytes[6] = (bytes[6] & 0x0f) | 0x30
  bytes[8] = (bytes[8] & 0x3f) | 0x80
  return bytesToUuid(bytes)
}

const parseErrorMessage = async (response) => {
  try {
    const data = await response.json()
    if (!data) return ''
    if (typeof data === 'string') return data
    if (typeof data.message === 'string') return data.message
    if (typeof data.title === 'string') return data.title
    if (data.errors && typeof data.errors === 'object') {
      const [firstKey] = Object.keys(data.errors)
      if (firstKey) {
        const detail = data.errors[firstKey]
        if (Array.isArray(detail) && detail[0]) return `${firstKey}: ${detail[0]}`
        if (typeof detail === 'string') return `${firstKey}: ${detail}`
      }
    }
  } catch {
    return ''
  }
  return ''
}

function App() {
  const [menuOpen, setMenuOpen] = useState(false)
  const [activeRole, setActiveRole] = useState('patient')
  const [apiBaseUrl, setApiBaseUrl] = useState(DEFAULT_API_BASE)
  const [patientIdInput, setPatientIdInput] = useState('')
  const [accessToken, setAccessToken] = useState('')
  const [mqttHost, setMqttHost] = useState('')
  const [mqttPort, setMqttPort] = useState('1883')
  const [loginEmail, setLoginEmail] = useState('')
  const [loginPassword, setLoginPassword] = useState('')
  const [loginDeviceInfo, setLoginDeviceInfo] = useState('web-app')
  const [autoConnectOnLogin, setAutoConnectOnLogin] = useState(true)
  const [authStatus, setAuthStatus] = useState('signed-out')
  const [authError, setAuthError] = useState('')
  const [authProfile, setAuthProfile] = useState(null)
  const [authExpiresAt, setAuthExpiresAt] = useState(null)
  const [connectionStatus, setConnectionStatus] = useState('disconnected')
  const [connectionError, setConnectionError] = useState('')
  const [vitals, setVitals] = useState(null)
  const [vitalsHistory, setVitalsHistory] = useState([])
  const [lastAlert, setLastAlert] = useState(null)
  const connectionRef = useRef(null)

  useEffect(() => () => {
    if (connectionRef.current) {
      connectionRef.current.stop().catch(() => {})
      connectionRef.current = null
    }
  }, [])

  const connectToVitalsHub = async (options = {}) => {
    const base = (options.baseOverride ?? apiBaseUrl).trim()
    const patientIdRaw = (options.patientOverride ?? patientIdInput).trim()
    const patientId = normalizePatientId(patientIdRaw)
    const token = (options.tokenOverride ?? accessToken).trim()

    if (!base || !patientId || !token) {
      setConnectionStatus('error')
      if (!patientIdRaw || !shortIdPattern.test(patientIdRaw)) {
        setConnectionError('Patient ID must be 6 characters (A-Z, 0-9).')
      } else {
        setConnectionError(token ? 'API base URL and patient ID are required.' : 'Sign in to get an access token.')
      }
      return
    }

    const hubUrl = (() => {
      try {
        return new URL('/hubs/vitals', base).toString()
      } catch {
        return ''
      }
    })()

    if (!hubUrl) {
      setConnectionStatus('error')
      setConnectionError('Invalid API base URL.')
      return
    }

    setConnectionStatus('connecting')
    setConnectionError('')
    setLastAlert(null)

    if (connectionRef.current) {
      await connectionRef.current.stop().catch(() => {})
      connectionRef.current = null
    }

    const connection = new HubConnectionBuilder()
      .withUrl(hubUrl, {
        accessTokenFactory: () => token,
      })
      .withAutomaticReconnect()
      .configureLogging(LogLevel.Warning)
      .build()

    connection.on('ReceiveVitals', (payload) => {
      const normalized = normalizeVitals(payload)
      if (!normalized) return

      setVitals(normalized)

      const snapshotTime = normalized.recordedAt ? new Date(normalized.recordedAt) : new Date()
      const entry = {
        time: Number.isNaN(snapshotTime.getTime()) ? 'n/a' : snapshotTime.toLocaleTimeString(),
        heartRateBpm: normalized.heartRateBpm ?? '--',
        spO2Percent: normalized.spO2Percent ?? '--',
        temperatureC: normalized.temperatureC ?? '--',
      }

      setVitalsHistory((prev) => [entry, ...prev].slice(0, 12))
    })

    connection.on('ReceiveAlert', (payload) => {
      setLastAlert(payload)
    })

    connection.onreconnecting(() => {
      setConnectionStatus('connecting')
    })

    connection.onreconnected(async () => {
      setConnectionStatus('connected')
      try {
        await connection.invoke('SubscribeToPatient', patientId)
      } catch (err) {
        setConnectionError(err?.message || 'Failed to re-subscribe after reconnect.')
      }
    })

    connection.onclose(() => {
      setConnectionStatus('disconnected')
    })

    connectionRef.current = connection

    try {
      await connection.start()
      await connection.invoke('SubscribeToPatient', patientId)
      setConnectionStatus('connected')
    } catch (err) {
      setConnectionStatus('error')
      setConnectionError(err?.message || 'Failed to connect to vitals hub.')
      await connection.stop().catch(() => {})
      connectionRef.current = null
    }
  }

  const disconnectFromVitalsHub = async () => {
    if (!connectionRef.current) return
    await connectionRef.current.stop().catch(() => {})
    connectionRef.current = null
    setConnectionStatus('disconnected')
  }

  const loginToApi = async () => {
    const base = apiBaseUrl.trim()
    const email = loginEmail.trim()
    const password = loginPassword

    if (!base || !email || !password) {
      setAuthStatus('error')
      setAuthError('API base URL, email, and password are required.')
      return
    }

    const loginUrl = (() => {
      try {
        return new URL('/api/auth/login', base).toString()
      } catch {
        return ''
      }
    })()

    if (!loginUrl) {
      setAuthStatus('error')
      setAuthError('Invalid API base URL.')
      return
    }

    setAuthStatus('loading')
    setAuthError('')
    setConnectionError('')

    try {
      const response = await fetch(loginUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email,
          password,
          deviceInfo: loginDeviceInfo.trim() || null,
        }),
      })

      if (!response.ok) {
        const message = await parseErrorMessage(response)
        throw new Error(message || `Login failed (${response.status}).`)
      }

      const data = await response.json()
      const token = data?.tokens?.accessToken
      if (!token) throw new Error('Access token missing from response.')

      setAccessToken(token)
      setAuthProfile(data?.user ?? null)
      setAuthExpiresAt(data?.tokens?.expiresAt ?? null)
      setAuthStatus('signed-in')
      setLoginPassword('')

      if (autoConnectOnLogin && patientIdInput.trim()) {
        await connectToVitalsHub({
          tokenOverride: token,
          baseOverride: base,
          patientOverride: patientIdInput,
        })
      }
    } catch (err) {
      setAuthStatus('error')
      setAuthError(err?.message || 'Login failed.')
    }
  }

  const signOut = async () => {
    await disconnectFromVitalsHub()
    setAccessToken('')
    setAuthProfile(null)
    setAuthExpiresAt(null)
    setAuthStatus('signed-out')
    setAuthError('')
    setConnectionError('')
    setVitals(null)
    setVitalsHistory([])
    setLastAlert(null)
  }

  const isConnected = connectionStatus === 'connected'
  const isSignedIn = authStatus === 'signed-in'
  const displayPatientId = patientIdInput.trim() || vitals?.patientId || ''
  const activeRoleLabel = roles.find((role) => role.id === activeRole)?.title || 'User'
  const statusLabel = {
    connected: 'Connected to SignalR',
    connecting: 'Connecting to SignalR',
    disconnected: 'Disconnected',
    error: 'Connection error',
  }[connectionStatus]
  const authStatusLabel = {
    'signed-out': 'Signed out',
    loading: 'Signing in...',
    'signed-in': 'Signed in',
    error: 'Sign-in error',
  }[authStatus]
  const hasBp = vitals && (
    (vitals.systolicBp !== null && vitals.systolicBp !== undefined) ||
    (vitals.diastolicBp !== null && vitals.diastolicBp !== undefined)
  )
  const bpValue = hasBp
    ? `${vitals.systolicBp ?? '--'}/${vitals.diastolicBp ?? '--'} mmHg`
    : '--'
  const wearingValue = vitals ? (vitals.isWearing ? 'Yes' : 'No') : '--'
  const fallValue = vitals ? (vitals.fallDetected ? 'Yes' : 'No') : '--'

  return (
    <div className={`page ${menuOpen ? 'menu-open' : ''}`}>
      <header className="nav">
        <div className="container nav-inner">
          <a className="brand" href="#top" aria-label="Remote Care">
            <span className="brand-mark">RC</span>
            Remote Care
          </a>
          <nav className="nav-links">
            <a href="#login">Sign in</a>
            <a href="#link">Link watch</a>
            <a href="#vitals">Live vitals</a>
          </nav>
          <div className="nav-cta">
            <a className="btn btn-primary" href="#login">Sign in</a>
          </div>
          <button
            className="nav-toggle"
            type="button"
            aria-expanded={menuOpen}
            aria-controls="nav-drawer"
            aria-label="Toggle navigation"
            onClick={() => setMenuOpen((prev) => !prev)}
          >
            <span />
            <span />
          </button>
        </div>
        <div id="nav-drawer" className="nav-drawer">
          <a href="#login" onClick={() => setMenuOpen(false)}>Sign in</a>
          <a href="#link" onClick={() => setMenuOpen(false)}>Link watch</a>
          <a href="#vitals" onClick={() => setMenuOpen(false)}>Live vitals</a>
          <a className="btn btn-primary" href="#login" onClick={() => setMenuOpen(false)}>
            Sign in
          </a>
        </div>
      </header>

      <main>
        <section className="section simple-hero" id="login">
          <div className="container">
            <div className="section-head">
              <p className="eyebrow">Access</p>
              <h1>Remote Care Console</h1>
              <p>Sign in as a patient, doctor, or family member to access live vitals.</p>
            </div>
            <div className="role-grid">
              {roles.map((role) => (
                <button
                  type="button"
                  key={role.id}
                  className={`role-card ${activeRole === role.id ? 'active' : ''}`}
                  onClick={() => setActiveRole(role.id)}
                >
                  <strong>{role.title}</strong>
                  <span>{role.description}</span>
                </button>
              ))}
            </div>

            <div className="live-panel">
              <div className="live-auth-head">
                <div>
                  <strong>Sign in</strong>
                  <p className="muted">Selected role: {activeRoleLabel}</p>
                </div>
                <span className={`live-auth-status status-${authStatus}`}>{authStatusLabel}</span>
              </div>
              <form className="live-form" onSubmit={(event) => {
                event.preventDefault()
                loginToApi()
              }}>
                <label>
                  Work email
                  <input
                    type="email"
                    value={loginEmail}
                    onChange={(event) => setLoginEmail(event.target.value)}
                    placeholder="jane@clinic.com"
                    autoComplete="email"
                    required
                  />
                </label>
                <label>
                  Password
                  <input
                    type="password"
                    value={loginPassword}
                    onChange={(event) => setLoginPassword(event.target.value)}
                    placeholder="Enter password"
                    autoComplete="current-password"
                    required
                  />
                </label>
                <label>
                  Device info (optional)
                  <input
                    type="text"
                    value={loginDeviceInfo}
                    onChange={(event) => setLoginDeviceInfo(event.target.value)}
                    placeholder="web-app"
                    autoComplete="off"
                  />
                </label>
                <label className="live-checkbox">
                  <input
                    type="checkbox"
                    checked={autoConnectOnLogin}
                    onChange={(event) => setAutoConnectOnLogin(event.target.checked)}
                  />
                  Auto-connect to vitals after sign in
                </label>
                <div className="live-actions">
                  <button className="btn btn-primary" type="submit" disabled={authStatus === 'loading'}>
                    {authStatus === 'loading' ? 'Signing in...' : 'Sign in'}
                  </button>
                  <button className="btn btn-outline" type="button" onClick={signOut} disabled={!isSignedIn}>
                    Sign out
                  </button>
                </div>
              </form>
              {authError ? <p className="live-error">{authError}</p> : null}
              {isSignedIn ? (
                <div className="live-auth-meta">
                  <span>Signed in as {authProfile?.fullName || authProfile?.email || 'user'}</span>
                  <span>Role: {authProfile?.role || 'n/a'}</span>
                  <span>Token expires: {formatTimestamp(authExpiresAt)}</span>
                </div>
              ) : null}
            </div>
          </div>
        </section>

        <section className="section alt" id="link">
          <div className="container link-grid">
            <div>
              <p className="eyebrow">Link watch</p>
              <h2>Pair the patient watch to the account.</h2>
              <ol className="link-steps">
                <li>Open the watch app and enter the patient ID shown below.</li>
                <li>Set the MQTT broker host/port used by the backend.</li>
                <li>Tap Start on the watch to begin publishing vitals.</li>
              </ol>
              <p className="muted">Patient ID must be 6 characters (A-Z, 0-9).</p>
              <p className="muted">Doctors and family members can subscribe using the same patient ID.</p>
            </div>
            <div className="live-panel">
              <div className="live-form">
                <label>
                  Patient ID
                  <input
                    type="text"
                    value={patientIdInput}
                    onChange={(event) => setPatientIdInput(event.target.value)}
                    placeholder="ABC123"
                    autoComplete="off"
                    minLength={6}
                    maxLength={6}
                    pattern="[A-Za-z0-9]{6}"
                  />
                </label>
                <label>
                  MQTT broker host
                  <input
                    type="text"
                    value={mqttHost}
                    onChange={(event) => setMqttHost(event.target.value)}
                    placeholder="broker.example.com"
                    autoComplete="off"
                  />
                </label>
                <label>
                  MQTT port
                  <input
                    type="text"
                    value={mqttPort}
                    onChange={(event) => setMqttPort(event.target.value)}
                    placeholder="1883"
                    autoComplete="off"
                  />
                </label>
                <div className="live-token">
                  <span>Share this patient ID with the doctor and family.</span>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section className="section live" id="vitals">
          <div className="container live-grid">
            <div className="live-copy reveal" style={{ '--d': '0ms' }}>
              <p className="eyebrow">Live vitals</p>
              <h2>Stream vitals to doctors and family.</h2>
              <p>Enter the patient ID and connect to view live readings.</p>
              <div className={`live-status status-${connectionStatus}`}>
                <span className={`status-dot ${connectionStatus}`} aria-hidden="true" />
                <div>
                  <strong>{statusLabel}</strong>
                  <span className="live-status-note">
                    {isConnected ? 'Streaming vitals in real time.' : 'Awaiting a live connection.'}
                  </span>
                </div>
              </div>
              {connectionError ? <p className="live-error">{connectionError}</p> : null}
              {lastAlert ? (
                <div className="live-alert">
                  <strong>Alert</strong>
                  <span>{typeof lastAlert === 'string' ? lastAlert : JSON.stringify(lastAlert)}</span>
                </div>
              ) : null}
              <div className="live-meta">
                <span>Patient: {compactId(displayPatientId)}</span>
                <span>Device: {compactId(vitals?.deviceId)}</span>
                <span>Last update: {formatTimestamp(vitals?.recordedAt)}</span>
              </div>
            </div>
            <div className="live-panel reveal" style={{ '--d': '120ms' }}>
              <form className="live-form" onSubmit={(event) => {
                event.preventDefault()
                connectToVitalsHub()
              }}>
                <label>
                  API base URL
                  <input
                    type="text"
                    value={apiBaseUrl}
                    onChange={(event) => setApiBaseUrl(event.target.value)}
                    placeholder="http://localhost:5000"
                    autoComplete="off"
                    required
                  />
                </label>
                <label>
                  Patient ID
                  <input
                    type="text"
                    value={patientIdInput}
                    onChange={(event) => setPatientIdInput(event.target.value)}
                    placeholder="ABC123"
                    autoComplete="off"
                    required
                    minLength={6}
                    maxLength={6}
                    pattern="[A-Za-z0-9]{6}"
                  />
                </label>
                <div className="live-token">
                  <span>Token status: {isSignedIn ? 'Ready' : 'Sign in required'}</span>
                  {isSignedIn ? <span>Signed in as {authProfile?.email || 'user'}</span> : null}
                </div>
                <div className="live-actions">
                  <button
                    className="btn btn-primary"
                    type="submit"
                    disabled={connectionStatus === 'connecting' || !isSignedIn}
                  >
                    {connectionStatus === 'connecting' ? 'Connecting...' : 'Connect'}
                  </button>
                  <button
                    className="btn btn-ghost"
                    type="button"
                    onClick={disconnectFromVitalsHub}
                    disabled={!connectionRef.current}
                  >
                    Disconnect
                  </button>
                </div>
              </form>

              <div className="live-metrics">
                <div className="live-metric">
                  <span>Heart rate</span>
                  <strong>{formatMetric(vitals?.heartRateBpm, 'bpm')}</strong>
                  <small>Latest reading</small>
                </div>
                <div className="live-metric">
                  <span>SpO2</span>
                  <strong>{formatMetric(vitals?.spO2Percent, '%')}</strong>
                  <small>Oxygen saturation</small>
                </div>
                <div className="live-metric">
                  <span>Blood pressure</span>
                  <strong>{bpValue}</strong>
                  <small>Sys / Dia</small>
                </div>
                <div className="live-metric">
                  <span>Temperature</span>
                  <strong>{formatMetric(vitals?.temperatureC, 'C')}</strong>
                  <small>Body temp</small>
                </div>
                <div className="live-metric">
                  <span>Steps</span>
                  <strong>{formatMetric(vitals?.stepsCount, '')}</strong>
                  <small>Today</small>
                </div>
                <div className="live-metric">
                  <span>Wear status</span>
                  <strong>{wearingValue}</strong>
                  <small>Fall: {fallValue}</small>
                </div>
              </div>

              <div className="live-log">
                <div className="live-log-head">
                  <strong>Recent readings</strong>
                  <span>{vitalsHistory.length} events</span>
                </div>
                {vitalsHistory.length ? (
                  vitalsHistory.map((entry, index) => (
                    <div className="live-log-item" key={`${entry.time}-${index}`}>
                      <span>{entry.time}</span>
                      <span>{entry.heartRateBpm} bpm</span>
                      <span>SpO2 {entry.spO2Percent === '--' ? '--' : `${entry.spO2Percent}%`}</span>
                      <span>{entry.temperatureC === '--' ? '--' : `${entry.temperatureC} C`}</span>
                    </div>
                  ))
                ) : (
                  <p className="live-empty">No readings yet. Start the watch service and connect.</p>
                )}
              </div>
            </div>
          </div>
        </section>
      </main>
    </div>
  )
}

export default App
