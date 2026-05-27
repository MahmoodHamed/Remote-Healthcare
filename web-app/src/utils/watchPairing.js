import { normalizePatientId } from './patientId'

export const WATCH_SHORT_ID_KEY = 'rpm-watch-shortid'
export const WATCH_HOST_KEY = 'rpm-watch-mqtt-host'
export const WATCH_PORT_KEY = 'rpm-watch-mqtt-port'

const shortIdPattern = /^[A-Za-z0-9]{6}$/

export const getWatchShortId = () => localStorage.getItem(WATCH_SHORT_ID_KEY)?.trim().toUpperCase() ?? ''

export const isValidWatchShortId = (value) => shortIdPattern.test(value.trim())

/** Patient ID the dashboard and watch must share for live vitals. */
export const getVitalsPatientId = (profileId) => {
  const shortId = getWatchShortId()
  if (shortId && isValidWatchShortId(shortId)) {
    const normalized = normalizePatientId(shortId)
    if (normalized) return normalized
  }
  return profileId ?? ''
}
