import md5 from 'md5'

const shortIdPattern = /^[A-Za-z0-9]{6}$/
const guidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

const hexToBytes = (hex) => {
  const bytes = new Uint8Array(16)
  for (let i = 0; i < 16; i += 1) {
    bytes[i] = parseInt(hex.slice(i * 2, i * 2 + 2), 16)
  }
  return bytes
}

const bytesToUuid = (bytes) => {
  const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('')
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}

export const normalizePatientId = (value) => {
  const trimmed = value.trim()
  if (!trimmed) return ''
  if (guidPattern.test(trimmed)) return trimmed.toLowerCase()
  if (!shortIdPattern.test(trimmed)) return ''

  const shortId = trimmed.toUpperCase()
  const bytes = hexToBytes(md5(shortId))
  bytes[6] = (bytes[6] & 0x0f) | 0x30
  bytes[8] = (bytes[8] & 0x3f) | 0x80
  return bytesToUuid(bytes)
}
