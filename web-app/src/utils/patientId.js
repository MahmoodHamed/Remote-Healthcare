const shortIdPattern = /^[A-Za-z0-9]{6}$/
const guidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

const md5Bytes = (str) => {
  const bytes = new TextEncoder().encode(str)
  const words = []

  for (let i = 0; i < bytes.length; i += 1) {
    words[i >> 2] = (words[i >> 2] || 0) | (bytes[i] << ((i % 4) * 8))
  }

  words[bytes.length >> 2] = (words[bytes.length >> 2] || 0) | (0x80 << ((bytes.length % 4) * 8))
  words[(((bytes.length + 8) >> 6) << 4) + 14] = bytes.length * 8

  let a = 0x67452301
  let b = 0xefcdab89
  let c = 0x98badcfe
  let d = 0x10325476

  const k = [
    0xd76aa478, 0xe8c7b756, 0x242070db, 0xc1bdceee,
    0xf57c0faf, 0x4787c62a, 0xa8304613, 0xfd469501,
    0x698098d8, 0x8b44f7af, 0xffff5bb1, 0x895cd7be,
    0x6b901122, 0xfd987193, 0xa679438e, 0x49b40821,
    0xf61e2562, 0xc040b340, 0x265e5a51, 0xe9b6c7aa,
    0xd62f105d, 0x02441453, 0xd8a1e681, 0xe7d3fbc8,
    0x21e1cde6, 0xc33707d6, 0xf4d50d87, 0x455a14ed,
    0xa9e3e905, 0xfcefa3f8, 0x676f02d9, 0x8d2a4c8a,
    0xfffa3942, 0x8771f681, 0x6d9d6122, 0xfde5380c,
    0xa4beea44, 0x4bdecfa9, 0xf6bb4b60, 0xbebfbc70,
    0x289b7ec6, 0xeaa127fa, 0xd4ef3085, 0x04881d05,
    0xd9d4d039, 0xe6db99e5, 0x1fa27cf8, 0xc4ac5665,
    0xf4292244, 0x432aff97, 0xab9423a7, 0xfc93a039,
    0x655b59c3, 0x8f0ccc92, 0xffeff47d, 0x85845dd1,
    0x6fa87e4f, 0xfe2ce6e0, 0xa3014314, 0x4e0811a1,
    0xf7537e82, 0xbd3af235, 0x2ad7d2bb, 0xeb86d391,
  ]

  const r = [
    7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
    5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
    4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
    6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21,
  ]

  for (let offset = 0; offset < words.length; offset += 16) {
    let aa = a
    let bb = b
    let cc = c
    let dd = d

    const chunk = words.slice(offset, offset + 16)
    while (chunk.length < 16) chunk.push(0)

    for (let i = 0; i < 64; i += 1) {
      let f = 0
      let g = 0

      if (i < 16) {
        f = (bb & cc) | (~bb & dd)
        g = i
      } else if (i < 32) {
        f = (dd & bb) | (~dd & cc)
        g = (5 * i + 1) % 16
      } else if (i < 48) {
        f = bb ^ cc ^ dd
        g = (3 * i + 5) % 16
      } else {
        f = cc ^ (bb | ~dd)
        g = (7 * i) % 16
      }

      const temp = dd
      dd = cc
      cc = bb
      const sum = (aa + f + k[i] + chunk[g]) >>> 0
      bb = (bb + ((sum << r[i]) | (sum >>> (32 - r[i])))) >>> 0
      aa = temp
    }

    a = (a + aa) >>> 0
    b = (b + bb) >>> 0
    c = (c + cc) >>> 0
    d = (d + dd) >>> 0
  }

  const out = new Uint8Array(16)
  const outView = new DataView(out.buffer)
  outView.setUint32(0, a, true)
  outView.setUint32(4, b, true)
  outView.setUint32(8, c, true)
  outView.setUint32(12, d, true)
  return out
}

const bytesToUuid = (bytes) => {
  const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('')
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}

/** @deprecated Legacy MD5 mapping — do not use for SignalR; use resolveHubPatientId instead. */
export const normalizePatientId = (value) => {
  const trimmed = value.trim()
  if (!trimmed) return ''
  if (guidPattern.test(trimmed)) return trimmed.toLowerCase()
  if (!shortIdPattern.test(trimmed)) return ''

  const bytes = md5Bytes(trimmed)
  bytes[6] = (bytes[6] & 0x0f) | 0x30
  bytes[8] = (bytes[8] & 0x3f) | 0x80
  return bytesToUuid(bytes)
}

export const isPatientGuid = (value) => guidPattern.test(String(value || '').trim())

export const normalizeGuid = (value) => {
  const trimmed = String(value || '').trim()
  return guidPattern.test(trimmed) ? trimmed.toLowerCase() : ''
}

/** Real account GUID from login profile — never MD5 of the watch code. */
export const accountUserId = (profile) => {
  if (!profile || typeof profile !== 'object') return ''
  const raw = profile.id ?? profile.Id ?? profile.userId ?? profile.UserId
  return normalizeGuid(raw)
}

/**
 * Resolves a patient identifier for SignalR subscription.
 * Full GUIDs pass through; 6-char codes are resolved via the server.
 */
export const resolveConnectPatientId = async (input, authProfile, { apiBase, accessToken }) => {
  const fallbackGuid = authProfile?.role === 'Patient' ? accountUserId(authProfile) : ''
  const value = String(input || '').trim() || fallbackGuid
  let id = normalizeGuid(value)
  if (!id && value) id = await resolveHubPatientId(value, { apiBase, accessToken })
  return id
}

export const resolveHubPatientId = async (value, { apiBase, accessToken }) => {
  const trimmed = String(value || '').trim()
  if (!trimmed) return ''
  if (guidPattern.test(trimmed)) return trimmed.toLowerCase()
  if (!shortIdPattern.test(trimmed)) return ''

  const url = new URL(`/api/patients/resolve-code/${encodeURIComponent(trimmed)}`, apiBase).toString()
  const headers = accessToken ? { Authorization: `Bearer ${accessToken}` } : {}
  const res = await fetch(url, { headers })
  if (!res.ok) return ''
  const data = await res.json()
  const userId = data?.userId ?? data?.UserId
  return userId ? String(userId).toLowerCase() : ''
}
