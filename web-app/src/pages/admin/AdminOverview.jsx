import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../utils/api'

const formatNumber = (value) => (typeof value === 'number' ? value.toLocaleString() : '—')

export default function AdminOverview() {
  const [overview, setOverview] = useState(null)
  const [doctors, setDoctors] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let mounted = true
    const load = async () => {
      setLoading(true)
      setError('')
      try {
        const [ov, docs] = await Promise.all([
          api.get('/api/admin/overview'),
          api.get('/api/admin/doctors'),
        ])
        if (!mounted) return
        setOverview(ov)
        setDoctors(docs ?? [])
      } catch (err) {
        if (mounted) setError(err.message || 'Could not load admin overview.')
      } finally {
        if (mounted) setLoading(false)
      }
    }
    load()
    return () => { mounted = false }
  }, [])

  return (
    <>
      <div className="stat-grid">
        <div className="stat admin">
          <span className="label">Total users</span>
          <span className="value">{loading ? '…' : formatNumber(overview?.totalUsers)}</span>
        </div>
        <div className="stat doctor">
          <span className="label">Active doctors</span>
          <span className="value">{loading ? '…' : formatNumber(overview?.totalDoctors)}</span>
        </div>
        <div className="stat patient">
          <span className="label">Patients</span>
          <span className="value">{loading ? '…' : formatNumber(overview?.totalPatients)}</span>
        </div>
        <div className="stat accent">
          <span className="label">Active assignments</span>
          <span className="value">{loading ? '…' : formatNumber(overview?.activeAssignments)}</span>
          <span className="trend">Doctor ↔ patient links</span>
        </div>
      </div>

      {error && <div className="form-error">{error}</div>}

      <section className="card">
        <div className="card-head">
          <div>
            <h2>Doctors directory</h2>
            <span className="muted">All clinicians in the hospital and their patient load.</span>
          </div>
          <Link to="/admin/doctors" className="btn btn-outline btn-sm">View full directory →</Link>
        </div>

        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Doctor</th>
                <th>Specialization</th>
                <th>Hospital</th>
                <th>Patients</th>
                <th>Status</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {loading && Array.from({ length: 4 }).map((_, idx) => (
                <tr key={idx}>
                  <td colSpan={6}><div className="skeleton" style={{ height: 18 }} /></td>
                </tr>
              ))}
              {!loading && doctors.length === 0 && (
                <tr>
                  <td colSpan={6} className="empty-state">No doctors yet.</td>
                </tr>
              )}
              {!loading && doctors.slice(0, 6).map((doctor) => (
                <tr key={doctor.userId}>
                  <td>
                    <strong>{doctor.fullName}</strong>
                    <div className="muted" style={{ fontSize: '0.78rem' }}>{doctor.email}</div>
                  </td>
                  <td>{doctor.specialization}</td>
                  <td>{doctor.hospitalName || '—'}</td>
                  <td>{doctor.patientCount}</td>
                  <td>
                    {doctor.isActive
                      ? <span className="tag success">Active</span>
                      : <span className="tag danger">Disabled</span>}
                  </td>
                  <td>
                    <Link to={`/admin/doctors/${doctor.userId}`} className="row-link">Open →</Link>
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
