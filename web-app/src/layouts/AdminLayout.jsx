import { Outlet } from 'react-router-dom'
import RoleShell from './RoleShell.jsx'

const navItems = [
  { to: '/admin/dashboard', label: 'Overview', icon: '🏥' },
  { to: '/admin/doctors', label: 'Doctors', icon: '🩺' },
  { to: '/admin/patients', label: 'Patients', icon: '👤' },
  { to: '/admin/users', label: 'All users', icon: '⚙️' },
  { to: '/admin/audit', label: 'Audit log', icon: '📋' },
]

export default function AdminLayout({ profile, onLogout }) {
  return (
    <RoleShell
      profile={profile}
      onLogout={onLogout}
      roleClass="role-admin"
      brand={{ title: 'Hospital console', subtitle: 'Administration' }}
      nav={navItems}
    >
      <Outlet />
    </RoleShell>
  )
}
