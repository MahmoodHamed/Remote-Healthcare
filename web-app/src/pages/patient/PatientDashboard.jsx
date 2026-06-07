import { useEffect, useState } from 'react'
import { readProfile } from '../../utils/auth'
import { loadAndSyncWatchShortId } from '../../utils/watchPairing'
import { api } from '../../utils/api'
import LiveVitals from '../../components/LiveVitals.jsx'

export default function PatientDashboard() {
  const profile = readProfile()
  const [vitalsPatientId, setVitalsPatientId] = useState(() => loadAndSyncWatchShortId(null, profile?.id))

  useEffect(() => {
    if (!profile?.id) return
    // Fetch profile from backend to get the stored watchShortId and use it
    api.get(`/api/patients/${profile.id}`)
      .then((data) => {
        const resolved = loadAndSyncWatchShortId(data?.watchShortId, profile.id)
        setVitalsPatientId(resolved)
      })
      .catch(() => {})
  }, [profile?.id])

  return (
    <>
      <section className="card">
        <div className="card-head">
          <div>
            <h2>Welcome back{profile?.fullName ? `, ${profile.fullName.split(' ')[0]}` : ''}</h2>
            <span className="muted">
              Your watch streams every sensor it supports — heart rate, SpO₂, ECG, skin temperature, sleep, stress and more.
            </span>
          </div>
          <span className="tag patient">Patient</span>
        </div>
      </section>
      <LiveVitals patientId={vitalsPatientId} patientName={profile?.fullName} />
    </>
  )
}
