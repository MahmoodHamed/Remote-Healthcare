import { useState, useEffect } from 'react'
import { BrowserRouter, Routes, Route, Navigate, Link } from 'react-router-dom'
import './App.css'

import Landing from './pages/Landing'
import Login from './pages/Login'
import Register from './pages/Register'
import Dashboard, { HeartRateMonitor } from './pages/Dashboard'
import PatientMonitor from './pages/PatientMonitor'
import LinkWatch from './pages/LinkWatch'

const DEFAULT_API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000'

const roleRoutes = {
  Admin: [
    { to: '/dashboard', label: 'Admin dashboard' },
    { to: '/monitor', label: 'Live monitor' },
    { to: '/heart-rate', label: 'Heart rate' },
    { to: '/link-watch', label: 'Watch setup' },
  ],
  Doctor: [
    { to: '/dashboard', label: 'Doctor dashboard' },
    { to: '/monitor', label: 'Patient monitor' },
    { to: '/heart-rate', label: 'Heart rate' },
  ],
  Patient: [
    { to: '/dashboard', label: 'My dashboard' },
    { to: '/monitor', label: 'My vitals' },
    { to: '/link-watch', label: 'Link watch' },
  ],
}

const normalizeRoleClass = (role) => {
  if (role === 'Admin') return 'role-admin'
  if (role === 'Doctor') return 'role-doctor'
  return 'role-patient'
}

function Header({ authProfile, onLogout }) {
  const navItems = roleRoutes[authProfile?.role] || roleRoutes.Patient

  return (
    <header className="nav">
      <div className="container nav-inner">
        <Link className="brand" to="/" aria-label="Remote Care">
          <span className="brand-mark">RC</span>
          Remote Care
        </Link>

        <nav className="nav-links">
          {authProfile ? (
            <>
              {navItems.map((item) => (
                <Link key={item.to} to={item.to}>{item.label}</Link>
              ))}
              {authProfile?.role === 'Admin' && <Link to="/dashboard#admin">Users</Link>}
              <span className={`role-badge role-badge-${normalizeRoleClass(authProfile?.role)}`}>
                {authProfile?.role}
              </span>
              <button className="nav-link-btn" onClick={onLogout} style={{ cursor: 'pointer' }}>
                Sign out
              </button>
            </>
          ) : (
            <>
              <Link to="/login">Sign in</Link>
              <Link to="/register">Register</Link>
            </>
          )}
        </nav>

        <div className="nav-cta">
          {!authProfile && <Link className="btn btn-primary" to="/login">Sign in</Link>}
        </div>
      </div>
    </header>
  )
}

function Footer() {
  return (
    <footer className="footer">
      <div className="container">
        <p>&copy; 2026 Remote Care. Secure remote patient monitoring system.</p>
      </div>
    </footer>
  )
}

// Protected Route - redirect to login if not authenticated
function ProtectedRoute({ authProfile, children }) {
  if (!authProfile) {
    return <Navigate to="/login" replace />
  }
  return children
}

export default function App() {
  const [authProfile, setAuthProfile] = useState(() => {
    try {
      const saved = localStorage.getItem('authSession')
      if (!saved) return null
      const session = JSON.parse(saved)
      return session?.profile ?? null
    } catch (err) {
      localStorage.removeItem('authSession')
      return null
    }
  })
  const [accessToken, setAccessToken] = useState(() => {
    try {
      const saved = localStorage.getItem('authSession')
      if (!saved) return null
      const session = JSON.parse(saved)
      return session?.token ?? null
    } catch (err) {
      localStorage.removeItem('authSession')
      return null
    }
  })

  const handleLoginSuccess = (data) => {
    const token = data?.tokens?.accessToken
    const user = data?.user
    if (token && user) {
      setAccessToken(token)
      setAuthProfile(user)
      localStorage.setItem('authSession', JSON.stringify({ token, profile: user }))
    }
  }

  const handleRegisterSuccess = (data) => {
    const token = data?.tokens?.accessToken
    const user = data?.user
    if (token && user) {
      setAccessToken(token)
      setAuthProfile(user)
      localStorage.setItem('authSession', JSON.stringify({ token, profile: user }))
    }
  }

  const handleLogout = () => {
    setAccessToken(null)
    setAuthProfile(null)
    localStorage.removeItem('authSession')
  }

  return (
    <BrowserRouter>
      <div className={`page ${normalizeRoleClass(authProfile?.role)}`}>
        <Header authProfile={authProfile} onLogout={handleLogout} />

        <Routes>
          <Route path="/" element={<Landing />} />
          <Route path="/login" element={<Login onLoginSuccess={handleLoginSuccess} />} />
          <Route path="/register" element={<Register onRegisterSuccess={handleRegisterSuccess} />} />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute authProfile={authProfile}>
                <Dashboard
                  authProfile={authProfile}
                  accessToken={accessToken}
                  onLogout={handleLogout}
                />
              </ProtectedRoute>
            }
          />
          <Route
            path="/heart-rate"
            element={
              <ProtectedRoute authProfile={authProfile}>
                <HeartRateMonitor
                  authProfile={authProfile}
                  accessToken={accessToken}
                  onLogout={handleLogout}
                />
              </ProtectedRoute>
            }
          />
          <Route
            path="/monitor"
            element={
              <ProtectedRoute authProfile={authProfile}>
                <PatientMonitor
                  authProfile={authProfile}
                  accessToken={accessToken}
                  onLogout={handleLogout}
                />
              </ProtectedRoute>
            }
          />
          <Route
            path="/link-watch"
            element={
              <ProtectedRoute authProfile={authProfile}>
                <LinkWatch authProfile={authProfile} />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>

        <Footer />
      </div>
    </BrowserRouter>
  )
}
