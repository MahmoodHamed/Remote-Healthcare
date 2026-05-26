const AUTH_SESSION_KEY = 'authSession'

const readJson = (value) => {
  try {
    return JSON.parse(value)
  } catch {
    return null
  }
}

export const loadAuthSession = () => {
  if (typeof localStorage === 'undefined') return null

  const saved = localStorage.getItem(AUTH_SESSION_KEY)
  if (!saved) return null

  const session = readJson(saved)
  if (!session || typeof session !== 'object') return null

  return {
    accessToken: session.accessToken ?? session.token ?? null,
    refreshToken: session.refreshToken ?? null,
    profile: session.profile ?? null,
  }
}

export const saveAuthSession = ({ accessToken, refreshToken = null, profile }) => {
  if (typeof localStorage === 'undefined') return

  localStorage.setItem(
    AUTH_SESSION_KEY,
    JSON.stringify({ accessToken, refreshToken, profile })
  )
  try { window.dispatchEvent(new CustomEvent('authSessionChanged', { detail: { accessToken, refreshToken, profile } })) } catch {}
}

export const clearAuthSession = () => {
  if (typeof localStorage === 'undefined') return

  localStorage.removeItem(AUTH_SESSION_KEY)
  try { window.dispatchEvent(new CustomEvent('authSessionChanged', { detail: null })) } catch {}
}