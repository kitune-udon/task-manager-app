import { apiClient } from './apiClient'
import { unwrapApiData } from './apiTypes'
import type { PageResponse } from './taskApi'

export type NotificationItem = {
  id: number
  activityLogId: number
  eventType: string
  message: string
  relatedTaskId?: number | null
  relatedTaskTitle?: string | null
  targetType?: string | null
  targetId?: number | null
  detailJson?: unknown
  isRead: boolean
  readAt?: string | null
  createdAt?: string | null
}

export type UnreadCountResponse = {
  unreadCount: number
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
