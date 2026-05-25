/** API origin: same host as the page (avoids www vs non-www token issues). */
export const getApiBase = () => {
  const fromEnv = import.meta.env.VITE_API_BASE_URL?.trim()
  if (fromEnv) return fromEnv.replace(/\/$/, '')
  if (typeof window !== 'undefined' && window.location?.origin) {
    return window.location.origin
  }
  return 'http://localhost:5000'
}

export const getMqttHost = () =>
  import.meta.env.VITE_MQTT_HOST?.trim() || 'remote-care.tech'

export const getMqttPort = () =>
  import.meta.env.VITE_MQTT_PORT?.trim() || '1883'
