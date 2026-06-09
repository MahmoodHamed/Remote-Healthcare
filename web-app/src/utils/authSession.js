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

export const getRefreshToken = () => {
  const session = getAuthSession()
  const token = session?.refreshToken ?? ''
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

export const updateAuthTokens = (tokens) => {
  const session = getAuthSession()
  if (!session || !tokens) return false
  const accessToken = tokens.accessToken ?? tokens.AccessToken
  const refreshToken = tokens.refreshToken ?? tokens.RefreshToken ?? session.refreshToken
  if (!accessToken) return false
  localStorage.setItem(
    AUTH_KEY,
    JSON.stringify({ ...session, token: accessToken, refreshToken }),
  )
  window.dispatchEvent(new Event('auth:tokens-updated'))
  return true
}

export const clearAuthSession = () => {
  localStorage.removeItem(AUTH_KEY)
}

export const isUnauthorizedError = (err) => {
  if (err?.status === 401) return true
  const message = `${err?.message || ''}`.toLowerCase()
  return message.includes('401') || message.includes('unauthorized')
}

let refreshInFlight = null

/** Refresh the access token using the stored refresh token. Returns true on success. */
export const refreshAccessToken = async (apiBase) => {
  if (refreshInFlight) return refreshInFlight

  refreshInFlight = (async () => {
    try {
      const refreshToken = getRefreshToken()
      if (!refreshToken) return false

      const url = new URL('/api/auth/refresh', apiBase).toString()
      const response = await fetch(url, {
        method: 'POST',
        headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
        body: JSON.stringify({
          refreshToken,
          accessToken: getAccessToken() || undefined,
          deviceInfo: 'web',
        }),
      })

      if (!response.ok) return false
      const data = await response.json()
      return updateAuthTokens(data)
    } catch {
      return false
    } finally {
      refreshInFlight = null
    }
  })()

  return refreshInFlight
}

/** Ensure a usable access token exists; refresh when expired. */
export const ensureValidSession = async (apiBase) => {
  const token = getAccessToken()
  if (token && !isTokenExpired(token)) return true
  return refreshAccessToken(apiBase)
}

export const notifySessionExpired = () => {
  clearAuthSession()
  window.dispatchEvent(new Event('auth:session-expired'))
}

