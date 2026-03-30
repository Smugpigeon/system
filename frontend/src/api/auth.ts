import { http } from './http'
import type { ApiResponse } from '../types/api'
import type { AuthPayload, Credentials } from '../types/auth'

export async function login(credentials: Credentials) {
  const response = await http.post<ApiResponse<AuthPayload>>(
    '/auth/login',
    credentials,
  )
  return response.data.data
}

export async function register(credentials: Credentials) {
  const response = await http.post<ApiResponse<AuthPayload>>(
    '/auth/register',
    credentials,
  )
  return response.data.data
}
