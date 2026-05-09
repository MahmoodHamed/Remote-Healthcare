import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'

const DEFAULT_API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000'

const roles = [
  {
    id: 'patient',
    title: 'Patient',
    roleName: 'Patient',
    description: 'Pair your watch and review your own vitals.',
  },
  {
    id: 'doctor',
    title: 'Doctor',
    roleName: 'Doctor',
    description: 'Monitor assigned patients and receive live alerts.',
  },
  {
    id: 'family',
    title: 'Family',
    roleName: 'Relative',
    description: 'Follow live vitals for your loved one.',
  },
]

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

export default function Register({ onRegisterSuccess }) {
  const navigate = useNavigate()
  const [activeRole, setActiveRole] = useState('patient')
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [password, setPassword] = useState('')
  const [licenseNumber, setLicenseNumber] = useState('')
  const [specialization, setSpecialization] = useState('General')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const activeRoleName = roles.find((role) => role.id === activeRole)?.roleName || 'Patient'

  const handleRegister = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError('')

    try {
      const base = DEFAULT_API_BASE.trim()
      if (!base || !fullName.trim() || !email.trim() || !phone.trim() || !password) {
        throw new Error('All fields are required.')
      }

      const registerUrl = new URL('/api/auth/register', base).toString()

      const response = await fetch(registerUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          fullName: fullName.trim(),
          email: email.trim(),
          phone: phone.trim(),
          password,
          role: activeRoleName,
          licenseNumber: activeRole === 'doctor' ? licenseNumber.trim() || null : null,
          specialization: activeRole === 'doctor' ? specialization.trim() || null : null,
        }),
      })

      if (!response.ok) {
        const message = await parseErrorMessage(response)
        throw new Error(message || `Register failed (${response.status}).`)
      }

      const data = await response.json()
      if (onRegisterSuccess) {
        onRegisterSuccess(data)
      }
      navigate('/dashboard')
    } catch (err) {
      setError(err?.message || 'Register failed.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main>
      <section className="section simple-hero">
        <div className="container">
          <div className="section-head">
            <p className="eyebrow">Get started</p>
            <h2>Create an account</h2>
            <p className="muted">Choose a role and create your account to start streaming vitals.</p>
          </div>

          <div className="role-grid">
            {roles.map((role) => (
              <button
                type="button"
                key={role.id}
                className={`role-card ${activeRole === role.id ? 'active' : ''}`}
                onClick={() => setActiveRole(role.id)}
              >
                <strong>{role.title}</strong>
                <span>{role.description}</span>
              </button>
            ))}
          </div>

          <div className="live-panel" style={{ maxWidth: '500px', margin: '2rem auto' }}>
            <form className="live-form" onSubmit={handleRegister}>
              <label>
                Full name
                <input
                  type="text"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  placeholder="Mahmood Job"
                  autoComplete="name"
                  required
                />
              </label>
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
                Phone
                <input
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="+9647700000000"
                  autoComplete="tel"
                  required
                />
              </label>
              <label>
                Password
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Create a strong password"
                  autoComplete="new-password"
                  required
                />
              </label>

              {activeRole === 'doctor' ? (
                <>
                  <label>
                    License number
                    <input
                      type="text"
                      value={licenseNumber}
                      onChange={(e) => setLicenseNumber(e.target.value)}
                      placeholder="MED-12345"
                      autoComplete="off"
                    />
                  </label>
                  <label>
                    Specialization
                    <input
                      type="text"
                      value={specialization}
                      onChange={(e) => setSpecialization(e.target.value)}
                      placeholder="General"
                      autoComplete="off"
                    />
                  </label>
                </>
              ) : null}

              <div className="live-actions">
                <button className="btn btn-primary" type="submit" disabled={loading}>
                  {loading ? 'Creating account...' : 'Register'}
                </button>
                <Link className="btn btn-outline" to="/">
                  Back
                </Link>
              </div>

              {error ? <p className="live-error">{error}</p> : null}

              <p className="muted auth-hint">
                Already have an account? <Link to="/login">Sign in</Link>
              </p>
            </form>
          </div>
        </div>
      </section>
    </main>
  )
}
