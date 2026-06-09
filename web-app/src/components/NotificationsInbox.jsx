import { useEffect, useState } from 'react'
import { api } from '../utils/api'

const iconFor = (title = '') => {
  const lower = title.toLowerCase()
  if (lower.includes('message from')) return { cls: 'info', icon: '💬' }
  if (lower.includes('critical')) return { cls: 'critical', icon: '🚨' }
  if (lower.includes('urgent') || lower.includes('high')) return { cls: 'warning', icon: '⚠️' }
  return { cls: 'info', icon: '🔔' }
}

export default function NotificationsInbox() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = async () => {
    setLoading(true)
    setError('')
    try {
      const result = await api.get('/api/notifications?page=1&pageSize=50')
      setData(result)
    } catch (err) {
      setError(err.message || 'Could not load notifications.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    const id = setInterval(load, 15_000)
    const onFocus = () => load()
    window.addEventListener('focus', onFocus)
    return () => {
      clearInterval(id)
      window.removeEventListener('focus', onFocus)
    }
  }, [])

  useEffect(() => {
    if (typeof Notification === 'undefined' || Notification.permission !== 'default') return
    Notification.requestPermission().catch(() => {})
  }, [])

  const markRead = async (id) => {
    try {
      await api.patch(`/api/notifications/${id}/read`)
      load()
    } catch (err) {
      setError(err.message || 'Could not mark as read.')
    }
  }

  const markAll = async () => {
    try {
      await api.patch('/api/notifications/read-all')
      load()
    } catch (err) {
      setError(err.message || 'Could not mark all as read.')
    }
  }

  return (
    <section className="card">
      <div className="card-head">
        <div>
          <h2>Notifications</h2>
          <span className="muted">{loading ? 'Loading…' : `${data?.unreadCount ?? 0} unread`}</span>
        </div>
        <button type="button" className="btn btn-outline btn-sm" onClick={markAll} disabled={loading || !data?.unreadCount}>
          Mark all as read
        </button>
      </div>

      {error && <div className="form-error">{error}</div>}

      <div className="notification-list">
        {loading && Array.from({ length: 4 }).map((_, idx) => (
          <div key={idx} className="skeleton" style={{ height: 64, borderRadius: 14 }} />
        ))}
        {!loading && (data?.items?.length ?? 0) === 0 && (
          <div className="empty-state"><div className="big">🔕</div>You're all caught up.</div>
        )}
        {!loading && data?.items?.map((n) => {
          const { cls, icon } = iconFor(n.title)
          return (
            <article key={n.id} className={`notification-item ${n.isRead ? '' : 'unread'}`}>
              <span className={`notification-icon ${cls}`}>{icon}</span>
              <div className="notification-body">
                <strong>{n.title}</strong>
                <p>{n.body}</p>
                <span className="muted" style={{ fontSize: '0.78rem' }}>{new Date(n.sentAt).toLocaleString()}</span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                {!n.isRead && (
                  <button type="button" className="btn btn-outline btn-sm" onClick={() => markRead(n.id)}>
                    Mark read
                  </button>
                )}
              </div>
            </article>
          )
        })}
      </div>
    </section>
  )
}
