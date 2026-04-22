import { apiClient } from './apiClient'
import { unwrapApiData } from './apiTypes'
import type { PageResponse, TaskUser } from './taskApi'

/**
 * タスクに紐づくコメント情報。
 */
export type TaskComment = {
  id: number
  taskId: number
  content: string
  createdBy?: TaskUser | null
  createdAt?: string | null
  updatedAt?: string | null
  version?: number | null
}

/**
 * コメント作成APIへ送るリクエスト。
 */
export type CreateCommentRequest = {
  content: string
}

/**
 * コメント更新APIへ送るリクエスト。
 */
export type UpdateCommentRequest = {
  content: string
  /** 楽観ロックの競合検知に使う現在バージョン。 */
  version: number
}

/**
 * 指定タスクのコメント一覧を取得する。
 */
export async function fetchComments(taskId: number | string, size = 100): Promise<TaskComment[]> {
  // 詳細画面ではコメントをまとめて表示するため、先頭ページを指定件数分だけ取得する。
  const response = await apiClient.get<PageResponse<TaskComment>>(`/api/tasks/${taskId}/comments`, {
    params: { page: 0, size },
  })
  const data = unwrapApiData(response.data)
  return Array.isArray(data.content) ? data.content : []
}

/**
 * 指定タスクへコメントを作成する。
 */
export async function createComment(taskId: number | string, request: CreateCommentRequest): Promise<TaskComment> {
  const response = await apiClient.post<TaskComment>(`/api/tasks/${taskId}/comments`, request)
  return unwrapApiData(response.data)
}

/**
 * コメントを更新する。
 */
export async function updateComment(commentId: number | string, request: UpdateCommentRequest): Promise<TaskComment> {
  const response = await apiClient.put<TaskComment>(`/api/comments/${commentId}`, request)
  return unwrapApiData(response.data)
}

/**
 * コメントを削除する。
 */
export async function deleteComment(commentId: number | string): Promise<void> {
  await apiClient.delete(`/api/comments/${commentId}`)
}
