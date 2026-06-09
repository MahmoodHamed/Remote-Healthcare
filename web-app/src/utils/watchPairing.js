import { normalizePatientId } from './patientId'
import { api } from './api'

export const WATCH_SHORT_ID_KEY = 'rpm-watch-shortid'
export const WATCH_HOST_KEY = 'rpm-watch-mqtt-host'
export const WATCH_PORT_KEY = 'rpm-watch-mqtt-port'

const shortIdPattern = /^[A-Za-z0-9]{6}$/

export const getWatchShortId = () => localStorage.getItem(WATCH_SHORT_ID_KEY)?.trim().toUpperCase() ?? ''

export const setWatchShortId = (value) => {
  const upper = value?.trim().toUpperCase() ?? ''
  if (upper) localStorage.setItem(WATCH_SHORT_ID_KEY, upper)
  else localStorage.removeItem(WATCH_SHORT_ID_KEY)
}

export const isValidWatchShortId = (value) => shortIdPattern.test(value.trim())

/** Resolve streaming UUID from watch short ID + account UUID (no localStorage). */
export const resolveVitalsPatientId = (watchShortId, profileId) => {
  if (watchShortId && isValidWatchShortId(watchShortId)) {
    const normalized = normalizePatientId(watchShortId)
    if (normalized) return normalized
  }
  return profileId ?? ''
}

/** Patient ID the dashboard and watch must share for live vitals. */
export const getVitalsPatientId = (profileId) => {
  const shortId = getWatchShortId()
  return resolveVitalsPatientId(shortId, profileId)
}

/**
 * Save watch short ID both locally (for instant use) and to the backend
 * (so mobile and other devices automatically pick it up).
 */
export const saveWatchShortId = async (userId, shortId) => {
  setWatchShortId(shortId)
  await api.put(`/api/patients/${userId}/watch-setup`, { shortId: shortId?.trim().toUpperCase() || null })
}

/**
 * Load watch short ID from the backend profile and sync it to localStorage.
 * Returns the normalised streaming UUID, or profileId as fallback.
 */
export const loadAndSyncWatchShortId = (profileWatchShortId, profileId) => {
  if (profileWatchShortId && isValidWatchShortId(profileWatchShortId)) {
    setWatchShortId(profileWatchShortId)
  }
  return getVitalsPatientId(profileId)
}
