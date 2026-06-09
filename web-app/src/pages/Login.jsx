import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import ThemeToggle from '../components/ThemeToggle'
import { api } from '../utils/api'
import { setAuthSession, homeRouteForRole } from '../utils/auth'

export default function Login({ onSignedIn }) {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const presetRole = params.get('as') === 'doctor' ? 'doctor' : 'patient'

  const [tab, setTab] = useState(presetRole)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (event) => {
    event.preventDefault()
    if (busy) return
    setError('')

    const trimmedEmail = email.trim()
    const trimmedPassword = password

    if (!trimmedEmail || !trimmedPassword) {
      setError('Please enter your email and password.')
      return
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmedEmail)) {
      setError('Please enter a valid email address.')
      return
    }

    setBusy(true)
    try {
      const response = await api.post(
        '/api/auth/login',
        { email: trimmedEmail, password: trimmedPassword, deviceInfo: 'web' },
        { auth: false },
      )
      const user = setAuthSession(response)
      if (!user) throw new Error('Unexpected response from server.')
      if (user.role === 'Admin') {
        setError('Use the admin sign-in page to access the hospital console.')
        return
      }
      onSignedIn?.(user)
      navigate(homeRouteForRole(user.role), { replace: true })
    } catch (err) {
      if (err.status === 401) {
        setError('Invalid email or password.')
      } else {
        setError(err.message || 'Sign-in failed.')
      }
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth-shell">
      <div className="auth-side">
        <ThemeToggle className="auth-theme-toggle" />
        <div className="auth-card">
          <Link to="/" className="brand" style={{ marginBottom: '1.25rem' }}>
            <span className="brand-mark">RC</span>
            Remote Care
          </Link>
          <h1>Welcome back</h1>
          <p className="auth-sub">Sign in to your patient or doctor workspace.</p>

          <div className="auth-tabs" role="tablist">
            <button
              type="button"
              className={`auth-tab ${tab === 'patient' ? 'active' : ''}`}
              role="tab"
              aria-selected={tab === 'patient'}
              onClick={() => setTab('patient')}
            >
              Patient
            </button>
            <button
              type="button"
              className={`auth-tab ${tab === 'doctor' ? 'active' : ''}`}
              role="tab"
              aria-selected={tab === 'doctor'}
              onClick={() => setTab('doctor')}
            >
              Doctor
            </button>
          </div>

          <form className="form" onSubmit={submit} noValidate>
            <label>
              Email
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                placeholder="you@example.com"
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
              {busy ? 'Signing in…' : `Sign in as ${tab}`}
            </button>
          </form>

          <div className="auth-foot">
            {tab === 'doctor' ? (
              <>Need a doctor account? <Link to="/register/doctor">Apply for access</Link></>
            ) : (
              <>New here? <Link to="/register/patient">Create a patient account</Link></>
            )}
            <div style={{ marginTop: '0.5rem' }}>
              Hospital administrator? <Link to="/admin/login">Admin sign in</Link>
            </div>
          </div>
        </div>
      </div>

      <aside className="auth-art">
        <div className="auth-art-content">
          <span className="pill">Clinician console</span>
          <h2>Care that follows the patient everywhere.</h2>
          <p>
            Doctors and patients use the same secure platform. The data your watch streams shows up in the doctor’s
            workspace in real time, with alerts when thresholds are crossed.
          </p>
        </div>
      </aside>
    </div>
  )
}
