import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'

const DEFAULT_API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000'

const parseErrorMessage = async (response) => {
  try {
    const data = await response.json()
    if (!data) return ''
    if (typeof data === 'string') return data
    if (typeof data.message === 'string') return data.message
    if (typeof data.title === 'string') return data.title
    if (data.errors && typeof data.errors === 'object') {
      const [firstKey] = Object.keys(data.errors)
      if (firstKey) {
        const detail = data.errors[firstKey]
        if (Array.isArray(detail) && detail[0]) return `${firstKey}: ${detail[0]}`
        if (typeof detail === 'string') return `${firstKey}: ${detail}`
      }
    }
  } catch {
    return ''
  }
  return ''
}

export default function Login({ onLoginSuccess }) {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [deviceInfo, setDeviceInfo] = useState('web-app')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleLogin = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError('')

    try {
      const base = DEFAULT_API_BASE.trim()
      if (!base || !email.trim() || !password) {
        throw new Error('Email and password are required.')
      }

      const loginUrl = new URL('/api/auth/login', base).toString()

      const response = await fetch(loginUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email: email.trim(),
          password,
          deviceInfo: deviceInfo.trim() || 'web-app',
        }),
      })

      if (!response.ok) {
        const message = await parseErrorMessage(response)
        throw new Error(message || `Login failed (${response.status}).`)
      }

      const data = await response.json()
      if (onLoginSuccess) {
        onLoginSuccess(data)
      }
      navigate('/dashboard')
    } catch (err) {
      setError(err?.message || 'Login failed.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main>
      <section className="section simple-hero">
        <div className="container">
          <div className="section-head">
            <p className="eyebrow">Access</p>
            <h2>Sign in to Remote Care</h2>
            <p className="muted">Enter your work email and password to access the dashboard.</p>
          </div>

          <div className="live-panel" style={{ maxWidth: '500px', margin: '2rem auto' }}>
            <form className="live-form" onSubmit={handleLogin}>
              <label>
                Work email
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="jane@clinic.com"
                  autoComplete="email"
                  required
                />
              </label>
              <label>
                Password
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Enter password"
                  autoComplete="current-password"
                  required
                />
              </label>
              <label>
                Device info (optional)
                <input
                  type="text"
                  value={deviceInfo}
                  onChange={(e) => setDeviceInfo(e.target.value)}
                  placeholder="web-app"
                  autoComplete="off"
                />
              </label>

              <div className="live-actions">
                <button className="btn btn-primary" type="submit" disabled={loading}>
                  {loading ? 'Signing in...' : 'Sign in'}
                </button>
                <Link className="btn btn-outline" to="/">
                  Back
                </Link>
              </div>

              {error ? <p className="live-error">{error}</p> : null}

              <p className="muted auth-hint">
                Don't have an account? <Link to="/register">Create one</Link>
              </p>
            </form>
          </div>
        </div>
      </section>
    </main>
  )
}
