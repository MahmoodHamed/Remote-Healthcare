import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { getMqttHost, getMqttPort } from '../utils/apiBase'

const DEFAULT_MQTT_HOST = getMqttHost()
const DEFAULT_MQTT_PORT = getMqttPort()

export default function LinkWatch({ authProfile }) {
  const navigate = useNavigate()
  const [patientId, setPatientId] = useState(() => localStorage.getItem('rpmPatientId') || 'ABC123')
  const [mqttHost, setMqttHost] = useState(DEFAULT_MQTT_HOST)
  const [mqttPort, setMqttPort] = useState(DEFAULT_MQTT_PORT)
  const [saved, setSaved] = useState(false)

  useEffect(() => {
    if (!authProfile) navigate('/login')
  }, [authProfile, navigate])

  const handleSave = (event) => {
    event.preventDefault()
    const trimmed = patientId.trim().toUpperCase()
    if (!/^[A-Z0-9]{6}$/.test(trimmed)) return
    localStorage.setItem('rpmPatientId', trimmed)
    localStorage.setItem('rpmMqttHost', mqttHost.trim())
    localStorage.setItem('rpmMqttPort', mqttPort.trim())
    setPatientId(trimmed)
    setSaved(true)
    setTimeout(() => setSaved(false), 2500)
  }

  return (
    <main className="section">
      <div className="container link-grid">
        <div>
          <p className="eyebrow">Link watch</p>
          <h2>Pair the Samsung watch with the server</h2>
          <ol className="link-steps">
            <li>On the watch, open Setup and enter the patient ID below.</li>
            <li>Set MQTT host to <strong>{mqttHost || DEFAULT_MQTT_HOST}</strong> and port <strong>{mqttPort || DEFAULT_MQTT_PORT}</strong>.</li>
            <li>Tap Start on the watch to publish vitals every 5 seconds.</li>
            <li>On Patient monitor, enter the same patient ID and connect.</li>
          </ol>
          <p className="muted">Patient ID must be 6 characters (A-Z, 0-9). Doctors and family use the same ID.</p>
        </div>

        <div className="live-panel">
          <form className="live-form" onSubmit={handleSave}>
            <label>
              Patient ID
              <input
                type="text"
                value={patientId}
                onChange={(e) => setPatientId(e.target.value.toUpperCase())}
                placeholder="ABC123"
                autoComplete="off"
                minLength={6}
                maxLength={6}
                pattern="[A-Z0-9]{6}"
                required
              />
            </label>
            <label>
              MQTT broker host (watch → server)
              <input
                type="text"
                value={mqttHost}
                onChange={(e) => setMqttHost(e.target.value)}
                placeholder={DEFAULT_MQTT_HOST}
                autoComplete="off"
                required
              />
            </label>
            <label>
              MQTT port
              <input
                type="text"
                value={mqttPort}
                onChange={(e) => setMqttPort(e.target.value)}
                placeholder="1883"
                autoComplete="off"
                required
              />
            </label>
            <button type="submit" className="btn btn-primary">
              Save pairing info
            </button>
            {saved && <p className="muted" style={{ marginTop: '0.5rem' }}>Saved. Use these values on the watch.</p>}
          </form>
        </div>
      </div>
    </main>
  )
}
