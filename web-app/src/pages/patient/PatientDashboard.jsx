import { readProfile } from '../../utils/auth'
import LiveVitals from '../../components/LiveVitals.jsx'

export default function PatientDashboard() {
  const profile = readProfile()
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
      <LiveVitals patientId={profile?.id} patientName={profile?.fullName} />
    </>
  )
}
