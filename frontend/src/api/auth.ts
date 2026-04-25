import { http } from './http'
import type { ApiResponse } from '../types/api'
import type { AuthPayload, Credentials } from '../types/auth'

export async function login(payload: Credentials) {
  const response = await http.post<ApiResponse<AuthPayload>>('/auth/login', payload)
  return response.data.data
}

export async function register(payload: Credentials) {
  const response = await http.post<ApiResponse<AuthPayload>>('/auth/register', payload)
  return response.data.data
}
