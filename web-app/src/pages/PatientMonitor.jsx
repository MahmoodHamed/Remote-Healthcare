import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { normalizePatientId } from '../utils/patientId'
import { clearAuthSession, getAccessToken, isTokenExpired } from '../utils/authSession'
import { buildVitalsHubConnection, startVitalsHub } from '../utils/signalr'
import { fetchLatestVitals, mapVitalsPayload } from '../utils/vitals'

export default function PatientMonitor({ authProfile, accessToken, onLogout }) {
  const navigate = useNavigate()
  const [patientIdInput, setPatientIdInput] = useState(() => localStorage.getItem('rpmPatientId') || '')
  const [connectionStatus, setConnectionStatus] = useState('disconnected')
  const [connectionError, setConnectionError] = useState('')
  const [latestVitals, setLatestVitals] = useState(null)
  const [timeline, setTimeline] = useState([])
  const connectionRef = useRef(null)
  const pollRef = useRef(null)
  const activePatientRef = useRef('')

  const applyVitals = (vitals) => {
    if (!vitals) return
    setLatestVitals(vitals)
    const stamp = new Date(vitals.recordedAt).toLocaleTimeString()
    setTimeline((prev) => {
      if (prev[0]?.recordedAt === vitals.recordedAt) return prev
      return [{ stamp, ...vitals }, ...prev].slice(0, 12)
    })
  }

  const loadLatestVitals = async (patientId) => {
    const token = getAccessToken() || accessToken
    if (!token) return
    try {
      const vitals = await fetchLatestVitals(patientId, token)
      applyVitals(vitals)
    } catch (err) {
      if (err?.status === 401) handleUnauthorized()
    }
  }

  useEffect(() => {
    const token = getAccessToken() || accessToken
    if (!token || isTokenExpired(token) || !authProfile) {
      clearAuthSession()
      onLogout?.()
      navigate('/login', { replace: true })
    }
  }, [accessToken, authProfile, navigate, onLogout])

  const handleUnauthorized = () => {
    clearAuthSession()
    onLogout?.()
    setConnectionError('Session expired. Please sign in again.')
    navigate('/login', { replace: true })
  }

  useEffect(() => () => {
    if (pollRef.current) clearInterval(pollRef.current)
    if (connectionRef.current) connectionRef.current.stop().catch(() => {})
  }, [])

  const handleLogout = () => {
    if (pollRef.current) clearInterval(pollRef.current)
    pollRef.current = null
    if (connectionRef.current) {
      connectionRef.current.stop().catch(() => {})
      connectionRef.current = null
    }
    onLogout?.()
    navigate('/login')
  }

  const connect = async () => {
    const patientId = normalizePatientId(patientIdInput)
    if (!patientId) {
      setConnectionError('Patient ID must be 6 characters (A-Z, 0-9).')
      return
    }

    setConnectionStatus('connecting')
    setConnectionError('')

    if (connectionRef.current) {
      await connectionRef.current.stop().catch(() => {})
      connectionRef.current = null
    }

    const shortPatientId = patientIdInput.trim().toUpperCase()
    activePatientRef.current = shortPatientId
    localStorage.setItem('rpmPatientId', shortPatientId)

    if (pollRef.current) clearInterval(pollRef.current)
    await loadLatestVitals(shortPatientId)
    pollRef.current = setInterval(() => {
      loadLatestVitals(activePatientRef.current)
    }, 5000)

    const connection = buildVitalsHubConnection({
      onVitals: (payload) => applyVitals(mapVitalsPayload(payload)),
    })

    connection.onreconnecting(() => setConnectionStatus('connecting'))
    connection.onreconnected(async () => {
      setConnectionStatus('connected')
      try {
        await connection.invoke('SubscribeToPatient', patientIdInput.trim().toUpperCase())
      } catch (err) {
        setConnectionError(err?.message || 'Failed to re-subscribe.')
      }
    })
    connection.onclose(() => setConnectionStatus('disconnected'))

    connectionRef.current = connection

    try {
      await startVitalsHub(connection, shortPatientId, { onUnauthorized: handleUnauthorized })
      setConnectionStatus('connected')
    } catch (err) {
      setConnectionStatus('error')
      setConnectionError(err?.message || 'Failed to connect to the patient monitor.')
      await connection.stop().catch(() => {})
      connectionRef.current = null
    }
  }

  const readings = [
    {
      label: 'Heart rate',
      value: latestVitals?.heartRateBpm ?? '--',
      unit: 'bpm',
      tone: 'accent',
    },
    {
      label: 'SpO2',
      value: latestVitals?.spO2Percent ?? '--',
      unit: '%',
      tone: 'teal',
    },
    {
      label: 'Blood pressure',
      value: latestVitals ? `${latestVitals.systolicBp ?? '--'}/${latestVitals.diastolicBp ?? '--'}` : '--',
      unit: 'mmHg',
      tone: 'warm',
    },
    {
      label: 'Temperature',
      value: latestVitals?.temperatureC ?? '--',
      unit: '°C',
      tone: 'amber',
    },
    {
      label: 'Steps',
      value: latestVitals?.stepsCount ?? '--',
      unit: 'steps',
      tone: 'ink',
    },
    {
      label: 'Calories',
      value: latestVitals?.caloriesBurned ?? '--',
      unit: 'kcal',
      tone: 'ink',
    },
    {
      label: 'Fall detected',
      value: latestVitals ? (latestVitals.fallDetected ? 'Yes' : 'No') : '--',
      unit: '',
      tone: latestVitals?.fallDetected ? 'danger' : 'teal',
    },
    {
      label: 'Wearing watch',
      value: latestVitals ? (latestVitals.isWearing ? 'Yes' : 'No') : '--',
      unit: '',
      tone: latestVitals?.isWearing ? 'teal' : 'danger',
    },
  ]

  return (
    <main>
      <section className="section">
        <div className="container">
          <div className="dashboard-header">
            <div>
              <p className="eyebrow">Patient Monitor</p>
              <h2>Samsung Watch 8 sensor dashboard</h2>
              <p className="muted">Monitor all available sensors in one live page.</p>
            </div>
            <div className="live-actions">
              <a className="btn btn-outline" href="/dashboard">Dashboard</a>
              <button className="btn btn-outline" onClick={handleLogout}>Sign out</button>
            </div>
          </div>
        </div>
      </section>

      <section className="section live">
        <div className="container live-grid">
          <div className="live-copy">
            <p className="eyebrow">Live sensor feed</p>
            <h2>All vitals from the watch</h2>
            <p>Connect a patient ID and receive heart rate, oxygen, blood pressure, temperature, movement, and safety alerts in real time.</p>
            <div className={`live-status status-${connectionStatus}`}>
              <span className={`status-dot ${connectionStatus}`} aria-hidden="true" />
              <div>
                <strong>
                  {connectionStatus === 'connected'
                    ? 'Connected'
                    : connectionStatus === 'connecting'
                      ? 'Connecting'
                      : connectionStatus === 'error'
                        ? 'Connection error'
                        : 'Disconnected'}
                </strong>
                <span className="live-status-note">
                  {connectionStatus === 'connected' ? 'Streaming live readings now.' : 'Awaiting patient subscription.'}
                </span>
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
                Start monitoring
              </button>
              <div className="sensor-grid">
                {readings.map((reading) => (
                  <div key={reading.label} className={`sensor-card tone-${reading.tone}`}>
                    <span className="sensor-label">{reading.label}</span>
                    <strong className="sensor-value">
                      {reading.value}
                      {reading.unit ? <small>{reading.unit}</small> : null}
                    </strong>
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
              <p>Most recent sensor updates received from the watch.</p>
            </div>
            <table className="vitals-table">
              <thead>
                <tr>
                  <th>Time</th>
                  <th>Heart Rate</th>
                  <th>SpO2</th>
                  <th>BP</th>
                  <th>Temp</th>
                  <th>Steps</th>
                  <th>Calories</th>
                  <th>Fall</th>
                  <th>Wearing</th>
                </tr>
              </thead>
              <tbody>
                {timeline.map((row, index) => (
                  <tr key={`${row.stamp}-${index}`}>
                    <td>{row.stamp}</td>
                    <td>{row.heartRateBpm ?? '--'} bpm</td>
                    <td>{row.spO2Percent ?? '--'}%</td>
                    <td>{row.systolicBp ?? '--'}/{row.diastolicBp ?? '--'} mmHg</td>
                    <td>{row.temperatureC ?? '--'}°C</td>
                    <td>{row.stepsCount ?? '--'}</td>
                    <td>{row.caloriesBurned ?? '--'}</td>
                    <td>{row.fallDetected ? 'Yes' : 'No'}</td>
                    <td>{row.isWearing ? 'Yes' : 'No'}</td>
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
