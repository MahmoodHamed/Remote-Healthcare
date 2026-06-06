import { useEffect, useState } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import './App.css'
import { ThemeProvider } from './context/ThemeContext'

import { readProfile, logout as clearSession, homeRouteForRole } from './utils/auth'
import { ensureValidSession, getAccessToken } from './utils/authSession'
import { getApiBase } from './utils/apiBase'

import Landing from './pages/Landing.jsx'
import Login from './pages/Login.jsx'
import RegisterPicker from './pages/RegisterPicker.jsx'
import RegisterPatient from './pages/RegisterPatient.jsx'
import RegisterDoctor from './pages/RegisterDoctor.jsx'
import AdminLogin from './pages/AdminLogin.jsx'

import AdminLayout from './layouts/AdminLayout.jsx'
import DoctorLayout from './layouts/DoctorLayout.jsx'
import PatientLayout from './layouts/PatientLayout.jsx'

import AdminOverview from './pages/admin/AdminOverview.jsx'
import AdminDoctors from './pages/admin/AdminDoctors.jsx'
import AdminDoctorDetail from './pages/admin/AdminDoctorDetail.jsx'
import AdminPatients from './pages/admin/AdminPatients.jsx'
import AdminUsers from './pages/admin/AdminUsers.jsx'
import AdminAuditLog from './pages/admin/AdminAuditLog.jsx'

import DoctorDashboard from './pages/doctor/DoctorDashboard.jsx'
import DoctorPatients from './pages/doctor/DoctorPatients.jsx'
import DoctorPatientDetail from './pages/doctor/DoctorPatientDetail.jsx'
import DoctorNotifications from './pages/doctor/DoctorNotifications.jsx'
import DoctorConversations from './pages/doctor/DoctorConversations.jsx'

import PatientDashboard from './pages/patient/PatientDashboard.jsx'
import PatientWatch from './pages/patient/PatientWatch.jsx'
import PatientNotifications from './pages/patient/PatientNotifications.jsx'
import PatientConversations from './pages/patient/PatientConversations.jsx'

const AuthContext = ({ children }) => children

function ProtectedRoute({ role, profile, children }) {
  if (!profile) return <Navigate to="/login" replace />
  if (role && profile.role !== role) return <Navigate to={homeRouteForRole(profile.role)} replace />
  return children
}

function AdminProtected({ profile, children }) {
  if (!profile) return <Navigate to="/admin/login" replace />
  if (profile.role !== 'Admin') return <Navigate to={homeRouteForRole(profile.role)} replace />
  return children
}

export default function App() {
  const [profile, setProfile] = useState(() => readProfile())

  useEffect(() => {
    const handler = () => setProfile(readProfile())
    window.addEventListener('storage', handler)
    window.addEventListener('auth:tokens-updated', handler)
    return () => {
      window.removeEventListener('storage', handler)
      window.removeEventListener('auth:tokens-updated', handler)
    }
  }, [])

  useEffect(() => {
    const onExpired = () => {
      clearSession()
      setProfile(null)
    }
    window.addEventListener('auth:session-expired', onExpired)
    return () => window.removeEventListener('auth:session-expired', onExpired)
  }, [])

  useEffect(() => {
    const session = readProfile()
    if (!session || !getAccessToken()) return undefined
    let active = true
    ensureValidSession(getApiBase()).then((ok) => {
      if (active && !ok) {
        clearSession()
        setProfile(null)
      }
    })
    return () => { active = false }
  }, [])

  const handleSignedIn = (user) => setProfile(user)
  const handleLogout = () => {
    clearSession()
    setProfile(null)
  }

  return (
    <ThemeProvider>
    <BrowserRouter>
      <AuthContext>
        <Routes>
          {/* Public */}
          <Route path="/" element={<Landing profile={profile} />} />
          <Route path="/login" element={<Login onSignedIn={handleSignedIn} />} />
          <Route path="/register" element={<RegisterPicker />} />
          <Route path="/register/patient" element={<RegisterPatient onSignedIn={handleSignedIn} />} />
          <Route path="/register/doctor" element={<RegisterDoctor onSignedIn={handleSignedIn} />} />
          <Route path="/admin/login" element={<AdminLogin onSignedIn={handleSignedIn} />} />

          {/* Admin */}
          <Route
            path="/admin"
            element={
              <AdminProtected profile={profile}>
                <AdminLayout profile={profile} onLogout={handleLogout} />
              </AdminProtected>
            }
          >
            <Route index element={<Navigate to="dashboard" replace />} />
            <Route path="dashboard" element={<AdminOverview />} />
            <Route path="doctors" element={<AdminDoctors />} />
            <Route path="doctors/:doctorId" element={<AdminDoctorDetail />} />
            <Route path="patients" element={<AdminPatients />} />
            <Route path="users" element={<AdminUsers />} />
            <Route path="audit" element={<AdminAuditLog />} />
          </Route>

          {/* Doctor */}
          <Route
            path="/doctor"
            element={
              <ProtectedRoute role="Doctor" profile={profile}>
                <DoctorLayout profile={profile} onLogout={handleLogout} />
              </ProtectedRoute>
            }
          >
            <Route index element={<Navigate to="dashboard" replace />} />
            <Route path="dashboard" element={<DoctorDashboard />} />
            <Route path="patients" element={<DoctorPatients />} />
            <Route path="patients/:patientUserId" element={<DoctorPatientDetail />} />
            <Route path="chat" element={<DoctorConversations />} />
            <Route path="notifications" element={<DoctorNotifications />} />
          </Route>

          {/* Patient */}
          <Route
            path="/patient"
            element={
              <ProtectedRoute role="Patient" profile={profile}>
                <PatientLayout profile={profile} onLogout={handleLogout} />
              </ProtectedRoute>
            }
          >
            <Route index element={<Navigate to="dashboard" replace />} />
            <Route path="dashboard" element={<PatientDashboard />} />
            <Route path="watch" element={<PatientWatch />} />
            <Route path="chat" element={<PatientConversations />} />
            <Route path="notifications" element={<PatientNotifications />} />
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthContext>
    </BrowserRouter>
    </ThemeProvider>
  )
}
