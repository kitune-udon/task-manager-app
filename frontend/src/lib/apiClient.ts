import axios from 'axios'
import { clearAuthToken, getAuthToken } from './authStorage'

const DEFAULT_API_BASE_URL = 'http://localhost:8080'

function resolveApiBaseUrl() {
  const configuredValue = import.meta.env.VITE_API_BASE_URL?.trim()

  if (!configuredValue) {
    return DEFAULT_API_BASE_URL
  }

  return configuredValue.endsWith('/') ? configuredValue.slice(0, -1) : configuredValue
}

export const API_BASE_URL = resolveApiBaseUrl()
export const UNAUTHORIZED_EVENT = 'app:unauthorized'

function isAuthApiRequest(url: string | undefined): boolean {
  if (!url) {
    return false
  }

  return url.startsWith('/api/auth/')
}

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

apiClient.interceptors.request.use((config) => {
  const token = getAuthToken()

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const requestUrl = typeof error.config?.url === 'string' ? error.config.url : undefined

    if (error.response?.status === 401 && !isAuthApiRequest(requestUrl)) {
      clearAuthToken()
      window.dispatchEvent(new CustomEvent(UNAUTHORIZED_EVENT))
    }

    return Promise.reject(error)
  },
)
