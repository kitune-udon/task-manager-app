import { apiClient } from './apiClient'
import { unwrapApiData } from './apiTypes'
import type { ApiEnvelope } from './apiTypes'

export type LoginRequest = {
  email: string
  password: string
}

export type LoginResponse = {
  token?: string
  tokenType?: string
  expiresIn?: number
}

export type RegisterRequest = {
  name: string
  email: string
  password: string
}

export type RegisterResponse = {
  id?: number
  name?: string
  email?: string
  createdAt?: string
}

export async function login(request: LoginRequest): Promise<LoginResponse> {
  const response = await apiClient.post<ApiEnvelope<LoginResponse> | LoginResponse>(
    '/api/auth/login',
    request,
  )

  return unwrapApiData(response.data)
}

export async function register(request: RegisterRequest): Promise<RegisterResponse> {
  const response = await apiClient.post<ApiEnvelope<RegisterResponse> | RegisterResponse>(
    '/api/auth/register',
    request,
  )

  return unwrapApiData(response.data)
}

export { extractApiErrorMessage, extractFieldErrorsFromApiError, hasFieldErrors, resolveUserMessage } from './apiError'
