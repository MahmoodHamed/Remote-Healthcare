import { readProfile } from '../../utils/auth'
import { getVitalsPatientId } from '../../utils/watchPairing'
import LiveVitals from '../../components/LiveVitals.jsx'

export default function PatientDashboard() {
  const profile = readProfile()
  const vitalsPatientId = getVitalsPatientId(profile?.id)
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
