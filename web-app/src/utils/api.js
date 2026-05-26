import { getApiBase } from './apiBase'
import { getAccessToken } from './authSession'

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
      const [firstKey] = Object.keys(data.errors)
      if (firstKey) {
        const detail = data.errors[firstKey]
        if (Array.isArray(detail) && detail[0]) return `${firstKey}: ${detail[0]}`
        if (typeof detail === 'string') return `${firstKey}: ${detail}`
      }
    }
  } catch {
    return null
  }
  return null
}

const request = async (path, { method = 'GET', body, signal, headers = {}, auth = true } = {}) => {
  const url = new URL(path, getApiBase()).toString()
  const finalHeaders = { Accept: 'application/json', ...headers }
  if (body !== undefined) finalHeaders['Content-Type'] = 'application/json'
  if (auth) {
    const token = getAccessToken()
    if (token) finalHeaders.Authorization = `Bearer ${token}`
  }

  const response = await fetch(url, {
    method,
    headers: finalHeaders,
    body: body === undefined ? undefined : JSON.stringify(body),
    signal,
  })

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
