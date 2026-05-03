import { createContext } from 'react'
import type { AuthPayload } from '../types/auth'

export type AuthContextValue = {
  auth: AuthPayload | null
  isAuthenticated: boolean
  setAuthSession: (payload: AuthPayload) => void
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)
