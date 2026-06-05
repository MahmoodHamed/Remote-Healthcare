import { Link } from 'react-router-dom'
import ThemeToggle from '../components/ThemeToggle'

export default function RegisterPicker() {
  return (
    <div className="app-shell">
      <header className="public-nav">
        <div className="container public-nav-inner">
          <Link to="/" className="brand">
            <span className="brand-mark">RC</span>
            Remote Care
          </Link>
          <div className="nav-cta">
            <ThemeToggle />
            <Link to="/login" className="btn btn-ghost btn-sm">Already have an account?</Link>
          </div>
        </div>
      </header>

      <main className="section">
        <div className="container">
          <div className="section-head">
            <span className="eyebrow">Create your account</span>
            <h2>How will you use Remote Care?</h2>
            <p>Pick the role that matches you — each role has its own dedicated workspace.</p>
          </div>

          <div className="role-grid">
            <article className="role-card patient">
              <span className="tag patient">Patient</span>
              <h3>I want to be monitored</h3>
              <p>Sign up, pair your Galaxy Watch and let your doctor follow your vitals safely.</p>
              <ul>
                <li>Free for patients</li>
                <li>Live vitals dashboard</li>
                <li>Direct line to your doctor</li>
              </ul>
              <Link to="/register/patient" className="btn btn-primary">Create patient account</Link>
            </article>

            <article className="role-card doctor">
              <span className="tag doctor">Doctor</span>
              <h3>I am a clinician</h3>
              <p>Apply for clinician access. You can monitor your patients in real time once your license is verified.</p>
              <ul>
                <li>License number required</li>
                <li>Workspace for your patients</li>
                <li>Threshold-based alerts</li>
              </ul>
              <Link to="/register/doctor" className="btn btn-primary">Apply as doctor</Link>
            </article>

            <article className="role-card admin">
              <span className="tag admin">Hospital admin</span>
              <h3>I run the hospital</h3>
              <p>Hospital administrators are created by the platform team. Use the dedicated admin sign-in.</p>
              <ul>
                <li>Controlled provisioning</li>
                <li>Audit-ready user management</li>
                <li>Doctor-patient assignments</li>
              </ul>
              <Link to="/admin/login" className="btn btn-outline">Admin sign in</Link>
            </article>
          </div>
        </div>
      </main>
    </div>
  )
}
