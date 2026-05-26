import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../utils/api'
import { setAuthSession } from '../utils/auth'

export default function AdminLogin({ onSignedIn }) {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (event) => {
    event.preventDefault()
    setError('')
    setBusy(true)
    try {
      const response = await api.post(
        '/api/auth/admin/login',
        { email, password, deviceInfo: 'web-admin' },
        { auth: false },
      )
      const user = setAuthSession(response)
      if (!user) throw new Error('Unexpected response from server.')
      onSignedIn?.(user)
      navigate('/admin/dashboard', { replace: true })
    } catch (err) {
      setError(err.message || 'Sign-in failed.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth-shell">
      <aside className="auth-art admin">
        <div className="auth-art-content">
          <span className="pill">Hospital console</span>
          <h2>Run the whole hospital from one place.</h2>
          <p>
            See every doctor, every patient and every assignment. Move patients between doctors, monitor alerts and
            manage staff in seconds.
          </p>
        </div>
      </aside>
      <div className="auth-side">
        <div className="auth-card">
          <Link to="/" className="brand" style={{ marginBottom: '1.25rem' }}>
            <span className="brand-mark" style={{ background: 'linear-gradient(135deg, #7c3aed, #4c1d95)' }}>RC</span>
            Remote Care · Admin
          </Link>
          <h1>Hospital administrator</h1>
          <p className="auth-sub">This portal is reserved for hospital staff with elevated privileges.</p>

          <form className="form" onSubmit={submit} noValidate>
            <label>
              Email
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                placeholder="admin@hospital.com"
                autoComplete="email"
              />
            </label>
            <label>
              Password
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                placeholder="Your password"
                autoComplete="current-password"
              />
            </label>
            {error && <div className="form-error">{error}</div>}
            <button className="btn btn-primary btn-block" disabled={busy}>
              {busy ? 'Signing in…' : 'Enter admin console'}
            </button>
          </form>

          <div className="auth-foot">
            Not an admin? <Link to="/login">Patient & doctor sign in</Link>
          </div>
        </div>
      </div>
    </div>
  )
}
