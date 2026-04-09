import axios from 'axios'

export type ApiErrorDetail = {
  field?: string
  message?: string
}

export type ApiErrorPayload = {
  status?: number
  errorCode?: string
  message?: string
  error?: string
  details?: ApiErrorDetail[]
}

export type FieldErrors = Record<string, string>

function parseApiError(error: unknown): ApiErrorPayload | undefined {
  if (!axios.isAxiosError(error)) {
    return undefined
  }

  const responseData = error.response?.data
  if (!responseData || typeof responseData !== 'object') {
    return undefined
  }

  return responseData as ApiErrorPayload
}

export function extractApiErrorMessage(error: unknown): string {
  const apiError = parseApiError(error)
  return apiError?.message ?? apiError?.error ?? 'リクエストに失敗しました。'
}

export function extractApiErrorCode(error: unknown): string {
  return parseApiError(error)?.errorCode ?? ''
}

export function extractFieldErrorsFromApiError(error: unknown): FieldErrors {
  const details = parseApiError(error)?.details

  if (!Array.isArray(details)) {
    return {}
  }

  return details.reduce<FieldErrors>((acc, detail) => {
    const field = detail.field?.trim()
    const message = detail.message?.trim()

    if (field && message && !acc[field]) {
      acc[field] = message
    }

    return acc
  }, {})
}

export function hasFieldErrors(fieldErrors: FieldErrors): boolean {
  return Object.keys(fieldErrors).length > 0
}


export function resolveUserMessage(error: unknown): string {
  const apiError = parseApiError(error)
  const code = apiError?.errorCode ?? ''
  const status = apiError?.status
  const rawMessage = apiError?.message ?? apiError?.error ?? ''

  if (code.startsWith('SYS-') || status === 500 || rawMessage === 'Internal Server Error') {
    return 'システムエラーが発生しました。しばらくしてから再度お試しください。'
  }

  if (code.startsWith('RES-')) {
    return rawMessage || '対象データが見つかりません。'
  }

  if (code.startsWith('PERM-') || code === 'AUTH-005' || status == 403) {
    return 'この操作を行う権限がありません。'
  }

  if (code.startsWith('AUTH-') || status == 401) {
    return rawMessage || '認証エラーが発生しました。再度ログインしてください。'
  }

  if (error instanceof Error && error.message.trim()) {
    return error.message
  }

  return rawMessage || 'エラーが発生しました。'
}
