import { createContext, useEffect, useState } from 'react'
import type { PropsWithChildren } from 'react'
import type { AuthPayload } from '../types/auth'
import {
  clearStoredAuth,
  readStoredAuth,
  writeStoredAuth,
} from '../utils/storage'

type AuthContextValue = {
  auth: AuthPayload | null
  isAuthenticated: boolean
  setAuthSession: (payload: AuthPayload) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: PropsWithChildren) {
  const [auth, setAuth] = useState<AuthPayload | null>(() => readStoredAuth())

  useEffect(() => {
    if (auth) {
      writeStoredAuth(auth)
      return
    }

    clearStoredAuth()
  }, [auth])

  const value: AuthContextValue = {
    auth,
    isAuthenticated: Boolean(auth?.accessToken),
    setAuthSession: setAuth,
    logout: () => setAuth(null),
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export { AuthContext }
