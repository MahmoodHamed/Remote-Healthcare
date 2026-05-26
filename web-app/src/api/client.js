import { loadAuthSession, saveAuthSession, clearAuthSession } from '../utils/authSession'

const DEFAULT_API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const buildUrl = (path) => new URL(path, DEFAULT_API_BASE).toString()

async function refreshTokens(currentAccess, currentRefresh) {
  try {
    const res = await fetch(buildUrl('/api/auth/refresh'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ accessToken: currentAccess, refreshToken: currentRefresh, deviceInfo: 'web-app' }),
    })
    if (!res.ok) return null
    const data = await res.json()
    return data // expected to be AuthTokensDto
  } catch {
    return null
  }
}

export async function apiFetch(path, opts = {}) {
  const session = loadAuthSession()
  const accessToken = session?.accessToken
  const refreshToken = session?.refreshToken

  const headers = Object.assign({ 'Content-Type': 'application/json' }, opts.headers || {})
  if (accessToken) headers['Authorization'] = `Bearer ${accessToken}`

  const response = await fetch(buildUrl(path), { ...opts, headers })
  if (response.status !== 401) return response

  // Try refresh
  if (!refreshToken) return response
  const refreshed = await refreshTokens(accessToken, refreshToken)
  if (!refreshed) return response

  // Save new tokens and retry original
  const newAccess = refreshed?.accessToken ?? refreshed?.AccessToken ?? null
  const newRefresh = refreshed?.refreshToken ?? refreshed?.RefreshToken ?? null
  saveAuthSession({ accessToken: newAccess, refreshToken: newRefresh, profile: session.profile })

  // retry
  const retryHeaders = Object.assign({}, headers, { Authorization: `Bearer ${newAccess}` })
  return fetch(buildUrl(path), { ...opts, headers: retryHeaders })
}

export default { apiFetch }
