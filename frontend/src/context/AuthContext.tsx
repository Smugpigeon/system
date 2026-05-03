import { useMemo, useState, type PropsWithChildren } from 'react'
import type { AuthPayload } from '../types/auth'
import { AuthContext, type AuthContextValue } from './auth-context'
import { clearStoredAuth, getStoredAuth, setStoredAuth } from '../utils/storage'

export function AuthProvider({ children }: PropsWithChildren) {
  const [auth, setAuth] = useState<AuthPayload | null>(() => getStoredAuth())

  const value = useMemo<AuthContextValue>(() => ({
    auth,
    isAuthenticated: Boolean(auth?.accessToken),
    setAuthSession: (payload) => {
      setAuth(payload)
      setStoredAuth(payload)
    },
    logout: () => {
      setAuth(null)
      clearStoredAuth()
    },
  }), [auth])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
