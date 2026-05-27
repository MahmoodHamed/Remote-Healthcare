import { useEffect, useState } from 'react'
import { readProfile } from '../../utils/auth'
import { getMqttHost, getMqttPort } from '../../utils/apiBase'
import { normalizePatientId } from '../../utils/patientId'
import {
  WATCH_HOST_KEY,
  WATCH_PORT_KEY,
  WATCH_SHORT_ID_KEY,
  getWatchShortId,
  isValidWatchShortId,
} from '../../utils/watchPairing'

export default function PatientWatch() {
  const profile = readProfile()
  const [shortId, setShortId] = useState('')
  const [host, setHost] = useState('')
  const [port, setPort] = useState('')
  const [feedback, setFeedback] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    setShortId(getWatchShortId())
    setHost(localStorage.getItem(WATCH_HOST_KEY) ?? getMqttHost())
    setPort(localStorage.getItem(WATCH_PORT_KEY) ?? getMqttPort())
  }, [])

  const streamingId = shortId && isValidWatchShortId(shortId)
    ? normalizePatientId(shortId)
    : profile?.id ?? ''

  const save = (event) => {
    event.preventDefault()
    const trimmed = shortId.trim().toUpperCase()
    if (!isValidWatchShortId(trimmed)) {
      setError('Enter exactly 6 letters or digits (for example ABC123).')
      setFeedback('')
      return
    }
    localStorage.setItem(WATCH_SHORT_ID_KEY, trimmed)
    localStorage.setItem(WATCH_HOST_KEY, host.trim())
    localStorage.setItem(WATCH_PORT_KEY, port.trim())
    setError('')
    setFeedback(`Saved. Enter ${trimmed} on your watch Setup screen, then press Start.`)
  }

  return (
    <>
      <section className="card">
        <div className="card-head">
          <div>
            <h2>Pair your Galaxy Watch 8</h2>
            <span className="muted">Three small steps to start streaming every sensor to your doctor.</span>
          </div>
          <span className="tag patient">Patient</span>
        </div>

        <div className="feature-grid">
          <article className="feature-card">
            <span className="feature-icon">1</span>
            <h3>Install the watch app</h3>
            <p>Side-load the <strong>Remote Care</strong> Wear OS APK on the watch (provided by your hospital).</p>
          </article>
          <article className="feature-card">
            <span className="feature-icon">2</span>
            <h3>Enter your patient ID</h3>
            <p>Open the app on the watch and type the same 6-character ID shown below.</p>
          </article>
          <article className="feature-card">
            <span className="feature-icon">3</span>
            <h3>Press start</h3>
            <p>The watch will stream HR, SpO₂, ECG, skin temperature, stress, sleep and more to this dashboard.</p>
          </article>
        </div>
      </section>

      <section className="card">
        <div className="card-head">
          <div>
            <h3>Pairing details</h3>
            <span className="muted">Save here first, then enter the same values on the watch.</span>
          </div>
        </div>

        <form className="form" onSubmit={save}>
          <div className="form-row">
            <label>
              Patient short ID
              <input
                value={shortId}
                onChange={(e) => setShortId(e.target.value.toUpperCase())}
                maxLength={6}
                placeholder="ABC123"
                style={{ fontFamily: 'monospace', letterSpacing: '0.18em' }}
              />
              <span className="form-hint">
                Must match the watch exactly. Your dashboard listens for this code after you save.
              </span>
            </label>
            <label>
              Streaming patient ID
              <input value={streamingId} readOnly style={{ fontFamily: 'monospace' }} />
              <span className="form-hint">Internal ID used by the server after your short code is normalized.</span>
            </label>
          </div>
          <div className="form-row">
            <label>
              MQTT host
              <input value={host} onChange={(e) => setHost(e.target.value)} placeholder="remote-care.tech" />
            </label>
            <label>
              MQTT port
              <input value={port} onChange={(e) => setPort(e.target.value)} placeholder="1883" />
            </label>
          </div>
          {error && <div className="form-error">{error}</div>}
          {feedback && <div className="form-success">{feedback}</div>}
          <button className="btn btn-primary" style={{ alignSelf: 'flex-start' }}>Save pairing details</button>
        </form>
      </section>
    </>
  )
}
