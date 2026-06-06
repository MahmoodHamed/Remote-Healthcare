import { useEffect, useState } from 'react'
import { api } from '../../utils/api'

const ACTION_ICON = {
  Login: '🔑',
  AdminLogin: '🛡️',
  Logout: '🚪',
  Register: '📝',
  RegisterPatient: '📝',
  RegisterDoctor: '📝',
  AdminCreateUser: '➕',
  AdminUpdateUser: '✏️',
  AdminDeleteUser: '🗑️',
}

export default function AdminAuditLog() {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [page, setPage] = useState(1)

  const load = async (p = 1) => {
    setLoading(true)
    setError('')
    try {
      const data = await api.get(`/api/audit?page=${p}&pageSize=50`)
      setRows(data ?? [])
    } catch (err) {
      setError(err.message || 'Could not load audit log.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load(page) }, [page])

  return (
    <section className="card">
      <div className="card-head">
        <div>
          <h2>Audit Log</h2>
          <span className="muted">All user actions recorded by the system.</span>
        </div>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button
            type="button"
            className="btn btn-outline btn-sm"
            disabled={page === 1}
            onClick={() => setPage((p) => Math.max(1, p - 1))}
          >
            ← Prev
          </button>
          <span className="muted" style={{ lineHeight: '2rem', fontSize: '0.85rem' }}>Page {page}</span>
          <button
            type="button"
            className="btn btn-outline btn-sm"
            disabled={rows.length < 50}
            onClick={() => setPage((p) => p + 1)}
          >
            Next →
          </button>
          <button type="button" className="btn btn-outline btn-sm" onClick={() => load(page)}>
            Refresh
          </button>
        </div>
      </div>

      {error && <div className="form-error">{error}</div>}

      <div className="table-wrap">
        <table className="data-table">
          <thead>
            <tr>
              <th>Time</th>
              <th>Action</th>
              <th>User / Email</th>
              <th>Resource</th>
              <th>Detail</th>
              <th>IP Address</th>
            </tr>
          </thead>
          <tbody>
            {loading && Array.from({ length: 8 }).map((_, i) => (
              <tr key={i}><td colSpan={6}><div className="skeleton" style={{ height: 18 }} /></td></tr>
            ))}
            {!loading && rows.length === 0 && (
              <tr>
                <td colSpan={6} className="empty-state">No audit records yet.</td>
              </tr>
            )}
            {rows.map((row) => (
              <tr key={row.id}>
                <td style={{ whiteSpace: 'nowrap', fontSize: '0.82rem' }}>
                  {new Date(row.occurredAt).toLocaleString()}
                </td>
                <td>
                  <span style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                    <span>{ACTION_ICON[row.action] ?? '📋'}</span>
                    <strong style={{ fontSize: '0.87rem' }}>{row.action}</strong>
                  </span>
                </td>
                <td style={{ fontSize: '0.85rem' }}>
                  {row.userEmail ?? <span className="muted">{row.userId ? row.userId.slice(0, 8) + '…' : '—'}</span>}
                </td>
                <td><span className="tag">{row.resource ?? '—'}</span></td>
                <td style={{ fontSize: '0.82rem', color: 'var(--ink-muted)' }}>{row.detail ?? '—'}</td>
                <td style={{ fontSize: '0.82rem', color: 'var(--ink-muted)' }}>{row.ipAddress ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}
