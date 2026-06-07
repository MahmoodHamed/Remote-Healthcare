import { api, ApiError } from './api'
import { mapVitalsPayload } from './vitalsPayload'

export { mapVitalsPayload, mergeVitalsPayload } from './vitalsPayload'

export const fetchLatestVitals = async (patientId) => {
  try {
    const data = await api.get(`/api/patients/${patientId}/vitals/latest`)
    return mapVitalsPayload(data)
  } catch (err) {
    if (err instanceof ApiError && err.status === 401) {
      const authErr = new Error('Session expired. Please sign in again.')
      authErr.status = 401
      throw authErr
    }
    if (err instanceof ApiError && (err.status === 404 || err.status === 204)) return null
    throw err
  }
}
