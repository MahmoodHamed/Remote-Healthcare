import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { buildVitalsHubConnection, startVitalsHub } from '../utils/signalr'
import { fetchLatestVitals, mergeVitalsPayload } from '../utils/vitals'
import VitalsGrid from './VitalsGrid.jsx'

const statusLabel = {
  idle: 'Not connected',
  connecting: 'Connecting…',
  connected: 'Live',
  waiting: 'Waiting for watch',
  error: 'Connection error',
}

const hasAnyReading = (vitals) => {
  if (!vitals || vitals.isWearing === false) return false
  return [
    vitals.heartRateBpm,
    vitals.spO2Percent,
    vitals.systolicBp,
    vitals.diastolicBp,
    vitals.temperatureC,
    vitals.skinTemperatureC,
    vitals.stepsCount,
    vitals.stressScore,
  ].some((v) => v != null && v !== '')
}

export default function LiveVitals({
  patientId,
  patientName,
  watchSetupHref = '/patient/watch',
  viewerRole = 'patient',
}) {
  const [status, setStatus] = useState('idle')
  const [error, setError] = useState('')
  const [latest, setLatest] = useState(null)
  const [updatedAt, setUpdatedAt] = useState(null)
  const connectionRef = useRef(null)
  const startTaskRef = useRef(null)
  const mountedRef = useRef(true)

  useEffect(() => () => { mountedRef.current = false }, [])

  useEffect(() => {
    if (!patientId) return undefined
    let cancelled = false
    setStatus('connecting')
    setError('')

    const initialise = async () => {
      let hadInitial = false
      try {
        const initial = await fetchLatestVitals(patientId)
        if (!cancelled && initial) {
          hadInitial = true
          setLatest(initial)
          setUpdatedAt(new Date(initial.recordedAt))
        }
      } catch (err) {
        if (!cancelled) setError(err.message || 'Could not load latest vitals.')
      }

      const connection = buildVitalsHubConnection({
        onVitals: (payload) => {
          if (!mountedRef.current) return
          setLatest((prev) => {
            const merged = mergeVitalsPayload(prev, payload)
            if (!merged) return prev
            setUpdatedAt(new Date())
            setStatus('connected')
            return merged
          })
        },
      })
      connectionRef.current = connection

      const startTask = (async () => {
        await startVitalsHub(connection, patientId)
      })()
      startTaskRef.current = startTask

      try {
        await startTask
        if (!cancelled) setStatus(hadInitial ? 'connected' : 'waiting')
      } catch (err) {
        if (cancelled) return
        if (err.message?.includes('before stop() was called')) return
        setStatus('error')
        setError(err.message || 'Live updates unavailable. Vitals will appear when your watch streams data.')
      }
    }

    initialise()

    return () => {
      cancelled = true
      const connection = connectionRef.current
      const startTask = startTaskRef.current
      connectionRef.current = null
      startTaskRef.current = null
      void (async () => {
        try {
          if (startTask) await startTask
        } catch {
          /* start may fail if unmounted mid-flight */
        }
        if (connection) {
          try {
            await connection.stop()
          } catch {
            /* ignore stop races */
          }
        }
      })()
    }
  }, [patientId])

  const showEmpty = !hasAnyReading(latest) && status !== 'connecting'

  return (
    <section className="card">
      <div className="card-head">
        <div>
          <h2>{viewerRole === 'doctor' ? 'Live monitoring' : 'Live vitals'}{patientName ? ` · ${patientName}` : ''}</h2>
          <span className="muted">
            {updatedAt
              ? `Last update ${updatedAt.toLocaleTimeString()}`
              : viewerRole === 'doctor'
                ? 'No readings yet — patient watch must be paired and streaming.'
                : 'No readings yet — pair your watch to start streaming.'}
          </span>
        </div>
        <div className="connection-bar">
          <span
            className={`connection-dot ${
              status === 'connected'
                ? 'connected'
                : status === 'connecting'
                  ? 'connecting'
                  : status === 'error'
                    ? 'error'
                    : status === 'waiting'
                      ? 'connecting'
                      : ''
            }`}
          />
          {statusLabel[status] ?? status}
        </div>
      </div>
      {error && <div className="form-error">{error}</div>}
      {latest?.isWearing === false && (
        <div className="form-hint" style={{ marginBottom: '0.75rem' }}>
          Watch is off-wrist — sensor readings are hidden until you wear it again.
        </div>
      )}
      {showEmpty && latest?.isWearing !== false && (
        <div className="vitals-empty">
          <h3>{viewerRole === 'doctor' ? 'Waiting for patient readings' : 'Waiting for your first reading'}</h3>
          <p>
            {viewerRole === 'doctor'
              ? 'This patient has no live vitals yet. Ask them to pair their Galaxy Watch and start monitoring — readings will appear here automatically.'
              : 'Your dashboard is ready. Install the watch app, enter your patient ID on the watch, and press start. Vitals will appear here automatically.'}
          </p>
          {viewerRole !== 'doctor' && watchSetupHref && (
            <Link to={watchSetupHref} className="btn btn-primary btn-sm">
              Set up my watch →
            </Link>
          )}
        </div>
      )}
      <VitalsGrid vitals={latest} />
    </section>
  )
}
