import { Link } from 'react-router-dom'
import { homeRouteForRole } from '../utils/auth'

const features = [
  {
    icon: '⌚',
    title: 'Samsung Galaxy Watch 8',
    text: 'Live heart rate, SpO2, skin temperature, ECG, stress, sleep, blood pressure, BIA and fall detection — all in one pipeline.',
  },
  {
    icon: '⚡',
    title: 'Real-time streaming',
    text: 'MQTT pushes every reading from the watch through a TimescaleDB hypertable to your dashboard in milliseconds.',
  },
  {
    icon: '🛎️',
    title: 'Threshold alerts',
    text: 'Per-patient thresholds trigger push notifications, in-app alerts and an audit trail the moment a vital crosses safe limits.',
  },
  {
    icon: '🔐',
    title: 'Role-aware access',
    text: 'Patients see only their own data, doctors see assigned patients, the hospital admin sees and controls the whole platform.',
  },
  {
    icon: '🧠',
    title: 'Clinical context',
    text: 'Chronic diseases, allergies, medications and emergency contacts stay alongside live vitals so doctors can decide fast.',
  },
  {
    icon: '🌍',
    title: 'Cross-platform',
    text: 'Web for the clinic, Android for the patient and a Wear OS app on the watch — one source of truth.',
  },
]

const roles = [
  {
    key: 'admin',
    title: 'Hospital admin',
    summary: 'Manage every doctor, every patient and every assignment from one console.',
    bullets: ['View all doctors', 'Drill into a doctor’s patients', 'Assign or revoke care', 'Audit alerts & users'],
    cta: 'Admin sign in',
    link: '/admin/login',
  },
  {
    key: 'doctor',
    title: 'Doctor',
    summary: 'Watch every assigned patient stream live and react before things escalate.',
    bullets: ['See my patients live', 'Threshold-based alerts', 'Push notifications', 'Vitals timeline & ECG'],
    cta: 'Doctor sign in',
    link: '/login?as=doctor',
  },
  {
    key: 'patient',
    title: 'Patient',
    summary: 'Pair your Galaxy Watch, see your own vitals and stay safely connected to your doctor.',
    bullets: ['Live personal vitals', 'Watch setup walkthrough', 'Alerts on your phone', 'History of every reading'],
    cta: 'Create patient account',
    link: '/register/patient',
  },
]

export default function Landing({ profile }) {
  return (
    <div className="app-shell">
      <header className="public-nav">
        <div className="container public-nav-inner">
          <Link to="/" className="brand">
            <span className="brand-mark">RC</span>
            Remote Care
          </Link>
          <nav className="nav-links">
            <a href="#features">Features</a>
            <a href="#roles">For each role</a>
            <a href="#how">How it works</a>
          </nav>
          <div className="nav-cta">
            {profile ? (
              <Link to={homeRouteForRole(profile.role)} className="btn btn-primary btn-sm">
                Open my workspace
              </Link>
            ) : (
              <>
                <Link to="/login" className="btn btn-ghost btn-sm">Sign in</Link>
                <Link to="/register" className="btn btn-primary btn-sm">Get started</Link>
              </>
            )}
          </div>
        </div>
      </header>

      <main>
        <section className="hero">
          <div className="container hero-grid">
            <div>
              <span className="eyebrow">Remote Patient Monitoring</span>
              <h1>
                A clinical-grade hub for <span>connected health</span> — built around the Galaxy Watch 8.
              </h1>
              <p className="lead">
                Stream every sensor your wearable can produce — heart rate, SpO2, ECG, stress, sleep, skin
                temperature, BIA, falls and more — to a single hospital workspace where doctors act in real time.
              </p>
              <div className="hero-actions">
                <Link to="/register" className="btn btn-primary">Start as patient or doctor</Link>
                <Link to="/admin/login" className="btn btn-outline">Admin sign in</Link>
              </div>
            </div>

            <div className="hero-preview" aria-hidden="true">
              <div className="preview-head">
                <strong>Patient · ABC123</strong>
                <span className="preview-pill">Live</span>
              </div>
              <div className="preview-pulse" />
              <div className="preview-stats">
                <div className="preview-stat">
                  <span className="label">Heart rate</span>
                  <span><span className="value">82</span> <span className="unit">bpm</span></span>
                </div>
                <div className="preview-stat">
                  <span className="label">SpO2</span>
                  <span><span className="value">98</span> <span className="unit">%</span></span>
                </div>
                <div className="preview-stat">
                  <span className="label">Skin temp.</span>
                  <span><span className="value">36.4</span> <span className="unit">°C</span></span>
                </div>
                <div className="preview-stat danger">
                  <span className="label">Stress</span>
                  <span><span className="value">74</span> <span className="unit">/100</span></span>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section className="section" id="features">
          <div className="container">
            <div className="section-head">
              <span className="eyebrow">What you get</span>
              <h2>Every sensor, every alert, in one workspace</h2>
              <p>One platform from the watch to the doctor — covering data ingestion, alerting, dashboards and chat.</p>
            </div>

            <div className="feature-grid">
              {features.map((feature) => (
                <article className="feature-card" key={feature.title}>
                  <span className="feature-icon" aria-hidden="true">{feature.icon}</span>
                  <h3>{feature.title}</h3>
                  <p>{feature.text}</p>
                </article>
              ))}
            </div>
          </div>
        </section>

        <section className="section" id="roles" style={{ background: 'linear-gradient(180deg, transparent, var(--surface-2))' }}>
          <div className="container">
            <div className="section-head">
              <span className="eyebrow">Three workspaces, one platform</span>
              <h2>Each role gets a dedicated experience</h2>
              <p>The admin runs the hospital. The doctor runs their patients. The patient runs their own watch.</p>
            </div>

            <div className="role-grid">
              {roles.map((role) => (
                <article className={`role-card ${role.key}`} key={role.key}>
                  <span className={`tag ${role.key}`}>{role.title}</span>
                  <h3>{role.title}</h3>
                  <p>{role.summary}</p>
                  <ul>
                    {role.bullets.map((b) => <li key={b}>{b}</li>)}
                  </ul>
                  <Link className="btn btn-outline" to={role.link}>{role.cta}</Link>
                </article>
              ))}
            </div>
          </div>
        </section>

        <section className="section" id="how">
          <div className="container">
            <div className="section-head">
              <span className="eyebrow">How it works</span>
              <h2>From watch to doctor in three steps</h2>
            </div>
            <div className="feature-grid">
              <article className="feature-card">
                <span className="feature-icon">1</span>
                <h3>Pair your watch</h3>
                <p>Sign up as a patient, install the Wear OS app, and enter the 6-character patient ID once.</p>
              </article>
              <article className="feature-card">
                <span className="feature-icon">2</span>
                <h3>Stream everything</h3>
                <p>Every sensor the watch supports — continuous and on-demand — is published over MQTT to the backend.</p>
              </article>
              <article className="feature-card">
                <span className="feature-icon">3</span>
                <h3>Your doctor sees it live</h3>
                <p>Doctors get a real-time dashboard, alerts and push notifications when something needs attention.</p>
              </article>
            </div>
          </div>
        </section>
      </main>

      <footer className="footer-public">
        <div className="container" style={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem' }}>
          <span>© {new Date().getFullYear()} Remote Care. Secure remote patient monitoring.</span>
          <span>
            <Link to="/login">Patient & doctor sign in</Link>
            {' · '}
            <Link to="/admin/login">Admin sign in</Link>
          </span>
        </div>
      </footer>
    </div>
  )
}
