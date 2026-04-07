const TOKEN_KEY = 'authToken'
const USER_DISPLAY_NAME_KEY = 'userDisplayName'

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

export { TOKEN_KEY, USER_DISPLAY_NAME_KEY }
