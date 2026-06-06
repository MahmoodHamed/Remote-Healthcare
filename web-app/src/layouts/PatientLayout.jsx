import { Outlet } from 'react-router-dom'
import RoleShell from './RoleShell.jsx'

const navItems = [
  { to: '/patient/dashboard', label: 'My vitals', icon: '❤️' },
  { to: '/patient/watch', label: 'My watch', icon: '⌚' },
  { to: '/patient/chat', label: 'Messages', icon: '💬' },
  { to: '/patient/notifications', label: 'Notifications', icon: '🔔' },
]

export default function PatientLayout({ profile, onLogout }) {
  return (
    <RoleShell
      profile={profile}
      onLogout={onLogout}
      roleClass="role-patient"
      brand={{ title: 'My health hub', subtitle: 'Patient' }}
      nav={navItems}
    >
      <Outlet />
    </RoleShell>
  )
}
