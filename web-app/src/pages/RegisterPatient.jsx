import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../utils/api'
import { setAuthSession, homeRouteForRole } from '../utils/auth'

export default function RegisterPatient({ onSignedIn }) {
  const navigate = useNavigate()
  const [form, setForm] = useState({ fullName: '', email: '', phone: '', password: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const update = (key) => (e) => setForm({ ...form, [key]: e.target.value })

  const submit = async (event) => {
    event.preventDefault()
    setError('')
    setBusy(true)
    try {
      const response = await api.post('/api/auth/register/patient', form, { auth: false })
      const user = setAuthSession(response)
      if (!user) throw new Error('Unexpected response from server.')
      onSignedIn?.(user)
      navigate(homeRouteForRole('Patient'), { replace: true })
    } catch (err) {
      setError(err.message || 'Could not create your account.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth-shell">
      <div className="auth-side">
        <div className="auth-card">
          <Link to="/" className="brand" style={{ marginBottom: '1.25rem' }}>
            <span className="brand-mark">RC</span>
            Remote Care
          </Link>
          <h1>Create a patient account</h1>
          <p className="auth-sub">Sign up to pair your Galaxy Watch and start streaming your vitals to your doctor.</p>

          <form className="form" onSubmit={submit} noValidate>
            <label>
              Full name
              <input value={form.fullName} onChange={update('fullName')} required minLength={2} />
            </label>
            <label>
              Email
              <input type="email" value={form.email} onChange={update('email')} required autoComplete="email" />
            </label>
            <label>
              Phone number
              <input type="tel" value={form.phone} onChange={update('phone')} required placeholder="+9647xxxxxxxxx" />
            </label>
            <label>
              Password
              <input
                type="password"
                value={form.password}
                onChange={update('password')}
                required
                minLength={8}
                placeholder="At least 8 characters"
                autoComplete="new-password"
              />
              <span className="form-hint">Must include an uppercase letter and a digit.</span>
            </label>
            {error && <div className="form-error">{error}</div>}
            <button className="btn btn-primary btn-block" disabled={busy}>
              {busy ? 'Creating account…' : 'Create patient account'}
            </button>
          </form>

          <div className="auth-foot">
            Already have an account? <Link to="/login">Sign in</Link>
          </div>
        </div>
      </div>
      <aside className="auth-art">
        <div className="auth-art-content">
          <span className="pill">Patient workspace</span>
          <h2>Your watch, your data, your doctor.</h2>
          <p>You stay in control. Pair your watch in one screen and your doctor sees every vital in real time, with alerts when something needs attention.</p>
        </div>
      </aside>
    </div>
  )
}
