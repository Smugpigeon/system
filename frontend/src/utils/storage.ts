import type { AuthPayload } from '../types/auth'

const AUTH_STORAGE_KEY = 'task-manager-auth'

export function getStoredAuth(): AuthPayload | null {
  const raw = window.localStorage.getItem(AUTH_STORAGE_KEY)
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw) as AuthPayload
  } catch {
    window.localStorage.removeItem(AUTH_STORAGE_KEY)
    return null
  }
}

export function setStoredAuth(payload: AuthPayload) {
  window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(payload))
}

export function clearStoredAuth() {
  window.localStorage.removeItem(AUTH_STORAGE_KEY)
}
