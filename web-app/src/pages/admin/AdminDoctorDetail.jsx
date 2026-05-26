import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../../utils/api'

export default function AdminDoctorDetail() {
  const { doctorId } = useParams()
  const [data, setData] = useState(null)
  const [allPatients, setAllPatients] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [feedback, setFeedback] = useState('')

  const load = async () => {
    setLoading(true)
    setError('')
    try {
      const [doctor, patients] = await Promise.all([
        api.get(`/api/admin/doctors/${doctorId}`),
        api.get('/api/admin/patients'),
      ])
      setData(doctor)
      setAllPatients(patients ?? [])
    } catch (err) {
      setError(err.message || 'Could not load doctor.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [doctorId])

  const assignedIds = new Set((data?.patients ?? []).map((p) => p.userId))
  const unassigned = allPatients.filter((p) => !assignedIds.has(p.userId))

  const assign = async (patientUserId) => {
    setBusy(true)
    setFeedback('')
    try {
      await api.post('/api/admin/assignments', { doctorUserId: doctorId, patientUserId })
      setFeedback('Patient assigned successfully.')
      await load()
    } catch (err) {
      setError(err.message || 'Could not assign patient.')
    } finally {
      setBusy(false)
    }
  }

  const revoke = async (patientUserId) => {
    if (!confirm('Revoke this assignment?')) return
    setBusy(true)
    setFeedback('')
    try {
      await api.delete('/api/admin/assignments', { doctorUserId: doctorId, patientUserId })
      setFeedback('Assignment revoked.')
      await load()
    } catch (err) {
      setError(err.message || 'Could not revoke assignment.')
    } finally {
      setBusy(false)
    }
  }

  if (loading) {
    return <div className="card"><div className="skeleton" style={{ height: 200 }} /></div>
  }

  if (error) {
    return (
      <div className="card">
        <div className="form-error">{error}</div>
        <Link to="/admin/doctors" className="btn btn-outline btn-sm" style={{ marginTop: '1rem' }}>← Back to doctors</Link>
      </div>
    )
  }

  if (!data?.doctor) {
    return (
      <div className="card empty-state">
        <div className="big">😕</div>
        Doctor not found.
        <div><Link to="/admin/doctors" className="btn btn-outline btn-sm" style={{ marginTop: '1rem' }}>Back</Link></div>
      </div>
    )
  }

  const doctor = data.doctor
  const patients = data.patients ?? []

  return (
    <>
      <section className="card">
        <div className="card-head">
          <div>
            <Link to="/admin/doctors" className="muted" style={{ fontSize: '0.85rem' }}>← Back to all doctors</Link>
            <h2 style={{ marginTop: '0.35rem' }}>{doctor.fullName}</h2>
            <div className="muted">{doctor.specialization} · {doctor.hospitalName || 'No hospital set'}</div>
          </div>
          <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
            <span className="tag doctor">Doctor</span>
            {doctor.isActive
              ? <span className="tag success">Active</span>
              : <span className="tag danger">Disabled</span>}
          </div>
        </div>
        <div className="stat-grid">
          <div className="stat doctor">
            <span className="label">Patients</span>
            <span className="value">{doctor.patientCount}</span>
          </div>
          <div className="stat">
            <span className="label">License number</span>
            <span className="value" style={{ fontSize: '1.1rem', fontFamily: 'monospace' }}>{doctor.licenseNumber || '—'}</span>
          </div>
          <div className="stat">
            <span className="label">Contact</span>
            <span className="value" style={{ fontSize: '1rem' }}>{doctor.email}</span>
            <span className="trend">{doctor.phone || 'No phone'}</span>
          </div>
        </div>
      </section>

      {feedback && <div className="form-success">{feedback}</div>}

      <section className="card">
        <div className="card-head">
          <div>
            <h3>Assigned patients</h3>
            <span className="muted">Patients under {doctor.fullName.split(' ')[0]}'s care.</span>
          </div>
        </div>
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Patient</th>
                <th>Email</th>
                <th>Date of birth</th>
                <th>Blood type</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {patients.length === 0 && (
                <tr><td colSpan={5} className="empty-state">No patients assigned yet.</td></tr>
              )}
              {patients.map((patient) => (
                <tr key={patient.userId}>
                  <td><strong>{patient.fullName}</strong></td>
                  <td>{patient.email}</td>
                  <td>{patient.dateOfBirth || '—'}</td>
                  <td>{patient.bloodType || '—'}</td>
                  <td>
                    <button
                      type="button"
                      className="btn btn-outline btn-sm"
                      onClick={() => revoke(patient.userId)}
                      disabled={busy}
                    >
                      Revoke
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="card">
        <div className="card-head">
          <div>
            <h3>Add patients to this doctor</h3>
            <span className="muted">Pick a patient from the hospital to bring under this doctor's care.</span>
          </div>
        </div>
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Patient</th>
                <th>Email</th>
                <th>Blood type</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {unassigned.length === 0 && (
                <tr><td colSpan={4} className="empty-state">All hospital patients are already assigned to this doctor.</td></tr>
              )}
              {unassigned.map((patient) => (
                <tr key={patient.userId}>
                  <td><strong>{patient.fullName}</strong></td>
                  <td>{patient.email}</td>
                  <td>{patient.bloodType || '—'}</td>
                  <td>
                    <button
                      type="button"
                      className="btn btn-primary btn-sm"
                      onClick={() => assign(patient.userId)}
                      disabled={busy}
                    >
                      Assign
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </>
  )
}
