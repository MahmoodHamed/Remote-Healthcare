import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../../utils/api'
import LiveVitals from '../../components/LiveVitals.jsx'

export default function DoctorPatientDetail() {
  const { patientUserId } = useParams()
  const [patient, setPatient] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let mounted = true
    setLoading(true)
    setError('')
    api.get(`/api/patients/${patientUserId}`)
      .then((data) => mounted && setPatient(data))
      .catch((err) => mounted && setError(err.message || 'Could not load patient.'))
      .finally(() => mounted && setLoading(false))
    return () => { mounted = false }
  }, [patientUserId])

  if (loading) {
    return <div className="card"><div className="skeleton" style={{ height: 220 }} /></div>
  }

  if (error || !patient) {
    return (
      <div className="card">
        <div className="form-error">{error || 'Patient not found.'}</div>
        <Link to="/doctor/patients" className="btn btn-outline btn-sm" style={{ marginTop: '1rem' }}>← Back</Link>
      </div>
    )
  }

  return (
    <>
      <section className="card">
        <div className="card-head">
          <div>
            <Link to="/doctor/patients" className="muted" style={{ fontSize: '0.85rem' }}>← Back to my patients</Link>
            <h2 style={{ marginTop: '0.35rem' }}>{patient.fullName}</h2>
            <div className="muted">
              {patient.email} {patient.phone ? `· ${patient.phone}` : ''}
            </div>
          </div>
          <span className="tag patient">Patient</span>
        </div>

        <div className="stat-grid">
          <div className="stat">
            <span className="label">Date of birth</span>
            <span className="value" style={{ fontSize: '1.05rem' }}>{patient.dateOfBirth || '—'}</span>
          </div>
          <div className="stat">
            <span className="label">Blood type</span>
            <span className="value">{patient.bloodType || '—'}</span>
          </div>
          <div className="stat">
            <span className="label">Weight / height</span>
            <span className="value" style={{ fontSize: '1.05rem' }}>
              {patient.weightKg ? `${patient.weightKg} kg` : '—'} / {patient.heightCm ? `${patient.heightCm} cm` : '—'}
            </span>
          </div>
          <div className="stat">
            <span className="label">Emergency phone</span>
            <span className="value" style={{ fontSize: '1.05rem' }}>{patient.emergencyContactPhone || '—'}</span>
          </div>
        </div>

        {patient.chronicDiseases?.length || patient.allergies?.length || patient.currentMedications?.length ? (
          <div className="stat-grid" style={{ marginTop: '1rem' }}>
            <div className="card" style={{ boxShadow: 'none', borderColor: 'var(--line)' }}>
              <h3 style={{ marginBottom: '0.5rem' }}>Chronic diseases</h3>
              <ul className="muted" style={{ display: 'grid', gap: '0.35rem' }}>
                {(patient.chronicDiseases ?? []).map((c) => <li key={c}>• {c}</li>)}
                {(patient.chronicDiseases ?? []).length === 0 && <li>No data</li>}
              </ul>
            </div>
            <div className="card" style={{ boxShadow: 'none', borderColor: 'var(--line)' }}>
              <h3 style={{ marginBottom: '0.5rem' }}>Allergies</h3>
              <ul className="muted" style={{ display: 'grid', gap: '0.35rem' }}>
                {(patient.allergies ?? []).map((c) => <li key={c}>• {c}</li>)}
                {(patient.allergies ?? []).length === 0 && <li>No data</li>}
              </ul>
            </div>
            <div className="card" style={{ boxShadow: 'none', borderColor: 'var(--line)' }}>
              <h3 style={{ marginBottom: '0.5rem' }}>Current medications</h3>
              <ul className="muted" style={{ display: 'grid', gap: '0.35rem' }}>
                {(patient.currentMedications ?? []).map((c) => <li key={c}>• {c}</li>)}
                {(patient.currentMedications ?? []).length === 0 && <li>No data</li>}
              </ul>
            </div>
          </div>
        ) : null}
      </section>

      <LiveVitals patientId={patient.userId} patientName={patient.fullName} />
    </>
  )
}
