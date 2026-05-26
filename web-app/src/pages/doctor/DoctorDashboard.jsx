import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../utils/api'

export default function DoctorDashboard() {
  const [patients, setPatients] = useState([])
  const [notifications, setNotifications] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let mounted = true
    const load = async () => {
      try {
        const [list, notif] = await Promise.all([
          api.get('/api/patients'),
          api.get('/api/notifications?page=1&pageSize=5'),
        ])
        if (!mounted) return
        setPatients(list ?? [])
        setNotifications(notif)
      } catch (err) {
        if (mounted) setError(err.message || 'Could not load doctor dashboard.')
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
        <div className="stat doctor">
          <span className="label">My patients</span>
          <span className="value">{loading ? '…' : patients.length}</span>
          <span className="trend">Under your active care</span>
        </div>
        <div className="stat accent">
          <span className="label">Unread alerts</span>
          <span className="value">{loading ? '…' : (notifications?.unreadCount ?? 0)}</span>
          <span className="trend"><Link to="/doctor/notifications">View inbox →</Link></span>
        </div>
      </div>

      {error && <div className="form-error">{error}</div>}

      <section className="card">
        <div className="card-head">
          <div>
            <h2>Your patients</h2>
            <span className="muted">Tap a row to open the live vitals dashboard.</span>
          </div>
          <Link to="/doctor/patients" className="btn btn-outline btn-sm">All patients →</Link>
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
              {loading && Array.from({ length: 4 }).map((_, idx) => (
                <tr key={idx}><td colSpan={5}><div className="skeleton" style={{ height: 18 }} /></td></tr>
              ))}
              {!loading && patients.length === 0 && (
                <tr>
                  <td colSpan={5} className="empty-state">
                    No patients assigned yet. The hospital admin will link patients to you.
                  </td>
                </tr>
              )}
              {!loading && patients.slice(0, 8).map((p) => (
                <tr key={p.userId}>
                  <td><strong>{p.fullName}</strong></td>
                  <td>{p.email}</td>
                  <td>{p.dateOfBirth || '—'}</td>
                  <td>{p.bloodType || '—'}</td>
                  <td><Link to={`/doctor/patients/${p.userId}`} className="row-link">Open live →</Link></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="card">
        <div className="card-head">
          <div>
            <h3>Latest notifications</h3>
            <span className="muted">Most recent alerts the system pushed to you.</span>
          </div>
          <Link to="/doctor/notifications" className="btn btn-outline btn-sm">Open inbox →</Link>
        </div>
        {!loading && (notifications?.items?.length ?? 0) === 0 && (
          <div className="empty-state"><div className="big">🔕</div>No alerts yet.</div>
        )}
        <div className="notification-list">
          {notifications?.items?.slice(0, 5).map((n) => (
            <article key={n.id} className={`notification-item ${n.isRead ? '' : 'unread'}`}>
              <span className="notification-icon warning">⚠️</span>
              <div className="notification-body">
                <strong>{n.title}</strong>
                <p>{n.body}</p>
              </div>
              <span className="notification-meta">{new Date(n.sentAt).toLocaleString()}</span>
            </article>
          ))}
        </div>
      </section>
    </>
  )
}
