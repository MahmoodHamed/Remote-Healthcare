import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../utils/api'

export default function DoctorPatients() {
  const [patients, setPatients] = useState([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let mounted = true
    api.get('/api/patients')
      .then((data) => mounted && setPatients(data ?? []))
      .catch((err) => mounted && setError(err.message || 'Could not load patients.'))
      .finally(() => mounted && setLoading(false))
    return () => { mounted = false }
  }, [])

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return patients
    return patients.filter((p) =>
      [p.fullName, p.email, p.bloodType].filter(Boolean).some((x) => x.toLowerCase().includes(q)),
    )
  }, [patients, search])

  return (
    <section className="card">
      <div className="card-head">
        <div>
          <h2>My patients</h2>
          <span className="muted">{loading ? 'Loading…' : `${filtered.length} of ${patients.length}`}</span>
        </div>
        <input
          type="search"
          placeholder="Search…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          style={{ minWidth: '240px', padding: '0.55rem 0.85rem', borderRadius: 10, border: '1px solid var(--line-strong)' }}
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
              <th>DOB</th>
              <th>Blood type</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {loading && Array.from({ length: 5 }).map((_, idx) => (
              <tr key={idx}><td colSpan={6}><div className="skeleton" style={{ height: 18 }} /></td></tr>
            ))}
            {!loading && filtered.length === 0 && (
              <tr><td colSpan={6} className="empty-state">No patients match this search.</td></tr>
            )}
            {!loading && filtered.map((p) => (
              <tr key={p.userId}>
                <td><strong>{p.fullName}</strong></td>
                <td>{p.email}</td>
                <td>{p.phone || '—'}</td>
                <td>{p.dateOfBirth || '—'}</td>
                <td>{p.bloodType || '—'}</td>
                <td><Link to={`/doctor/patients/${p.userId}`} className="row-link">Open live vitals →</Link></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}
