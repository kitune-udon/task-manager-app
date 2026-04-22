import axios from 'axios'
import { clearAuthToken, getAuthToken } from './authStorage'

const DEFAULT_API_BASE_URL = 'http://localhost:8080'
const LOCAL_HOSTNAMES = new Set(['localhost', '127.0.0.1'])

/**
 * URL末尾のスラッシュを取り除く。
 */
function trimTrailingSlash(value: string) {
  return value.endsWith('/') ? value.slice(0, -1) : value
}

/**
 * 実行環境に応じたAPIベースURLを解決する。
 */
function resolveApiBaseUrl() {
  const configuredValue = import.meta.env.VITE_API_BASE_URL?.trim()

  if (configuredValue) {
    return trimTrailingSlash(configuredValue)
  }

  if (typeof window !== 'undefined') {
    const { hostname, origin } = window.location

    if (!LOCAL_HOSTNAMES.has(hostname)) {
      // 本番配信ではフロントと同一originのAPIへ向ける。
      return trimTrailingSlash(origin)
    }
  }

  return DEFAULT_API_BASE_URL
}

/**
 * axiosクライアントで利用するAPIベースURL。
 */
export const API_BASE_URL = resolveApiBaseUrl()

/**
 * APIクライアントが認証切れをアプリ全体へ通知するイベント名。
 */
export const UNAUTHORIZED_EVENT = 'app:unauthorized'

/**
 * ログインや登録など、401を画面側で直接扱う認証APIかどうかを判定する。
 */
function isAuthApiRequest(url: string | undefined): boolean {
  if (!url) {
    return false
  }

  return url.startsWith('/api/auth/')
}

/**
 * 認証ヘッダー付与と認証切れ通知を共通化したaxiosクライアント。
 */
export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

apiClient.interceptors.request.use((config) => {
  const token = getAuthToken()

  if (token) {
    // 保存済みトークンがある場合は、すべてのAPIリクエストへBearerトークンを付与する。
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const requestUrl = typeof error.config?.url === 'string' ? error.config.url : undefined

    if (error.response?.status === 401 && !isAuthApiRequest(requestUrl)) {
      // 認証API以外の401はセッション切れとして扱い、useAuthStateへ再ログインを促す。
      clearAuthToken()
      window.dispatchEvent(new CustomEvent(UNAUTHORIZED_EVENT))
    }

    return Promise.reject(error)
  },
)
