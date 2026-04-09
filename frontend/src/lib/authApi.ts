import { apiClient } from './apiClient'
import { unwrapApiData } from './apiTypes'
import type { ApiEnvelope } from './apiTypes'

export type LoginRequest = {
  email: string
  password: string
}

export type LoginResponse = {
  token?: string
  user?: {
    id?: number
    name?: string
    email?: string
  }
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

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function assertLoginResponse(payload: unknown): asserts payload is LoginResponse {
  if (!isObject(payload) || typeof payload.token !== 'string' || !payload.token.trim()) {
    throw new Error('ログイン応答の形式が不正です。ページを再読み込みして再度お試しください。')
  }
}

function assertRegisterResponse(payload: unknown): asserts payload is RegisterResponse {
  if (
    !isObject(payload) ||
    typeof payload.id !== 'number' ||
    typeof payload.email !== 'string' ||
    !payload.email.trim()
  ) {
    throw new Error('登録応答の形式が不正です。ページを再読み込みして再度お試しください。')
  }
}

export async function login(request: LoginRequest): Promise<LoginResponse> {
  const response = await apiClient.post<ApiEnvelope<LoginResponse> | LoginResponse>(
    '/api/auth/login',
    request,
  )

  const result = unwrapApiData(response.data)
  assertLoginResponse(result)
  return result
}

export async function register(request: RegisterRequest): Promise<RegisterResponse> {
  const response = await apiClient.post<ApiEnvelope<RegisterResponse> | RegisterResponse>(
    '/api/auth/register',
    request,
  )

  const result = unwrapApiData(response.data)
  assertRegisterResponse(result)
  return result
}

export { extractApiErrorMessage, extractFieldErrorsFromApiError, hasFieldErrors, resolveUserMessage } from './apiError'
