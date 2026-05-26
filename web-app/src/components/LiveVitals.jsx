import { useEffect, useRef, useState } from 'react'
import { buildVitalsHubConnection, startVitalsHub } from '../utils/signalr'
import { fetchLatestVitals, mapVitalsPayload } from '../utils/vitals'
import VitalsGrid from './VitalsGrid.jsx'

const statusLabel = {
  idle: 'Not connected',
  connecting: 'Connecting…',
  connected: 'Live',
  error: 'Connection error',
}

export default function LiveVitals({ patientId, patientName }) {
  const [status, setStatus] = useState('idle')
  const [error, setError] = useState('')
  const [latest, setLatest] = useState(null)
  const [updatedAt, setUpdatedAt] = useState(null)
  const connectionRef = useRef(null)
  const mountedRef = useRef(true)

  useEffect(() => () => { mountedRef.current = false }, [])

  useEffect(() => {
    if (!patientId) return undefined
    let cancelled = false
    setStatus('connecting')
    setError('')

    const initialise = async () => {
      try {
        const initial = await fetchLatestVitals(patientId)
        if (!cancelled && initial) {
          setLatest(initial)
          setUpdatedAt(new Date(initial.recordedAt))
        }
      } catch (err) {
        if (!cancelled) setError(err.message || 'Could not load latest vitals.')
      }

      const connection = buildVitalsHubConnection({
        onVitals: (payload) => {
          if (!mountedRef.current) return
          const mapped = mapVitalsPayload(payload)
          if (mapped) {
            setLatest(mapped)
            setUpdatedAt(new Date(mapped.recordedAt))
          }
        },
      })
      connectionRef.current = connection

      try {
        await startVitalsHub(connection, patientId)
        if (!cancelled) setStatus('connected')
      } catch (err) {
        if (cancelled) return
        setStatus('error')
        setError(err.message || 'SignalR failed.')
      }
    }

    initialise()

    return () => {
      cancelled = true
      const connection = connectionRef.current
      connectionRef.current = null
      if (connection) {
        connection.stop().catch(() => {})
      }
    }
  }, [patientId])

  return (
    <section className="card">
      <div className="card-head">
        <div>
          <h2>Live vitals{patientName ? ` · ${patientName}` : ''}</h2>
          <span className="muted">
            {updatedAt ? `Last update ${updatedAt.toLocaleTimeString()}` : 'Awaiting data from the watch…'}
          </span>
        </div>
        <div className="connection-bar">
          <span className={`connection-dot ${status === 'connected' ? 'connected' : status === 'connecting' ? 'connecting' : status === 'error' ? 'error' : ''}`} />
          {statusLabel[status]}
        </div>
      </div>
      {error && <div className="form-error">{error}</div>}
      <VitalsGrid vitals={latest} />
    </section>
  )
}
