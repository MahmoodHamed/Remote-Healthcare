import { useState } from 'react'
import { BrowserRouter, Routes, Route, Navigate, Link } from 'react-router-dom'
import './App.css'

import Landing from './pages/Landing'
import Login from './pages/Login'
import Register from './pages/Register'
import Dashboard, { HeartRateMonitor } from './pages/Dashboard'
import PatientMonitor from './pages/PatientMonitor'
import { clearAuthSession, loadAuthSession, saveAuthSession } from './utils/authSession'

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

  const handleLoginSuccess = (data) => {
    const accessToken = data?.tokens?.accessToken
    const user = data?.user
    if (accessToken && user) {
      setAccessToken(accessToken)
      setAuthProfile(user)
      saveAuthSession({ accessToken, profile: user })
    }
  }

  const handleRegisterSuccess = (data) => {
    const accessToken = data?.tokens?.accessToken
    const user = data?.user
    if (accessToken && user) {
      setAccessToken(accessToken)
      setAuthProfile(user)
      saveAuthSession({ accessToken, profile: user })
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
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>

        <Footer />
      </div>
    </BrowserRouter>
  )
}
