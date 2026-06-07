import { useEffect, useState, useRef, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { HubConnectionBuilder, LogLevel } from '@microsoft/signalr'
import { resolveConnectPatientId } from '../utils/patientId'

const DEFAULT_API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000'

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
    const patientId = await resolveConnectPatientId(patientIdInput, authProfile, {
      apiBase: DEFAULT_API_BASE,
      accessToken,
    })
    if (!patientId) {
      setConnectionError('Enter a 6-character watch code or patient GUID.')
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
        skinTemperatureC: payload.skinTemperatureC ?? payload.temperatureC ?? null,
        temperatureC: payload.temperatureC ?? null,
        hrvMs: payload.hrvMs ?? null,
        stressScore: payload.stressScore ?? null,
        stepsCount: payload.stepsCount ?? null,
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
      const client = await import('../api/client')
      const response = await client.default.apiFetch('/api/admin/users')
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
            <p>
              Enter the same 6-character Patient ID shown on the watch (default <strong>ABC123</strong>),
              then click Connect while monitoring is running on the watch.
            </p>
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
                    <span className="vital-label">Skin temp.</span>
                    <span className="vital-value">{vitals.skinTemperatureC ?? vitals.temperatureC ?? '--'}°C</span>
                  </div>
                  <div className="vital-item">
                    <span className="vital-label">HRV</span>
                    <span className="vital-value">{vitals.hrvMs ?? '--'} ms</span>
                  </div>
                  <div className="vital-item">
                    <span className="vital-label">Stress</span>
                    <span className="vital-value">{vitals.stressScore ?? '--'} / 100</span>
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
                  <th>SpO₂</th>
                  <th>Skin °C</th>
                  <th>HRV</th>
                  <th>Steps</th>
                </tr>
              </thead>
              <tbody>
                {vitalsHistory.map((record, idx) => (
                  <tr key={idx}>
                    <td>{record.time}</td>
                    <td>{record.heartRateBpm ?? '--'} bpm</td>
                    <td>{record.spO2Percent ?? '--'}%</td>
                    <td>{record.skinTemperatureC ?? record.temperatureC ?? '--'}°C</td>
                    <td>{record.hrvMs ?? '--'} ms</td>
                    <td>{record.stepsCount ?? '--'}</td>
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
    const patientId = await resolveConnectPatientId(patientIdInput, authProfile, {
      apiBase: DEFAULT_API_BASE,
      accessToken,
    })
    if (!patientId) {
      setConnectionError('Enter a 6-character watch code or patient GUID.')
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
