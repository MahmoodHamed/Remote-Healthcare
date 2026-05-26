import { useEffect, useState } from 'react'
import { readProfile } from '../../utils/auth'
import { getMqttHost, getMqttPort } from '../../utils/apiBase'

const SHORT_KEY = 'rpm-watch-shortid'
const HOST_KEY = 'rpm-watch-mqtt-host'
const PORT_KEY = 'rpm-watch-mqtt-port'

export default function PatientWatch() {
  const profile = readProfile()
  const [shortId, setShortId] = useState('')
  const [host, setHost] = useState('')
  const [port, setPort] = useState('')
  const [feedback, setFeedback] = useState('')

  useEffect(() => {
    setShortId(localStorage.getItem(SHORT_KEY) ?? '')
    setHost(localStorage.getItem(HOST_KEY) ?? getMqttHost())
    setPort(localStorage.getItem(PORT_KEY) ?? getMqttPort())
  }, [])

  const save = (event) => {
    event.preventDefault()
    localStorage.setItem(SHORT_KEY, shortId.trim().toUpperCase())
    localStorage.setItem(HOST_KEY, host.trim())
    localStorage.setItem(PORT_KEY, port.trim())
    setFeedback('Saved. Open the Wear OS app on your watch to apply these values.')
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
            <p>Open the app on the watch and paste the 6-character ID shown below.</p>
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
            <span className="muted">Use these values on the watch.</span>
          </div>
        </div>

        <form className="form" onSubmit={save}>
          <div className="form-row">
            <label>
              Patient short ID
              <input
                value={shortId}
                onChange={(e) => setShortId(e.target.value)}
                maxLength={6}
                placeholder="ABC123"
                style={{ fontFamily: 'monospace', letterSpacing: '0.18em' }}
              />
              <span className="form-hint">6 letters or digits. Share this with your watch.</span>
            </label>
            <label>
              Your user ID
              <input value={profile?.id || ''} readOnly style={{ fontFamily: 'monospace' }} />
              <span className="form-hint">Use this GUID if your watch supports it directly.</span>
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
          {feedback && <div className="form-success">{feedback}</div>}
          <button className="btn btn-primary" style={{ alignSelf: 'flex-start' }}>Save pairing details</button>
        </form>
      </section>
    </>
  )
}
