const TOKEN_KEY = 'authToken'
const USER_DISPLAY_NAME_KEY = 'userDisplayName'
const USER_ID_KEY = 'userId'
const REDIRECT_PATH_KEY = 'postLoginRedirectPath'

/**
 * 保存済みJWTを取得する。
 */
export function getAuthToken(): string {
  return localStorage.getItem(TOKEN_KEY) ?? ''
}

/**
 * JWTをlocalStorageへ保存する。
 */
export function saveAuthToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

/**
 * 保存済みJWTを削除する。
 */
export function clearAuthToken() {
  localStorage.removeItem(TOKEN_KEY)
}

/**
 * 保存済みユーザー表示名を取得する。
 */
export function getUserDisplayName(): string {
  return localStorage.getItem(USER_DISPLAY_NAME_KEY) ?? ''
}

/**
 * ユーザー表示名をlocalStorageへ保存する。
 */
export function saveUserDisplayName(value: string) {
  localStorage.setItem(USER_DISPLAY_NAME_KEY, value)
}

/**
 * 保存済みユーザー表示名を削除する。
 */
export function clearUserDisplayName() {
  localStorage.removeItem(USER_DISPLAY_NAME_KEY)
}

/**
 * 保存済みユーザーIDを取得する。
 */
export function getUserId(): number | null {
  const value = localStorage.getItem(USER_ID_KEY)
  if (!value) {
    return null
  }

  const parsed = Number(value)
  // localStorage上の壊れた値は未ログイン相当として扱う。
  return Number.isFinite(parsed) ? parsed : null
}

/**
 * ユーザーIDをlocalStorageへ保存する。
 */
export function saveUserId(value: number) {
  localStorage.setItem(USER_ID_KEY, String(value))
}

/**
 * 保存済みユーザーIDを削除する。
 */
export function clearUserId() {
  localStorage.removeItem(USER_ID_KEY)
}

/**
 * 認証が必要なアプリ内パスかどうかを判定する。
 */
export function isProtectedPath(pathname: string): boolean {
  const normalizedPathname = pathname.split(/[?#]/)[0]
  return /^\/(?:tasks(?:\/.*)?|teams(?:\/.*)?|notifications)$/.test(normalizedPathname)
}

/**
 * ログイン後に戻すための保護ページパスを保存する。
 */
export function savePostLoginRedirectPath(pathname: string) {
  // ログイン/登録などの公開ページは戻り先として保存しない。
  if (!isProtectedPath(pathname)) return
  localStorage.setItem(REDIRECT_PATH_KEY, pathname)
}

/**
 * 保存済みのログイン後リダイレクト先を取得する。
 */
export function getPostLoginRedirectPath(): string {
  return localStorage.getItem(REDIRECT_PATH_KEY) ?? ''
}

/**
 * 保存済みのログイン後リダイレクト先を取得し、同時に削除する。
 */
export function consumePostLoginRedirectPath(): string {
  const pathname = getPostLoginRedirectPath()
  localStorage.removeItem(REDIRECT_PATH_KEY)
  return pathname
}

/**
 * 保存済みのログイン後リダイレクト先を削除する。
 */
export function clearPostLoginRedirectPath() {
  localStorage.removeItem(REDIRECT_PATH_KEY)
}

export {
  TOKEN_KEY,
  USER_DISPLAY_NAME_KEY,
  USER_ID_KEY,
  REDIRECT_PATH_KEY,
}
