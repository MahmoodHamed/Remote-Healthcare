import { useEffect, useState, useRef, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { HubConnectionBuilder, LogLevel } from '@microsoft/signalr'

const DEFAULT_API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000'
const shortIdPattern = /^[A-Za-z0-9]{6}$/
const guidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

// Simple MD5 hash implementation
const md5Bytes = (str) => {
  const x = []
  const k = [
    0xd76aa478, 0xe8c7b756, 0x242070db, 0xc1bdceee,
    0xf57c0faf, 0x4787c62a, 0xa8304613, 0xfd469501,
    0x698098d8, 0x8b44f7af, 0xffff5bb1, 0x895cd7be,
    0x6b901122, 0xfd987193, 0xa679438e, 0x49b40821,
    0xf61e2562, 0xc040b340, 0x265e5a51, 0xe9b6c7aa,
    0xd62f105d, 0x02441453, 0xd8a1e681, 0xe7d3fbc8,
    0x21e1cde6, 0xc33707d6, 0xf4d50d87, 0x455a14ed,
    0xa9e3e905, 0xfcefa3f8, 0x676f02d9, 0x8d2a4c8a,
    0xfffa3942, 0x8771f681, 0x6d9d6122, 0xfde5380c,
    0xa4beea44, 0x4bdecfa9, 0xf6bb4b60, 0xbebfbc70,
    0x289b7ec6, 0xeaa127fa, 0xd4ef3085, 0x04881d05,
    0xd9d4d039, 0xe6db99e5, 0x1fa27cf8, 0xc4ac5665,
    0xf4292244, 0x432aff97, 0xab9423a7, 0xfc93a039,
    0x655b59c3, 0x8f0ccc92, 0xffeff47d, 0x85845dd1,
    0x6fa87e4f, 0xfe2ce6e0, 0xa3014314, 0x4e0811a1,
    0xf7537e82, 0xbd3af235, 0x2ad7d2bb, 0xeb86d391,
  ]
  const r = [
    7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
    5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
    4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
    6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21,
  ]
  
  for (let i = 0; i < str.length; i += 1) {
    x[i >> 2] |= (str.charCodeAt(i) & 0xff) << ((i % 4) * 8)
  }
  x[(str.length >> 2)] |= 0x80 << (((str.length) % 4) * 8)
  x[(((str.length + 8) >> 6) << 4) + 14] = str.length * 8

  let a = 0x67452301
  let b = 0xefcdab89
  let c = 0x98badcfe
  let d = 0x10325476
  const chunk = x
  for (let g = 0; g < chunk.length; g += 16) {
    const aa = a
    const bb = b
    const cc = c
    const dd = d
    for (let i = 0; i < 64; i += 1) {
      let f, temp
      if (i < 16) {
        f = (bb & cc) | (~bb & dd)
        temp = i
      } else if (i < 32) {
        f = (dd & bb) | (~dd & cc)
        temp = (5 * i + 1) % 16
      } else if (i < 48) {
        f = bb ^ cc ^ dd
        temp = (3 * i + 5) % 16
      } else {
        f = cc ^ (bb | ~dd)
        temp = (7 * i) % 16
      }
      const temp2 = chunk[g + temp]
      const sum = (a + f + k[i] + temp2) >>> 0
      a = (d + ((sum << r[i]) | (sum >>> (32 - r[i])))) >>> 0
      d = c
      c = b
      b = (b + a) >>> 0
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

export default function Dashboard({ authProfile, accessToken, onLogout }) {
  const navigate = useNavigate()
  const [patientIdInput, setPatientIdInput] = useState('')
  const [connectionStatus, setConnectionStatus] = useState('disconnected')
  const [vitals, setVitals] = useState(null)
  const [vitalsHistory, setVitalsHistory] = useState([])
  const [connectionError, setConnectionError] = useState('')
  const [adminUsers, setAdminUsers] = useState([])
  const [adminLoading, setAdminLoading] = useState(false)
  const [adminError, setAdminError] = useState('')
  const connectionRef = useRef(null)

  const isAdmin = authProfile?.role === 'Admin'

  useEffect(() => {
    if (!accessToken || !authProfile) {
      navigate('/login')
    }
  }, [accessToken, authProfile, navigate])

  const handleLogout = () => {
    if (connectionRef.current) {
      connectionRef.current.stop().catch(() => {})
      connectionRef.current = null
    }
    setVitals(null)
    setVitalsHistory([])
    setConnectionError('')
    if (onLogout) {
      onLogout()
    }
    navigate('/login')
  }

  const connectToVitals = async () => {
    const patientId = normalizePatientId(patientIdInput)
    if (!patientId) {
      setConnectionError('Patient ID must be 6 characters (A-Z, 0-9).')
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
      if (!payload || typeof payload !== 'object') return

      const normalized = {
        heartRateBpm: payload.heartRateBpm ?? null,
        spO2Percent: payload.spO2Percent ?? null,
        systolicBp: payload.systolicBp ?? null,
        diastolicBp: payload.diastolicBp ?? null,
        temperatureC: payload.temperatureC ?? null,
        recordedAt: payload.recordedAt ?? null,
      }

      setVitals(normalized)
      const time = normalized.recordedAt ? new Date(normalized.recordedAt).toLocaleTimeString() : 'n/a'
      setVitalsHistory((prev) => [{ time, ...normalized }, ...prev].slice(0, 12))
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
      setConnectionError(err?.message || 'Failed to connect to vitals.')
      await connection.stop().catch(() => {})
      connectionRef.current = null
    }
  }

  const fetchAdminUsers = useCallback(async () => {
    if (!isAdmin) return
    setAdminLoading(true)
    try {
      const response = await fetch(new URL('/api/admin/users', DEFAULT_API_BASE).toString(), {
        headers: { Authorization: `Bearer ${accessToken}` },
      })
      if (!response.ok) throw new Error(`Failed to fetch users (${response.status}).`)
      const data = await response.json()
      setAdminUsers(Array.isArray(data) ? data : [])
    } catch (err) {
      setAdminError(err?.message || 'Failed to fetch admin users.')
    } finally {
      setAdminLoading(false)
    }
  }, [isAdmin, accessToken])

  useEffect(() => {
    if (isAdmin) {
      fetchAdminUsers()
    }
  }, [isAdmin, fetchAdminUsers])

  return (
    <main>
      <section className="section">
        <div className="container">
          <div className="dashboard-header">
            <div>
              <p className="eyebrow">Dashboard</p>
              <h2>Welcome back, {authProfile?.fullName || 'user'}!</h2>
              <p className="muted">Role: {authProfile?.role}</p>
            </div>
            <button className="btn btn-outline" onClick={handleLogout}>
              Sign out
            </button>
          </div>
        </div>
      </section>

      <section className="section live">
        <div className="container live-grid">
          <div className="live-copy">
            <p className="eyebrow">Live vitals</p>
            <h2>Stream vitals from your device</h2>
            <p>Enter your patient ID and connect to view real-time readings.</p>
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
                  required
                />
              </label>
              <button className="btn btn-primary" type="button" onClick={connectToVitals}>
                {connectionStatus === 'connected' ? 'Disconnect' : 'Connect'}
              </button>

              {vitals && (
                <div className="vitals-display">
                  <div className="vital-item">
                    <span className="vital-label">Heart Rate</span>
                    <span className="vital-value">{vitals.heartRateBpm ?? '--'} bpm</span>
                  </div>
                  <div className="vital-item">
                    <span className="vital-label">SpO2</span>
                    <span className="vital-value">{vitals.spO2Percent ?? '--'}%</span>
                  </div>
                  <div className="vital-item">
                    <span className="vital-label">Temp</span>
                    <span className="vital-value">{vitals.temperatureC ?? '--'}°C</span>
                  </div>
                  <div className="vital-item">
                    <span className="vital-label">BP</span>
                    <span className="vital-value">
                      {vitals.systolicBp ?? '--'}/{vitals.diastolicBp ?? '--'} mmHg
                    </span>
                  </div>
                </div>
              )}

              {connectionError && <p className="live-error">{connectionError}</p>}
            </div>
          </div>
        </div>
      </section>

      {vitalsHistory.length > 0 && (
        <section className="section alt">
          <div className="container">
            <h2>Vitals History</h2>
            <table className="vitals-table">
              <thead>
                <tr>
                  <th>Time</th>
                  <th>Heart Rate</th>
                  <th>SpO2</th>
                  <th>Temperature</th>
                  <th>BP</th>
                </tr>
              </thead>
              <tbody>
                {vitalsHistory.map((record, idx) => (
                  <tr key={idx}>
                    <td>{record.time}</td>
                    <td>{record.heartRateBpm ?? '--'} bpm</td>
                    <td>{record.spO2Percent ?? '--'}%</td>
                    <td>{record.temperatureC ?? '--'}°C</td>
                    <td>
                      {record.systolicBp ?? '--'}/{record.diastolicBp ?? '--'} mmHg
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {isAdmin && (
        <section className="section" id="admin">
          <div className="container">
            <div className="section-head">
              <p className="eyebrow">Admin</p>
              <h2>User management</h2>
              <p>View and manage all users in the system.</p>
            </div>

            {adminError && <p className="live-error">{adminError}</p>}
            {adminLoading && <p>Loading users...</p>}

            {adminUsers.length > 0 && (
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Phone</th>
                    <th>Role</th>
                  </tr>
                </thead>
                <tbody>
                  {adminUsers.map((user) => (
                    <tr key={user.id}>
                      <td>{user.fullName}</td>
                      <td>{user.email}</td>
                      <td>{user.phone}</td>
                      <td>{user.role}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </section>
      )}
    </main>
  )
}

export function HeartRateMonitor({ authProfile, accessToken, onLogout }) {
  const navigate = useNavigate()
  const [patientIdInput, setPatientIdInput] = useState('')
  const [connectionStatus, setConnectionStatus] = useState('disconnected')
  const [heartRate, setHeartRate] = useState(null)
  const [history, setHistory] = useState([])
  const [connectionError, setConnectionError] = useState('')
  const connectionRef = useRef(null)

  useEffect(() => {
    if (!accessToken || !authProfile) {
      navigate('/login')
    }
  }, [accessToken, authProfile, navigate])

  const handleLogout = () => {
    if (connectionRef.current) {
      connectionRef.current.stop().catch(() => {})
      connectionRef.current = null
    }
    if (onLogout) onLogout()
    navigate('/login')
  }

  const connectToVitals = async () => {
    const patientId = normalizePatientId(patientIdInput)
    if (!patientId) {
      setConnectionError('Patient ID must be 6 characters (A-Z, 0-9).')
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
      const bpm = payload?.heartRateBpm ?? null
      setHeartRate(bpm)
      setHistory((prev) => [{
        time: new Date().toLocaleTimeString(),
        heartRateBpm: bpm,
      }, ...prev].slice(0, 12))
    })

    connection.onclose(() => setConnectionStatus('disconnected'))
    connectionRef.current = connection

    try {
      await connection.start()
      await connection.invoke('SubscribeToPatient', patientId)
      setConnectionStatus('connected')
    } catch (err) {
      setConnectionStatus('error')
      setConnectionError(err?.message || 'Failed to connect to vitals.')
      await connection.stop().catch(() => {})
      connectionRef.current = null
    }
  }

  return (
    <main>
      <section className="section">
        <div className="container">
          <div className="dashboard-header">
            <div>
              <p className="eyebrow">Heart Rate Monitor</p>
              <h2>Track pulse in real time</h2>
              <p className="muted">Focused view for heart rate monitoring only.</p>
            </div>
            <button className="btn btn-outline" onClick={handleLogout}>Sign out</button>
          </div>
        </div>
      </section>

      <section className="section live">
        <div className="container live-grid">
          <div className="live-copy">
            <p className="eyebrow">Heart rate</p>
            <h2>Live BPM monitoring</h2>
            <p>Connect a patient ID and watch the latest pulse update in real time.</p>
            <div className={`live-status status-${connectionStatus}`}>
              <span className={`status-dot ${connectionStatus}`} aria-hidden="true" />
              <div>
                <strong>{connectionStatus === 'connected' ? 'Connected' : connectionStatus === 'connecting' ? 'Connecting' : 'Disconnected'}</strong>
                <span className="live-status-note">{connectionStatus === 'connected' ? 'Receiving live pulse readings.' : 'Awaiting connection.'}</span>
              </div>
            </div>
          </div>

          <div className="live-panel">
            <div className="live-form">
              <label>
                Patient ID
                <input type="text" value={patientIdInput} onChange={(e) => setPatientIdInput(e.target.value)} placeholder="ABC123" maxLength="6" required />
              </label>
              <button className="btn btn-primary" type="button" onClick={connectToVitals}>
                Monitor heart rate
              </button>

              <div className="heart-rate-display">
                <span className="heart-rate-label">Current BPM</span>
                <strong className="heart-rate-value">{heartRate ?? '--'}</strong>
              </div>

              {connectionError ? <p className="live-error">{connectionError}</p> : null}
            </div>
          </div>
        </div>
      </section>

      {history.length > 0 && (
        <section className="section alt">
          <div className="container">
            <h2>Recent readings</h2>
            <table className="vitals-table">
              <thead>
                <tr>
                  <th>Time</th>
                  <th>Heart Rate</th>
                </tr>
              </thead>
              <tbody>
                {history.map((row, index) => (
                  <tr key={index}>
                    <td>{row.time}</td>
                    <td>{row.heartRateBpm ?? '--'} bpm</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </main>
  )
}
