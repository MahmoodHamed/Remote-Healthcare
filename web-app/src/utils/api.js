import { getApiBase } from './apiBase'
import {
  ensureValidSession,
  getAccessToken,
  isTokenExpired,
  notifySessionExpired,
  refreshAccessToken,
} from './authSession'

class ApiError extends Error {
  constructor(message, status, payload) {
    super(message)
    this.status = status
    this.payload = payload
  }
}

const parseErrorMessage = async (response) => {
  try {
    const data = await response.json()
    if (!data) return null
    if (typeof data === 'string') return data
    if (typeof data.message === 'string') return data.message
    if (typeof data.title === 'string') return data.title
    if (data.errors && typeof data.errors === 'object') {
      const lines = Object.entries(data.errors).flatMap(([field, detail]) => {
        if (Array.isArray(detail)) return detail.map((m) => `${field}: ${m}`)
        if (typeof detail === 'string') return [`${field}: ${detail}`]
        return []
      })
      if (lines.length > 0) return lines[0]
    }
  } catch {
    return null
  }
  return null
}

const fetchWithAuth = async (url, { method, body, signal, headers, auth }) => {
  const finalHeaders = { Accept: 'application/json', ...headers }
  if (body !== undefined) finalHeaders['Content-Type'] = 'application/json'
  if (auth) {
    const token = getAccessToken()
    if (token) finalHeaders.Authorization = `Bearer ${token}`
  }

  return fetch(url, {
    method,
    headers: finalHeaders,
    body: body === undefined ? undefined : JSON.stringify(body),
    signal,
  })
}

const request = async (path, { method = 'GET', body, signal, headers = {}, auth = true } = {}) => {
  const apiBase = getApiBase()
  const url = new URL(path, apiBase).toString()

  if (auth) {
    const token = getAccessToken()
    if (!token || isTokenExpired(token)) {
      const ok = await ensureValidSession(apiBase)
      if (!ok && getAccessToken()) {
        /* token present but not yet expired per skew window */
      } else if (!ok) {
        notifySessionExpired()
        throw new ApiError('Session expired. Please sign in again.', 401)
      }
    }
  }

  let response = await fetchWithAuth(url, { method, body, signal, headers, auth })

  if (response.status === 401 && auth) {
    const refreshed = await refreshAccessToken(apiBase)
    if (refreshed) {
      response = await fetchWithAuth(url, { method, body, signal, headers, auth })
    }
    if (response.status === 401) {
      notifySessionExpired()
      throw new ApiError('Session expired. Please sign in again.', 401)
    }
  }

  if (response.status === 204) return null

  if (!response.ok) {
    const message = (await parseErrorMessage(response)) || `Request failed (${response.status})`
    throw new ApiError(message, response.status)
  }

  const text = await response.text()
  if (!text) return null
  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

export const api = {
  get: (path, options) => request(path, { ...options, method: 'GET' }),
  post: (path, body, options) => request(path, { ...options, method: 'POST', body }),
  put: (path, body, options) => request(path, { ...options, method: 'PUT', body }),
  patch: (path, body, options) => request(path, { ...options, method: 'PATCH', body }),
  delete: (path, body, options) => request(path, { ...options, method: 'DELETE', body }),
}

export { ApiError }
