import { HubConnectionBuilder, LogLevel } from '@microsoft/signalr'
import { getAccessToken, isTokenExpired, isUnauthorizedError } from './authSession'
import { getApiBase } from './apiBase'

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
  const token = getAccessToken()
  if (!token || isTokenExpired(token)) {
    const err = new Error('Session expired. Please sign in again.')
    onUnauthorized?.(err)
    throw err
  }

  try {
    await connection.start()
    await connection.invoke('SubscribeToPatient', patientId)
  } catch (err) {
    if (isUnauthorizedError(err)) {
      onUnauthorized?.(err)
    }
    throw err
  }
}
