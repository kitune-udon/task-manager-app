import { apiClient } from './apiClient'
import { unwrapApiData, type ApiEnvelope } from './apiTypes'

export type TaskStatus = 'TODO' | 'DOING' | 'DONE' | string
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | string

export type TaskUser = {
  id?: number | string | null
  name?: string | null
  email?: string | null
}

export type TaskItem = {
  id: number | string
  title: string
  description?: string | null
  status?: TaskStatus
  priority?: TaskPriority
  dueDate?: string | null
  assignedUserId?: number | string | null
  assignedUserName?: string | null
  assignedUser?: TaskUser | null
  createdBy?: TaskUser | null
  createdById?: number | string | null
  createdByName?: string | null
  createdAt?: string | null
  updatedAt?: string | null
  version?: number | null
}

export type CreateTaskRequest = {
  title: string
  description?: string
  status: TaskStatus
  priority: TaskPriority
  dueDate?: string
  assignedUserId?: number
  teamId?: number
}

export type UpdateTaskRequest = CreateTaskRequest & {
  version: number
}

export type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

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

export type ActivityItem = {
  id: number
  eventType: string
  actor?: TaskUser | null
  targetType?: string | null
  targetId?: number | null
  taskId?: number | null
  summary?: string | null
  detailJson?: unknown
  createdAt?: string | null
}

export type NotificationItem = {
  id: number
  activityLogId: number
  eventType: string
  message: string
  relatedTaskId?: number | null
  relatedTaskTitle?: string | null
  targetType?: string | null
  targetId?: number | null
  isRead: boolean
  readAt?: string | null
  createdAt?: string | null
}

export type UnreadCountResponse = {
  unreadCount: number
}

function normalizeTask(raw: TaskItem): TaskItem {
  const assignedUser = raw.assignedUser ?? null
  const createdBy = raw.createdBy ?? null

  return {
    ...raw,
    assignedUser,
    assignedUserId: raw.assignedUserId ?? assignedUser?.id ?? null,
    assignedUserName: raw.assignedUserName ?? assignedUser?.name ?? null,
    createdBy,
    createdById: raw.createdById ?? createdBy?.id ?? null,
    createdByName: raw.createdByName ?? createdBy?.name ?? null,
  }
}

export async function fetchTasks(): Promise<TaskItem[]> {
  const response = await apiClient.get<TaskItem[]>('/api/tasks')
  const data = unwrapApiData(response.data)

  if (!Array.isArray(data)) {
    return []
  }

  return data.map((task) => normalizeTask(task))
}

export async function fetchTaskById(id: number | string): Promise<TaskItem | null> {
  const response = await apiClient.get<TaskItem | ApiEnvelope<TaskItem>>(`/api/tasks/${id}`)
  const data = unwrapApiData(response.data)

  if (!data) {
    return null
  }

  return normalizeTask(data)
}

export async function createTask(request: CreateTaskRequest): Promise<TaskItem> {
  const response = await apiClient.post<TaskItem | ApiEnvelope<TaskItem>>('/api/tasks', request)
  return normalizeTask(unwrapApiData(response.data))
}

export async function updateTask(id: number | string, request: UpdateTaskRequest): Promise<TaskItem> {
  const response = await apiClient.put<TaskItem | ApiEnvelope<TaskItem>>(`/api/tasks/${id}`, request)
  return normalizeTask(unwrapApiData(response.data))
}

export async function deleteTask(id: number | string): Promise<void> {
  await apiClient.delete(`/api/tasks/${id}`)
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

export async function fetchAttachments(taskId: number | string): Promise<TaskAttachment[]> {
  const response = await apiClient.get<TaskAttachment[]>(`/api/tasks/${taskId}/attachments`)
  const data = unwrapApiData(response.data)
  return Array.isArray(data) ? data : []
}

export async function uploadAttachment(taskId: number | string, file: File): Promise<TaskAttachment> {
  const formData = new FormData()
  formData.append('file', file)

  const response = await apiClient.post<TaskAttachment>(`/api/tasks/${taskId}/attachments`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })

  return unwrapApiData(response.data)
}

function resolveDownloadFileName(contentDisposition: string | undefined, fallback: string) {
  if (!contentDisposition) {
    return fallback
  }

  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1])
  }

  const plainMatch = contentDisposition.match(/filename=\"?([^\";]+)\"?/i)
  return plainMatch?.[1] ?? fallback
}

export async function downloadAttachment(attachmentId: number | string, fallbackFileName: string): Promise<void> {
  const response = await apiClient.get<Blob>(`/api/attachments/${attachmentId}/download`, {
    responseType: 'blob',
  })
  const fileName = resolveDownloadFileName(response.headers['content-disposition'], fallbackFileName)
  const objectUrl = window.URL.createObjectURL(response.data)
  const anchor = document.createElement('a')
  anchor.href = objectUrl
  anchor.download = fileName
  document.body.append(anchor)
  anchor.click()
  anchor.remove()
  window.URL.revokeObjectURL(objectUrl)
}

export async function deleteAttachment(attachmentId: number | string): Promise<void> {
  await apiClient.delete(`/api/attachments/${attachmentId}`)
}

export async function fetchActivities(taskId: number | string, size = 100): Promise<ActivityItem[]> {
  const response = await apiClient.get<PageResponse<ActivityItem>>(`/api/tasks/${taskId}/activities`, {
    params: { page: 0, size },
  })
  const data = unwrapApiData(response.data)
  return Array.isArray(data.content) ? data.content : []
}

export async function fetchNotifications(unreadOnly = false, page = 0, size = 20): Promise<PageResponse<NotificationItem>> {
  const response = await apiClient.get<PageResponse<NotificationItem>>('/api/notifications', {
    params: { page, size, unreadOnly },
  })
  return unwrapApiData(response.data)
}

export async function fetchUnreadCount(): Promise<number> {
  const response = await apiClient.get<UnreadCountResponse>('/api/notifications/unread-count')
  return unwrapApiData(response.data).unreadCount ?? 0
}

export async function markNotificationAsRead(notificationId: number | string): Promise<NotificationItem> {
  const response = await apiClient.patch<NotificationItem>(`/api/notifications/${notificationId}/read`)
  return unwrapApiData(response.data)
}

export async function markAllNotificationsAsRead(): Promise<void> {
  await apiClient.patch('/api/notifications/read-all')
}
