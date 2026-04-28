import axios from 'axios'

/**
 * APIエラーレスポンスのフィールド単位詳細。
 */
export type ApiErrorDetail = {
  field?: string
  message?: string
}

/**
 * バックエンドの共通エラーレスポンスをフロントで扱うための型。
 */
export type ApiErrorPayload = {
  status?: number
  errorCode?: string
  message?: string
  error?: string
  details?: ApiErrorDetail[]
}

/**
 * フォームフィールド名をキーにした入力エラーメッセージ。
 */
export type FieldErrors = Record<string, string>

const NOT_FOUND_CODES = new Set(['ERR-TASK-004', 'ERR-USR-002'])
const FORBIDDEN_CODES = new Set(['ERR-AUTH-005', 'ERR-TASK-005', 'ERR-TASK-006'])

/**
 * axiosエラーからAPI共通エラーペイロードを取り出す。
 */
function parseApiError(error: unknown): ApiErrorPayload | undefined {
  if (!axios.isAxiosError(error)) {
    return undefined
  }

  const responseData = error.response?.data
  if (!responseData) {
    return undefined
  }

  if (typeof responseData === 'object') {
    return responseData as ApiErrorPayload
  }

  if (typeof responseData === 'string') {
    try {
      const parsed = JSON.parse(responseData)
      return typeof parsed === 'object' && parsed !== null ? parsed as ApiErrorPayload : undefined
    } catch {
      return undefined
    }
  }

  return undefined
}

/**
 * APIエラーから表示用メッセージを抽出する。
 */
export function extractApiErrorMessage(error: unknown): string {
  const apiError = parseApiError(error)
  return apiError?.message ?? apiError?.error ?? 'リクエストに失敗しました。'
}

/**
 * APIエラーからエラーコードを抽出する。
 */
export function extractApiErrorCode(error: unknown): string {
  return parseApiError(error)?.errorCode ?? ''
}

/**
 * APIエラー詳細をフォームフィールド別のエラーへ変換する。
 */
export function extractFieldErrorsFromApiError(error: unknown): FieldErrors {
  const details = parseApiError(error)?.details

  if (!Array.isArray(details)) {
    return {}
  }

  return details.reduce<FieldErrors>((acc, detail) => {
    const field = detail.field?.trim()
    const message = detail.message?.trim()

    if (field && message && !acc[field]) {
      // 同じフィールドに複数エラーがある場合は、先頭のメッセージを表示する。
      acc[field] = message
    }

    return acc
  }, {})
}

/**
 * フィールドエラーが1件以上あるかどうかを判定する。
 */
export function hasFieldErrors(fieldErrors: FieldErrors): boolean {
  return Object.keys(fieldErrors).length > 0
}


/**
 * APIエラーや通常のErrorを、画面に出してよいユーザー向けメッセージへ変換する。
 */
export function resolveUserMessage(error: unknown): string {
  const apiError = parseApiError(error)
  const code = apiError?.errorCode ?? ''
  const status = apiError?.status
  const rawMessage = apiError?.message ?? apiError?.error ?? ''

  if (code.startsWith('ERR-SYS-') || status === 500 || rawMessage === 'Internal Server Error') {
    // サーバー内部事情を出さないよう、システムエラーは定型文へ丸める。
    return 'システムエラーが発生しました。しばらくしてから再度お試しください。'
  }

  if (NOT_FOUND_CODES.has(code) || status === 404) {
    return rawMessage || '対象データが見つかりません。'
  }

  if (FORBIDDEN_CODES.has(code) || status === 403) {
    // 権限不足は詳細な対象を出さず、操作不可だけを伝える。
    return 'この操作を行う権限がありません。'
  }

  if (code.startsWith('ERR-AUTH-') || status === 401) {
    return rawMessage || '認証エラーが発生しました。再度ログインしてください。'
  }

  if (rawMessage) {
    return rawMessage
  }

  if (error instanceof Error && error.message.trim()) {
    return error.message
  }

  return 'エラーが発生しました。'
}
