import { useState, useEffect } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import './App.css'

import Landing from './pages/Landing'
import Login from './pages/Login'
import Register from './pages/Register'
import Dashboard from './pages/Dashboard'

const DEFAULT_API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000'

function Header({ authProfile, onLogout }) {
  return (
    <header className="nav">
      <div className="container nav-inner">
        <a className="brand" href="/" aria-label="Remote Care">
          <span className="brand-mark">RC</span>
          Remote Care
        </a>

        <nav className="nav-links">
          {authProfile ? (
            <>
              <a href="/dashboard">Dashboard</a>
              {authProfile?.role === 'Admin' && <a href="/dashboard#admin">Admin</a>}
              <button className="nav-link-btn" onClick={onLogout} style={{ cursor: 'pointer' }}>
                Sign out
              </button>
            </>
          ) : (
            <>
              <a href="/login">Sign in</a>
              <a href="/register">Register</a>
            </>
          )}
        </nav>

        <div className="nav-cta">
          {!authProfile && <a className="btn btn-primary" href="/login">Sign in</a>}
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
  const [authProfile, setAuthProfile] = useState(null)
  const [accessToken, setAccessToken] = useState(null)

  // Load auth state from localStorage on mount
  useEffect(() => {
    const saved = localStorage.getItem('authSession')
    if (saved) {
      try {
        const session = JSON.parse(saved)
        setAuthProfile(session.profile)
        setAccessToken(session.token)
      } catch (err) {
        localStorage.removeItem('authSession')
      }
    }
  }, [])

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
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>

        <Footer />
      </div>
    </BrowserRouter>
  )
}
