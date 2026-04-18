import { apiClient } from './apiClient'
import { unwrapApiData } from './apiTypes'
import type { PageResponse, TaskUser } from './taskApi'

export type TaskComment = {
  id: number
  taskId: number
  content: string
  createdBy?: TaskUser | null
  createdAt?: string | null
  updatedAt?: string | null
  version?: number | null
}

export type CreateCommentRequest = {
  content: string
}

export type UpdateCommentRequest = {
  content: string
  version: number
}

export async function fetchComments(taskId: number | string, size = 100): Promise<TaskComment[]> {
  const response = await apiClient.get<PageResponse<TaskComment>>(`/api/tasks/${taskId}/comments`, {
    params: { page: 0, size },
  })
  const data = unwrapApiData(response.data)
  return Array.isArray(data.content) ? data.content : []
}

export async function createComment(taskId: number | string, request: CreateCommentRequest): Promise<TaskComment> {
  const response = await apiClient.post<TaskComment>(`/api/tasks/${taskId}/comments`, request)
  return unwrapApiData(response.data)
}

export async function updateComment(commentId: number | string, request: UpdateCommentRequest): Promise<TaskComment> {
  const response = await apiClient.put<TaskComment>(`/api/comments/${commentId}`, request)
  return unwrapApiData(response.data)
}

export async function deleteComment(commentId: number | string): Promise<void> {
  await apiClient.delete(`/api/comments/${commentId}`)
}
