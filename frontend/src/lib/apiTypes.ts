export type ApiEnvelope<T> = {
  success?: boolean
  data?: T
  message?: string
  error?: string
}

export function unwrapApiData<T>(payload: ApiEnvelope<T> | T): T {
  if (payload && typeof payload === 'object' && 'data' in (payload as ApiEnvelope<T>)) {
    const apiPayload = payload as ApiEnvelope<T>

    if (apiPayload.data !== undefined) {
      return apiPayload.data
    }
  }

  return payload as T
}
