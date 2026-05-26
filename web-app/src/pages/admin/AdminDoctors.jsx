import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../utils/api'

export default function AdminDoctors() {
  const [doctors, setDoctors] = useState([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let mounted = true
    setLoading(true)
    api.get('/api/admin/doctors')
      .then((data) => mounted && setDoctors(data ?? []))
      .catch((err) => mounted && setError(err.message || 'Could not load doctors.'))
      .finally(() => mounted && setLoading(false))
    return () => { mounted = false }
  }, [])

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return doctors
    return doctors.filter((d) =>
      [d.fullName, d.email, d.specialization, d.hospitalName, d.licenseNumber]
        .filter(Boolean)
        .some((field) => field.toLowerCase().includes(q)),
    )
  }, [doctors, search])

  return (
    <>
      <section className="card">
        <div className="card-head">
          <div>
            <h2>All doctors</h2>
            <span className="muted">{loading ? 'Loading…' : `${filtered.length} of ${doctors.length} doctors`}</span>
          </div>
          <input
            type="search"
            placeholder="Search by name, specialization, license…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{ minWidth: '260px', padding: '0.55rem 0.85rem', borderRadius: 10, border: '1px solid var(--line-strong)' }}
          />
        </div>

        {error && <div className="form-error">{error}</div>}

        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Doctor</th>
                <th>Specialization</th>
                <th>License</th>
                <th>Hospital</th>
                <th>Patients</th>
                <th>Status</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {loading && Array.from({ length: 5 }).map((_, idx) => (
                <tr key={idx}><td colSpan={7}><div className="skeleton" style={{ height: 18 }} /></td></tr>
              ))}
              {!loading && filtered.length === 0 && (
                <tr><td colSpan={7} className="empty-state">No doctors match this search.</td></tr>
              )}
              {!loading && filtered.map((doctor) => (
                <tr key={doctor.userId}>
                  <td>
                    <strong>{doctor.fullName}</strong>
                    <div className="muted" style={{ fontSize: '0.78rem' }}>{doctor.email}</div>
                  </td>
                  <td>{doctor.specialization}</td>
                  <td>{doctor.licenseNumber || '—'}</td>
                  <td>{doctor.hospitalName || '—'}</td>
                  <td>{doctor.patientCount}</td>
                  <td>
                    {doctor.isActive
                      ? <span className="tag success">Active</span>
                      : <span className="tag danger">Disabled</span>}
                  </td>
                  <td>
                    <Link to={`/admin/doctors/${doctor.userId}`} className="row-link">View patients →</Link>
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
