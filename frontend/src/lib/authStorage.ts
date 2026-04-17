const TOKEN_KEY = 'authToken'
const USER_DISPLAY_NAME_KEY = 'userDisplayName'
const USER_ID_KEY = 'userId'
const REDIRECT_PATH_KEY = 'postLoginRedirectPath'

export function getAuthToken(): string {
  return localStorage.getItem(TOKEN_KEY) ?? ''
}

export function saveAuthToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearAuthToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export function getUserDisplayName(): string {
  return localStorage.getItem(USER_DISPLAY_NAME_KEY) ?? ''
}

export function saveUserDisplayName(value: string) {
  localStorage.setItem(USER_DISPLAY_NAME_KEY, value)
}

export function clearUserDisplayName() {
  localStorage.removeItem(USER_DISPLAY_NAME_KEY)
}

export function getUserId(): number | null {
  const value = localStorage.getItem(USER_ID_KEY)
  if (!value) {
    return null
  }

  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

export function saveUserId(value: number) {
  localStorage.setItem(USER_ID_KEY, String(value))
}

export function clearUserId() {
  localStorage.removeItem(USER_ID_KEY)
}

export function isProtectedPath(pathname: string): boolean {
  return /^\/(?:tasks(?:\/.*)?|notifications)$/.test(pathname)
}

export function savePostLoginRedirectPath(pathname: string) {
  if (!isProtectedPath(pathname)) return
  localStorage.setItem(REDIRECT_PATH_KEY, pathname)
}

export function getPostLoginRedirectPath(): string {
  return localStorage.getItem(REDIRECT_PATH_KEY) ?? ''
}

export function consumePostLoginRedirectPath(): string {
  const pathname = getPostLoginRedirectPath()
  localStorage.removeItem(REDIRECT_PATH_KEY)
  return pathname
}

export function clearPostLoginRedirectPath() {
  localStorage.removeItem(REDIRECT_PATH_KEY)
}

export {
  TOKEN_KEY,
  USER_DISPLAY_NAME_KEY,
  USER_ID_KEY,
  REDIRECT_PATH_KEY,
}
