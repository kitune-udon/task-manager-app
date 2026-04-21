import { apiClient } from './apiClient'
import { unwrapApiData } from './apiTypes'
import type { TaskUser } from './taskApi'

/**
 * タスクに紐づく添付ファイル情報。
 */
export type TaskAttachment = {
  id: number
  taskId: number
  originalFileName: string
  contentType?: string | null
  fileSize?: number | null
  storageType?: string | null
  uploadedBy?: TaskUser | null
  createdAt?: string | null
}

/**
 * 指定タスクの添付ファイル一覧を取得する。
 */
export async function fetchAttachments(taskId: number | string): Promise<TaskAttachment[]> {
  const response = await apiClient.get<TaskAttachment[]>(`/api/tasks/${taskId}/attachments`)
  const data = unwrapApiData(response.data)
  return Array.isArray(data) ? data : []
}

/**
 * 指定タスクへ添付ファイルをアップロードする。
 */
export async function uploadAttachment(taskId: number | string, file: File): Promise<TaskAttachment> {
  const formData = new FormData()
  // multipart/form-dataのfileフィールドとしてバックエンドへ送る。
  formData.append('file', file)

  const response = await apiClient.post<TaskAttachment>(`/api/tasks/${taskId}/attachments`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })

  return unwrapApiData(response.data)
}

/**
 * Content-Dispositionヘッダーからダウンロードファイル名を解決する。
 */
function resolveDownloadFileName(contentDisposition: string | undefined, fallback: string) {
  if (!contentDisposition) {
    return fallback
  }

  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    // RFC 5987形式のUTF-8ファイル名を優先する。
    return decodeURIComponent(utf8Match[1])
  }

  const plainMatch = contentDisposition.match(/filename="?([^";]+)"?/i)
  return plainMatch?.[1] ?? fallback
}

/**
 * 添付ファイルをダウンロードする。
 */
export async function downloadAttachment(attachmentId: number | string, fallbackFileName: string): Promise<void> {
  const response = await apiClient.get<Blob>(`/api/attachments/${attachmentId}/download`, {
    responseType: 'blob',
  })
  const fileName = resolveDownloadFileName(response.headers['content-disposition'], fallbackFileName)
  const objectUrl = window.URL.createObjectURL(response.data)
  // Blob URLを一時的なリンクに設定し、ブラウザの通常ダウンロードを発火させる。
  const anchor = document.createElement('a')
  anchor.href = objectUrl
  anchor.download = fileName
  document.body.append(anchor)
  anchor.click()
  anchor.remove()
  // 作成したObject URLは使い終わったら解放する。
  window.URL.revokeObjectURL(objectUrl)
}

/**
 * 添付ファイルを削除する。
 */
export async function deleteAttachment(attachmentId: number | string): Promise<void> {
  await apiClient.delete(`/api/attachments/${attachmentId}`)
}
