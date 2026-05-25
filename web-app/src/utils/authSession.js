const AUTH_KEY = 'authSession'

export const getAuthSession = () => {
  try {
    const raw = localStorage.getItem(AUTH_KEY)
    if (!raw) return null
    return JSON.parse(raw)
  } catch {
    localStorage.removeItem(AUTH_KEY)
    return null
  }
}

export const getAccessToken = () => {
  const session = getAuthSession()
  const token = session?.token ?? session?.accessToken ?? ''
  return typeof token === 'string' ? token.trim() : ''
}

export const isTokenExpired = (token) => {
  if (!token) return true
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    if (!payload?.exp) return false
    const expiresMs = payload.exp * 1000
    return Date.now() >= expiresMs - 30_000
  } catch {
    return false
  }
}

export const clearAuthSession = () => {
  localStorage.removeItem(AUTH_KEY)
}

export const isUnauthorizedError = (err) => {
  const message = `${err?.message || ''}`.toLowerCase()
  return message.includes('401') || message.includes('unauthorized')
}
