import { useEffect, useMemo, useState } from 'react'
import { api } from '../../utils/api'

export default function AdminPatients() {
  const [patients, setPatients] = useState([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let mounted = true
    setLoading(true)
    api.get('/api/admin/patients')
      .then((data) => mounted && setPatients(data ?? []))
      .catch((err) => mounted && setError(err.message || 'Could not load patients.'))
      .finally(() => mounted && setLoading(false))
    return () => { mounted = false }
  }, [])

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return patients
    return patients.filter((p) =>
      [p.fullName, p.email, p.phone, p.bloodType]
        .filter(Boolean)
        .some((field) => field.toLowerCase().includes(q)),
    )
  }, [patients, search])

  return (
    <>
      <section className="card">
        <div className="card-head">
          <div>
            <h2>All patients</h2>
            <span className="muted">{loading ? 'Loading…' : `${filtered.length} of ${patients.length} patients`}</span>
          </div>
          <input
            type="search"
            placeholder="Search by name, email or blood type…"
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
                <th>Patient</th>
                <th>Email</th>
                <th>Phone</th>
                <th>Date of birth</th>
                <th>Blood type</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {loading && Array.from({ length: 5 }).map((_, idx) => (
                <tr key={idx}><td colSpan={6}><div className="skeleton" style={{ height: 18 }} /></td></tr>
              ))}
              {!loading && filtered.length === 0 && (
                <tr><td colSpan={6} className="empty-state">No patients match this search.</td></tr>
              )}
              {!loading && filtered.map((patient) => (
                <tr key={patient.userId}>
                  <td><strong>{patient.fullName}</strong></td>
                  <td>{patient.email}</td>
                  <td>{patient.phone || '—'}</td>
                  <td>{patient.dateOfBirth || '—'}</td>
                  <td>{patient.bloodType || '—'}</td>
                  <td>
                    {patient.isActive
                      ? <span className="tag success">Active</span>
                      : <span className="tag danger">Disabled</span>}
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
