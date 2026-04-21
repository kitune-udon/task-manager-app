import { apiClient } from './apiClient'
import { unwrapApiData } from './apiTypes'
import type { ApiEnvelope } from './apiTypes'

/**
 * ログインAPIへ送るリクエスト。
 */
export type LoginRequest = {
  email: string
  password: string
}

/**
 * ログインAPIから返る認証トークンとユーザー情報。
 */
export type LoginResponse = {
  token?: string
  user?: {
    id?: number
    name?: string
    email?: string
  }
}

/**
 * ユーザー登録APIへ送るリクエスト。
 */
export type RegisterRequest = {
  name: string
  email: string
  password: string
}

/**
 * ユーザー登録APIから返る作成済みユーザー情報。
 */
export type RegisterResponse = {
  id?: number
  name?: string
  email?: string
  createdAt?: string
}

/**
 * 値がnullではないオブジェクトかどうかを判定する。
 */
function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

/**
 * ログインAPIレスポンスが画面で利用できる形式か検証する。
 */
function assertLoginResponse(payload: unknown): asserts payload is LoginResponse {
  if (!isObject(payload) || typeof payload.token !== 'string' || !payload.token.trim()) {
    // 認証後の状態保存に必須なtokenがない場合は、通常のAPIエラーとは別に扱う。
    throw new Error('ログイン応答の形式が不正です。ページを再読み込みして再度お試しください。')
  }
}

/**
 * 登録APIレスポンスが画面で利用できる形式か検証する。
 */
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

/**
 * ログインAPIを呼び出し、レスポンス形式を検証して返す。
 */
export async function login(request: LoginRequest): Promise<LoginResponse> {
  const response = await apiClient.post<ApiEnvelope<LoginResponse> | LoginResponse>(
    '/api/auth/login',
    request,
  )

  const result = unwrapApiData(response.data)
  assertLoginResponse(result)
  return result
}

/**
 * ユーザー登録APIを呼び出し、レスポンス形式を検証して返す。
 */
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
