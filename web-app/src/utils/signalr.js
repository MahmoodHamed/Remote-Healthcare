import { HubConnectionBuilder, LogLevel } from '@microsoft/signalr'
import { getApiBase } from './apiBase'
import {
  ensureValidSession,
  getAccessToken,
  isTokenExpired,
  isUnauthorizedError,
  refreshAccessToken,
} from './authSession'

export const buildVitalsHubConnection = (handlers = {}) => {
  const hubUrl = new URL('/hubs/vitals', getApiBase()).toString()

  const connection = new HubConnectionBuilder()
    .withUrl(hubUrl, {
      accessTokenFactory: () => getAccessToken(),
      withCredentials: true,
    })
    .withAutomaticReconnect()
    .configureLogging(LogLevel.Warning)
    .build()

  if (handlers.onVitals) {
    connection.on('ReceiveVitals', handlers.onVitals)
  }

  return connection
}

export const startVitalsHub = async (connection, patientId, { onUnauthorized } = {}) => {
  const apiBase = getApiBase()
  const token = getAccessToken()
  if (!token || isTokenExpired(token)) {
    const ok = await ensureValidSession(apiBase)
    if (!ok) {
      const err = new Error('Session expired. Please sign in again.')
      onUnauthorized?.(err)
      throw err
    }
  }

  try {
    await connection.start()
    await connection.invoke('SubscribeToPatient', patientId)
  } catch (err) {
    if (isUnauthorizedError(err)) {
      const refreshed = await refreshAccessToken(apiBase)
      if (refreshed) {
        try {
          await connection.start()
          await connection.invoke('SubscribeToPatient', patientId)
          return
        } catch (retryErr) {
          onUnauthorized?.(retryErr)
          throw retryErr
        }
      }
      onUnauthorized?.(err)
    }
    throw err
  }
}
