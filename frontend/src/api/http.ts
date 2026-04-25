import axios from 'axios'
import { clearStoredAuth, getStoredAuth } from '../utils/storage'

const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://127.0.0.1:8080/api'

export const http = axios.create({
  baseURL,
  timeout: 10000,
})

http.interceptors.request.use((config) => {
  const auth = getStoredAuth()
  if (auth?.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearStoredAuth()
      if (window.location.pathname !== '/login') {
        window.location.assign('/login')
      }
    }
    return Promise.reject(error)
  },
)

export function getErrorMessage(error: unknown) {
  if (axios.isAxiosError(error)) {
    return error.response?.data?.message ?? error.message
  }
  if (error instanceof Error) {
    return error.message
  }
  return '请求失败，请稍后重试'
}
