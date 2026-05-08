import { useState } from 'react'
import './App.css'

const stats = [
  { label: 'Vitals streamed daily', value: '2.4M' },
  { label: 'Average alert latency', value: '1.9s' },
  { label: 'Active care teams', value: '620+' },
  { label: 'Uptime target', value: '99.99%' },
]

const features = [
  {
    title: 'Live vitals streaming',
    body: 'Receive heart rate, SpO2, BP, temperature, and activity data in real time via MQTT and SignalR.',
    tag: 'Realtime',
  },
  {
    title: 'Smart alert thresholds',
    body: 'Custom thresholds trigger critical alerts instantly so care teams can intervene sooner.',
    tag: 'Alerts',
  },
  {
    title: 'Patient timeline',
    body: 'Historical vitals are stored in TimescaleDB and visualized for clinical review.',
    tag: 'Trends',
  },
  {
    title: 'Multi-role portals',
    body: 'Doctor, patient, and family views keep everyone aligned on the care plan.',
    tag: 'Access',
  },
  {
    title: 'Secure messaging',
    body: 'Built-in chat keeps discussions in one place with audit-friendly retention.',
    tag: 'Collaboration',
  },
  {
    title: 'Device health signals',
    body: 'Battery level, wear detection, and device status reduce blind spots.',
    tag: 'Reliability',
  },
]

const steps = [
  {
    step: '01',
    title: 'Connect the smartwatch',
    body: 'Register devices to the patient profile and begin secure streaming.',
  },
  {
    step: '02',
    title: 'Validate and ingest',
    body: 'Vitals flow through validation, normalization, and storage pipelines.',
  },
  {
    step: '03',
    title: 'Detect and alert',
    body: 'Rules and thresholds trigger alerts for care teams and family members.',
  },
  {
    step: '04',
    title: 'Collaborate in real time',
    body: 'Chat, dashboards, and notifications keep everyone informed.',
  },
]

const security = [
  'JWT authentication with refresh tokens and role-based access control.',
  'Encrypted data in transit (TLS) and structured audit logs.',
  'Isolated storage for patient data with strict access boundaries.',
  'Alerting and anomaly detection for device health issues.',
  'Dedicated MQTT broker with optional TLS and credentials.',
  'Secure file storage for attachments and clinical documentation.',
]

const portals = [
  {
    title: 'Doctor Portal',
    body: 'Clinical dashboards, alert triage, and patient timelines.',
    action: 'Sign in for clinicians',
  },
  {
    title: 'Patient App',
    body: 'Personal vitals view, reminders, and care messages.',
    action: 'Sign in for patients',
  },
  {
    title: 'Family View',
    body: 'Receive alerts and stay connected to the care team.',
    action: 'Sign in for family',
  },
]

const faqs = [
  {
    q: 'Which smartwatch devices are supported?',
    a: 'The platform is built for modern wearables that can stream heart rate, SpO2, and activity metrics. Device onboarding is handled per deployment.',
  },
  {
    q: 'How is data delivered to the mobile app?',
    a: 'Vitals are stored in the backend and delivered through secure REST APIs and real-time SignalR streams.',
  },
  {
    q: 'Can we configure alert thresholds per patient?',
    a: 'Yes. Thresholds are stored per patient and can be adjusted by clinicians.',
  },
  {
    q: 'Is the system compliant with healthcare security standards?',
    a: 'The platform follows best practices with encryption, access control, and audit logging. Specific compliance can be configured per region.',
  },
  {
    q: 'How do patients and families sign in?',
    a: 'Accounts are created by administrators. Each role receives secure access to their portal and mobile experience.',
  },
]

