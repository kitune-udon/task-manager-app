/**
 * バックエンドが返す共通レスポンスラッパー。
 */
export type ApiEnvelope<T> = {
  success?: boolean
  data?: T
  message?: string
  error?: string
}

/**
 * 共通レスポンスラッパーからdataを取り出し、未ラップのレスポンスはそのまま返す。
 */
export function unwrapApiData<T>(payload: ApiEnvelope<T> | T): T {
  if (payload && typeof payload === 'object' && 'data' in (payload as ApiEnvelope<T>)) {
    const apiPayload = payload as ApiEnvelope<T>

    if (apiPayload.data !== undefined) {
      return apiPayload.data
    }
  }

  // 一部APIやテストではラップされていない値を扱えるよう、そのまま返す。
  return payload as T
}
