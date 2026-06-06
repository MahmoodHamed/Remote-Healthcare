import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../../utils/api'
import { readProfile } from '../../utils/auth'
import LiveVitals from '../../components/LiveVitals.jsx'

export default function DoctorPatientDetail() {
  const { patientUserId } = useParams()
  const navigate = useNavigate()
  const profile = readProfile()
  const [patient, setPatient] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [chatLoading, setChatLoading] = useState(false)

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
          <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
            <button
              type="button"
              className="btn btn-outline btn-sm"
              disabled={chatLoading}
              onClick={async () => {
                setChatLoading(true)
                try {
                  const convs = await api.get('/api/chat/conversations')
                  const existing = (convs ?? []).find((c) =>
                    c.participants?.some((p) => p.userId === patientUserId) &&
                    c.participants?.some((p) => p.userId === profile?.id)
                  )
                  if (existing) {
                    navigate('/doctor/chat', { state: { openConvId: existing.id } })
                  } else {
                    await api.post('/api/chat/conversations', {
                      type: 'DoctorPatient',
                      name: `Chat with ${patient.fullName}`,
                      participantIds: [profile?.id, patientUserId],
                    })
                    navigate('/doctor/chat')
                  }
                } catch {
                  navigate('/doctor/chat')
                } finally {
                  setChatLoading(false)
                }
              }}
            >
              {chatLoading ? 'Opening…' : '💬 Chat'}
            </button>
            <span className="tag patient">Patient</span>
          </div>
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
