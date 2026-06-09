import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom'
import ThemeToggle from '../components/ThemeToggle'
import { api } from '../utils/api'

const initials = (name = '') =>
  name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join('') || '?'

export default function RoleShell({ profile, onLogout, roleClass, brand, nav, children }) {
  const [open, setOpen] = useState(false)
  const [unread, setUnread] = useState(0)
  const prevUnreadRef = useRef(0)
  const navigate = useNavigate()
  const location = useLocation()

  const loadUnread = useCallback(async () => {
    try {
      const data = await api.get('/api/notifications/unread-count')
      const count = data?.count ?? 0
      setUnread(count)

      if (
        count > prevUnreadRef.current &&
        document.hidden &&
        typeof Notification !== 'undefined' &&
        Notification.permission === 'granted'
      ) {
        const delta = count - prevUnreadRef.current
        new Notification(delta === 1 ? 'New notification' : `${delta} new notifications`, {
          body: 'Open your inbox to review alerts and messages.',
          tag: 'rpm-unread',
        })
      }
      prevUnreadRef.current = count
    } catch {
      setUnread(0)
    }
  }, [])

  useEffect(() => {
    loadUnread()
    const id = setInterval(loadUnread, 15_000)
    const onFocus = () => loadUnread()
    const onVisibility = () => {
      if (document.visibilityState === 'visible') loadUnread()
    }
    window.addEventListener('focus', onFocus)
    document.addEventListener('visibilitychange', onVisibility)
    return () => {
      clearInterval(id)
      window.removeEventListener('focus', onFocus)
      document.removeEventListener('visibilitychange', onVisibility)
    }
  }, [location.pathname, loadUnread])

  useEffect(() => {
    setOpen(false)
  }, [location.pathname])

  const handleSignOut = () => {
    onLogout?.()
    navigate('/login', { replace: true })
  }

  const notificationsRoute = nav.find((item) => item.to.endsWith('/notifications'))?.to

  const currentTitle = nav.find((item) => location.pathname.startsWith(item.to))?.label ?? brand.title

  return (
    <div className={`app-frame ${roleClass} ${open ? 'sidebar-open' : ''}`}>
      <aside className="sidebar" aria-label="Primary navigation">
        <Link to="/" className="sidebar-brand">
          <span className="brand-mark">RC</span>
          <span>
            <strong>{brand.title}</strong>
            <span className="small">{brand.subtitle}</span>
          </span>
        </Link>

        <nav className="sidebar-section" aria-label="Workspace navigation">
          <div className="sidebar-section-title">{brand.subtitle}</div>
          {nav.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
            >
              <span className="sidebar-icon" aria-hidden="true">{item.icon}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div className="sidebar-theme-row">
            <span className="sidebar-theme-label">Theme</span>
            <ThemeToggle variant="topbar" />
          </div>
          <div className="user-chip">
            <span className="avatar">{initials(profile?.fullName)}</span>
            <span className="meta">
              <strong>{profile?.fullName || 'User'}</strong>
              <small>{profile?.email}</small>
            </span>
          </div>
          <button className="btn btn-ghost btn-block btn-sm" onClick={handleSignOut} type="button">
            Sign out
          </button>
        </div>
      </aside>

      <div className="main">
        <div
          className="sidebar-backdrop"
          onClick={() => setOpen(false)}
          aria-hidden="true"
          tabIndex={-1}
        />
        <header className="app-topbar">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <button
              className="icon-btn mobile-menu-btn"
              type="button"
              aria-label="Open menu"
              onClick={() => setOpen(true)}
            >
              ☰
            </button>
            <div className="page-title">
              <small>{brand.subtitle}</small>
              <h1>{currentTitle}</h1>
            </div>
          </div>
          <div className="topbar-actions">
            <ThemeToggle variant="topbar" />
            {notificationsRoute && (
              <Link
                to={notificationsRoute}
                className="icon-btn"
                aria-label={`Notifications (${unread} unread)`}
              >
                🔔
                {unread > 0 && <span className="badge-dot">{unread > 99 ? '99+' : unread}</span>}
              </Link>
            )}
          </div>
        </header>
        <main className="app-content">{children}</main>
      </div>
    </div>
  )
}
