import { useState, useEffect } from 'react'
import { BrowserRouter, Routes, Route, Navigate, Link } from 'react-router-dom'
import './App.css'

import Landing from './pages/Landing'
import Login from './pages/Login'
import Register from './pages/Register'
import Dashboard, { HeartRateMonitor } from './pages/Dashboard'
import PatientMonitor from './pages/PatientMonitor'
import PatientHub from './pages/PatientHub'
import { clearAuthSession, loadAuthSession, saveAuthSession } from './utils/authSession'
import { accountUserId } from './utils/patientId'

const normalizeAuthUser = (user) => {
  if (!user || typeof user !== 'object') return user
  const id = accountUserId(user)
  return id ? { ...user, id } : user
}

function Header({ authProfile, onLogout }) {
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
              <Link to="/dashboard">Dashboard</Link>
              <Link to="/heart-rate">Heart rate</Link>
              <Link to="/monitor">Patient monitor</Link>
              {authProfile?.role === 'Admin' && <Link to="/dashboard#admin">Admin</Link>}
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
    return loadAuthSession()?.profile ?? null
  })
  const [accessToken, setAccessToken] = useState(() => {
    return loadAuthSession()?.accessToken ?? null
  })

  useEffect(() => {
    const handler = (e) => {
      const detail = e?.detail
      if (!detail) {
        setAccessToken(null)
        setAuthProfile(null)
        return
      }
      setAccessToken(detail.accessToken ?? null)
      setAuthProfile(detail.profile ?? null)
    }
    window.addEventListener('authSessionChanged', handler)
    return () => window.removeEventListener('authSessionChanged', handler)
  }, [])

  const handleLoginSuccess = (data) => {
    const accessToken = data?.tokens?.accessToken
    const refreshToken = data?.tokens?.refreshToken
    const user = normalizeAuthUser(data?.user)
    if (accessToken && user) {
      setAccessToken(accessToken)
      setAuthProfile(user)
      saveAuthSession({ accessToken, refreshToken, profile: user })
    }
  }

  const handleRegisterSuccess = (data) => {
    const accessToken = data?.tokens?.accessToken
    const refreshToken = data?.tokens?.refreshToken
    const user = normalizeAuthUser(data?.user)
    if (accessToken && user) {
      setAccessToken(accessToken)
      setAuthProfile(user)
      saveAuthSession({ accessToken, refreshToken, profile: user })
    }
  }

  const handleLogout = () => {
    setAccessToken(null)
    setAuthProfile(null)
    clearAuthSession()
  }

  return (
    <BrowserRouter>
      <div className="page">
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
            path="/hub"
            element={
              <ProtectedRoute authProfile={authProfile}>
                <PatientHub
                  authProfile={authProfile}
                  accessToken={accessToken}
                  onLogout={handleLogout}
                  section="vitals"
                />
              </ProtectedRoute>
            }
          />
          <Route
            path="/hub/vitals"
            element={
              <ProtectedRoute authProfile={authProfile}>
                <PatientHub
                  authProfile={authProfile}
                  accessToken={accessToken}
                  onLogout={handleLogout}
                  section="vitals"
                />
              </ProtectedRoute>
            }
          />
          <Route
            path="/hub/watch"
            element={
              <ProtectedRoute authProfile={authProfile}>
                <PatientHub
                  authProfile={authProfile}
                  accessToken={accessToken}
                  onLogout={handleLogout}
                  section="watch"
                />
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
