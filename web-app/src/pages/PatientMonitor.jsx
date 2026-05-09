import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { HubConnectionBuilder, LogLevel } from '@microsoft/signalr'

const DEFAULT_API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000'
const shortIdPattern = /^[A-Za-z0-9]{6}$/
const guidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

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

  for (let g = 0; g < x.length; g += 16) {
    const aa = a
    const bb = b
    const cc = c
    const dd = d
    for (let i = 0; i < 64; i += 1) {
      let f
      let temp
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
      const sum = (a + f + k[i] + (x[g + temp] || 0)) >>> 0
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

export default function PatientMonitor({ authProfile, accessToken, onLogout }) {
  const navigate = useNavigate()
  const [patientIdInput, setPatientIdInput] = useState('')
  const [connectionStatus, setConnectionStatus] = useState('disconnected')
  const [connectionError, setConnectionError] = useState('')
  const [latestVitals, setLatestVitals] = useState(null)
  const [timeline, setTimeline] = useState([])
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
    onLogout?.()
    navigate('/login')
  }

  const connect = async () => {
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

      const vitals = {
        heartRateBpm: payload.heartRateBpm ?? null,
        spO2Percent: payload.spO2Percent ?? null,
        systolicBp: payload.systolicBp ?? null,
        diastolicBp: payload.diastolicBp ?? null,
        temperatureC: payload.temperatureC ?? null,
        stepsCount: payload.stepsCount ?? null,
        caloriesBurned: payload.caloriesBurned ?? null,
        fallDetected: Boolean(payload.fallDetected),
        isWearing: payload.isWearing !== false,
        recordedAt: payload.recordedAt ?? new Date().toISOString(),
      }

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
