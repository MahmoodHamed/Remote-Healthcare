import { clearAuthSession, getAuthSession } from './authSession'

const AUTH_KEY = 'authSession'

export const setAuthSession = (data) => {
  const token = data?.tokens?.accessToken
  const user = data?.user
  if (!token || !user) return null
  const refreshToken = data?.tokens?.refreshToken ?? null
  const session = { token, refreshToken, profile: user }
  localStorage.setItem(AUTH_KEY, JSON.stringify(session))
  return user
}

export const readProfile = () => getAuthSession()?.profile ?? null

export const logout = () => {
  clearAuthSession()
}

export const homeRouteForRole = (role) => {
  if (role === 'Admin') return '/admin/dashboard'
  if (role === 'Doctor') return '/doctor/dashboard'
  if (role === 'Patient') return '/patient/dashboard'
  return '/'
}
