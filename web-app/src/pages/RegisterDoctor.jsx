import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../utils/api'
import { setAuthSession, homeRouteForRole } from '../utils/auth'

export default function RegisterDoctor({ onSignedIn }) {
  const navigate = useNavigate()
  const [form, setForm] = useState({
    fullName: '',
    email: '',
    phone: '',
    password: '',
    licenseNumber: '',
    specialization: '',
    hospitalName: '',
  })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const update = (key) => (e) => setForm({ ...form, [key]: e.target.value })

  const submit = async (event) => {
    event.preventDefault()
    setError('')
    setBusy(true)
    try {
      const response = await api.post('/api/auth/register/doctor', form, { auth: false })
      const user = setAuthSession(response)
      if (!user) throw new Error('Unexpected response from server.')
      onSignedIn?.(user)
      navigate(homeRouteForRole('Doctor'), { replace: true })
    } catch (err) {
      setError(err.message || 'Could not create your account.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth-shell">
      <aside className="auth-art">
        <div className="auth-art-content">
          <span className="pill">Clinician workspace</span>
          <h2>Care for many patients in real time.</h2>
          <p>Each of your patients gets a live dashboard. We push notifications and SignalR events the moment a vital crosses safe limits.</p>
        </div>
      </aside>

      <div className="auth-side">
        <div className="auth-card">
          <Link to="/" className="brand" style={{ marginBottom: '1.25rem' }}>
            <span className="brand-mark">RC</span>
            Remote Care
          </Link>
          <h1>Apply for clinician access</h1>
          <p className="auth-sub">Tell us about your practice. Your license is used for verification.</p>

          <form className="form" onSubmit={submit} noValidate>
            <label>
              Full name
              <input value={form.fullName} onChange={update('fullName')} required minLength={2} />
            </label>
            <div className="form-row">
              <label>
                Email
                <input type="email" value={form.email} onChange={update('email')} required autoComplete="email" />
              </label>
              <label>
                Phone
                <input type="tel" value={form.phone} onChange={update('phone')} required placeholder="+9647xxxxxxxxx" />
              </label>
            </div>
            <div className="form-row">
              <label>
                Medical license number
                <input value={form.licenseNumber} onChange={update('licenseNumber')} required minLength={3} />
              </label>
              <label>
                Specialization
                <input value={form.specialization} onChange={update('specialization')} required placeholder="Cardiology" />
              </label>
            </div>
            <label>
              Hospital / clinic <span className="form-hint">optional</span>
              <input value={form.hospitalName} onChange={update('hospitalName')} placeholder="e.g. Baghdad Medical City" />
            </label>
            <label>
              Password
              <input
                type="password"
                value={form.password}
                onChange={update('password')}
                required
                minLength={8}
                autoComplete="new-password"
              />
              <span className="form-hint">Must include an uppercase letter and a digit.</span>
            </label>
            {error && <div className="form-error">{error}</div>}
            <button className="btn btn-primary btn-block" disabled={busy}>
              {busy ? 'Creating account…' : 'Create doctor account'}
            </button>
          </form>

          <div className="auth-foot">
            Already approved? <Link to="/login?as=doctor">Sign in as doctor</Link>
          </div>
        </div>
      </div>
    </div>
  )
}
