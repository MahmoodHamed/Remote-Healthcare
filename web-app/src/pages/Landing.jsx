import { Link } from 'react-router-dom'

export default function Landing() {
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
              <h3>Multiple roles</h3>
              <p>Support for patients, doctors, family members, and administrators.</p>
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
    </main>
  )
}
