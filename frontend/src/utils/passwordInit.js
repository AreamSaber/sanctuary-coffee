import { getPasswordInitStatus } from '@/api/auth'

let statusCache = null
let pendingRequest = null

const normalizeStatus = (payload) => ({
  required: Boolean(payload?.required),
  pendingCount: Number(payload?.pendingCount || 0),
  users: Array.isArray(payload?.users) ? payload.users : []
})

export async function fetchPasswordInitStatus(force = false) {
  if (!force && statusCache) {
    return statusCache
  }

  if (!force && pendingRequest) {
    return pendingRequest
  }

  pendingRequest = getPasswordInitStatus()
    .then((response) => {
      statusCache = normalizeStatus(response?.data)
      return statusCache
    })
    .finally(() => {
      pendingRequest = null
    })

  return pendingRequest
}

export function clearPasswordInitStatusCache() {
  statusCache = null
  pendingRequest = null
}
