import { Outlet } from 'react-router-dom'
import RoleShell from './RoleShell.jsx'

const navItems = [
  { to: '/doctor/dashboard', label: 'Dashboard', icon: '📊' },
  { to: '/doctor/patients', label: 'My patients', icon: '👥' },
  { to: '/doctor/chat', label: 'Messages', icon: '💬' },
  { to: '/doctor/notifications', label: 'Notifications', icon: '🔔' },
]

export default function DoctorLayout({ profile, onLogout }) {
  return (
    <RoleShell
      profile={profile}
      onLogout={onLogout}
      roleClass="role-doctor"
      brand={{ title: 'Clinician workspace', subtitle: 'Doctor' }}
      nav={navItems}
    >
      <Outlet />
    </RoleShell>
  )
}
