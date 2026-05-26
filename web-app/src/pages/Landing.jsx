import { Link } from 'react-router-dom'

export default function Landing() {
  const roleHighlights = [
    {
      title: 'Admin',
      text: 'Manage users, control access, and keep the platform workflow organized.',
    },
    {
      title: 'Doctor',
      text: 'Track assigned patients in real time and spot risk trends early.',
    },
    {
      title: 'Patient',
      text: 'Follow your own live health readings with a simple clear dashboard.',
    },
  ]

  return (
    <main>
      <section className="section simple-hero">
        <div className="container">
          <div className="section-head">
            <p className="eyebrow">Remote Care</p>
            <h1>Remote patient monitoring made simple</h1>
            <p>
              Securely stream live vitals from wearable devices to doctors and family. Real-time alerts,
              easy device pairing, and centralized patient management.
            </p>
            <div className="hero-ctas">
              <Link className="btn btn-primary" to="/login">
                Sign in
              </Link>
              <Link className="btn btn-outline" to="/register">
                Register
              </Link>
            </div>
          </div>

          <div className="hero-features">
            <div className="feature-card">
              <h3>Real-time vitals</h3>
              <p>Stream heart rate, SpO2, blood pressure, and more from wearables instantly.</p>
            </div>
            <div className="feature-card">
              <h3>Role-based experience</h3>
              <p>Different UI workflow for admin, doctor, and patient users.</p>
            </div>
            <div className="feature-card">
              <h3>Smart alerts</h3>
              <p>Automatic notifications when vitals exceed configured thresholds.</p>
            </div>
            <div className="feature-card">
              <h3>Secure & HIPAA-ready</h3>
              <p>Enterprise-grade security with JWT authentication and encrypted connections.</p>
            </div>
          </div>
        </div>
      </section>

      <section className="section alt">
        <div className="container">
          <div className="section-head">
            <p className="eyebrow">Role spaces</p>
            <h2>Each user gets a focused interface</h2>
            <p>Work faster with pages designed around what each role needs most.</p>
          </div>
          <div className="quick-grid">
            {roleHighlights.map((item) => (
              <article className="workspace-card" key={item.title}>
                <h4>{item.title}</h4>
                <p className="muted">{item.text}</p>
              </article>
            ))}
          </div>
        </div>
      </section>
    </main>
  )
}