function App() {
  const [menuOpen, setMenuOpen] = useState(false)
  const [openFaq, setOpenFaq] = useState(0)
  const [submitted, setSubmitted] = useState(false)

  const handleSubmit = (event) => {
    event.preventDefault()
    setSubmitted(true)
  }

  const year = new Date().getFullYear()

  return (
    <div className={`page ${menuOpen ? 'menu-open' : ''}`}>
      <header className="nav">
        <div className="container nav-inner">
          <a className="brand" href="#top" aria-label="Remote Care">
            <span className="brand-mark">RC</span>
            Remote Care
          </a>
          <nav className="nav-links">
            <a href="#features">Features</a>
            <a href="#how">How it works</a>
            <a href="#dashboard">Dashboard</a>
            <a href="#security">Security</a>
            <a href="#faq">FAQ</a>
            <a href="#contact">Contact</a>
          </nav>
          <div className="nav-cta">
            <a className="btn btn-ghost" href="#contact">Request demo</a>
            <a className="btn btn-primary" href="#access">Sign in</a>
          </div>
          <button
            className="nav-toggle"
            type="button"
            aria-expanded={menuOpen}
            aria-controls="nav-drawer"
            aria-label="Toggle navigation"
            onClick={() => setMenuOpen((prev) => !prev)}
          >
            <span />
            <span />
          </button>
        </div>
        <div id="nav-drawer" className="nav-drawer">
          <a href="#features" onClick={() => setMenuOpen(false)}>Features</a>
          <a href="#how" onClick={() => setMenuOpen(false)}>How it works</a>
          <a href="#dashboard" onClick={() => setMenuOpen(false)}>Dashboard</a>
          <a href="#security" onClick={() => setMenuOpen(false)}>Security</a>
          <a href="#faq" onClick={() => setMenuOpen(false)}>FAQ</a>
          <a href="#contact" onClick={() => setMenuOpen(false)}>Contact</a>
          <a className="btn btn-primary" href="#access" onClick={() => setMenuOpen(false)}>
            Sign in
          </a>
        </div>
      </header>

      <main>
        <section className="hero" id="top">
          <div className="container hero-grid">
            <div className="hero-copy reveal" style={{ '--d': '0ms' }}>
              <span className="pill">Remote patient monitoring platform</span>
              <h1>Real-time monitoring for every patient, every minute.</h1>
              <p>
                Remote Care connects smartwatch data to doctors, patients, and families.
                Stream vitals securely, trigger alerts instantly, and deliver care insights from one dashboard.
              </p>
              <div className="hero-actions">
                <a className="btn btn-primary" href="#contact">Request demo</a>
                <a className="btn btn-ghost" href="#access">Sign in to portal</a>
              </div>
              <div className="hero-meta">
                <span>MQTT ingestion</span>
                <span>SignalR live updates</span>
                <span>FHIR-ready exports</span>
              </div>
            </div>
            <div className="hero-panel reveal" style={{ '--d': '160ms' }}>
              <div className="panel-header">
                <div>
                  <p className="panel-title">Patient 24 - Live session</p>
                  <p className="panel-sub">Updated just now</p>
                </div>
                <span className="badge">Live</span>
              </div>
              <div className="panel-grid">
                <div className="metric">
                  <span>Heart rate</span>
                  <strong>78 bpm</strong>
                  <small>Stable</small>
                </div>
                <div className="metric">
                  <span>SpO2</span>
                  <strong>98%</strong>
                  <small>Normal</small>
                </div>
                <div className="metric">
                  <span>Temperature</span>
                  <strong>36.8 C</strong>
                  <small>Normal</small>
                </div>
                <div className="metric">
                  <span>Activity</span>
                  <strong>1,480 steps</strong>
                  <small>Today</small>
                </div>
              </div>
              <div className="panel-chart">
                <div className="chart-line"></div>
                <div className="chart-line"></div>
                <div className="chart-line"></div>
              </div>
            </div>
          </div>
        </section>

        <section className="stats">
          <div className="container stats-grid">
            {stats.map((item) => (
              <div className="stat" key={item.label}>
                <strong>{item.value}</strong>
                <span>{item.label}</span>
              </div>
            ))}
          </div>
        </section>

        <section className="section" id="features">
          <div className="container">
            <div className="section-head">
              <p className="eyebrow">Platform features</p>
              <h2>Everything your care team needs to stay ahead.</h2>
              <p>
                Built for clinicians and care coordinators, Remote Care keeps data, alerts, and conversations in one place.
              </p>
            </div>
            <div className="card-grid">
              {features.map((feature) => (
                <article className="card reveal" style={{ '--d': '80ms' }} key={feature.title}>
                  <span className="tag">{feature.tag}</span>
                  <h3>{feature.title}</h3>
                  <p>{feature.body}</p>
                </article>
              ))}
            </div>
          </div>
        </section>

        <section className="section alt" id="how">
          <div className="container">
            <div className="section-head">
              <p className="eyebrow">How it works</p>
              <h2>From device to decision in four clear steps.</h2>
            </div>
            <div className="step-grid">
              {steps.map((step) => (
                <div className="step-card reveal" style={{ '--d': '120ms' }} key={step.step}>
                  <div className="step-number">{step.step}</div>
                  <h3>{step.title}</h3>
                  <p>{step.body}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section className="section" id="dashboard">
          <div className="container">
            <div className="section-head">
              <p className="eyebrow">Dashboard</p>
              <h2>Actionable views for clinicians and coordinators.</h2>
            </div>
            <div className="shot-grid">
              <div className="shot reveal" style={{ '--d': '80ms' }}>
                <div className="shot-label">Vitals Timeline</div>
                <div className="shot-body">
                  <div className="shot-line"></div>
                  <div className="shot-line"></div>
                  <div className="shot-line"></div>
                </div>
              </div>
              <div className="shot reveal" style={{ '--d': '160ms' }}>
                <div className="shot-label">Alerts Triage</div>
                <div className="shot-body">
                  <div className="shot-pill">High HR</div>
                  <div className="shot-pill">Low SpO2</div>
                  <div className="shot-pill">Fall detected</div>
                </div>
              </div>
              <div className="shot reveal" style={{ '--d': '240ms' }}>
                <div className="shot-label">Patient Profile</div>
                <div className="shot-body">
                  <div className="shot-avatar"></div>
                  <div className="shot-block"></div>
                  <div className="shot-block"></div>
                </div>
              </div>
            </div>
            <p className="subtle">Screenshots are illustrative. Customize views per clinic.</p>
          </div>
        </section>

        <section className="section alt" id="security">
          <div className="container security-grid">
            <div className="security-copy">
              <p className="eyebrow">Security and compliance</p>
              <h2>Designed for clinical trust.</h2>
              <p>
                Security is embedded across ingestion, storage, and delivery so that sensitive health data is always protected.
              </p>
              <div className="security-badges">
                <span className="badge">TLS encrypted</span>
                <span className="badge">Role based access</span>
                <span className="badge">Audit ready</span>
              </div>
            </div>
            <ul className="security-list">
              {security.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          </div>
        </section>

        <section className="section" id="access">
          <div className="container">
            <div className="section-head">
              <p className="eyebrow">Portal access</p>
              <h2>Sign in experiences for every role.</h2>
            </div>
            <div className="card-grid">
              {portals.map((portal) => (
                <article className="card" key={portal.title}>
                  <h3>{portal.title}</h3>
                  <p>{portal.body}</p>
                  <a className="btn btn-ghost" href="#contact">{portal.action}</a>
                </article>
              ))}
            </div>
          </div>
        </section>

        <section className="section alt" id="faq">
          <div className="container">
            <div className="section-head">
              <p className="eyebrow">FAQ</p>
              <h2>Common questions from care teams.</h2>
            </div>
            <div className="faq">
              {faqs.map((item, index) => (
                <div className={`faq-item ${openFaq === index ? 'open' : ''}`} key={item.q}>
                  <button type="button" onClick={() => setOpenFaq(index)} aria-expanded={openFaq === index}>
                    <span>{item.q}</span>
                    <span className="faq-icon">+</span>
                  </button>
                  <div className="faq-body">
                    <p>{item.a}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section className="section" id="contact">
          <div className="container contact-grid">
            <div>
              <p className="eyebrow">Contact</p>
              <h2>Ready to bring Remote Care to your clinic?</h2>
              <p>
                Tell us about your organization. We will set up a tailored demo and discuss onboarding for patients and care teams.
              </p>
              <div className="contact-card">
                <strong>Care operations</strong>
                <span>care@remote-care.tech</span>
                <span>24/7 monitoring support</span>
              </div>
            </div>
            <form className="form" onSubmit={handleSubmit}>
              <label>
                Full name
                <input type="text" name="name" placeholder="Dr. Jane Ahmed" required />
              </label>
              <label>
                Work email
                <input type="email" name="email" placeholder="jane@clinic.com" required />
              </label>
              <label>
                Organization
                <input type="text" name="org" placeholder="Northside Medical" />
              </label>
              <label>
                Message
                <textarea name="message" rows="4" placeholder="Tell us about your patient monitoring goals"></textarea>
              </label>
              <button className="btn btn-primary" type="submit">Request demo</button>
              {submitted ? <p className="form-note">Thanks. We will be in touch shortly.</p> : null}
            </form>
          </div>
        </section>
      </main>

      <footer className="footer">
        <div className="container footer-inner">
          <div>
            <div className="brand compact">
              <span className="brand-mark">RC</span>
              Remote Care
            </div>
            <p>Real-time remote patient monitoring for clinicians, patients, and families.</p>
          </div>
          <div className="footer-links">
            <a href="#features">Features</a>
            <a href="#security">Security</a>
            <a href="#contact">Contact</a>
          </div>
          <div className="footer-meta">© {year} Remote Care. All rights reserved.</div>
        </div>
      </footer>
    </div>
  )
}

export default App
