import { apiClient } from './apiClient'
import { unwrapApiData } from './apiTypes'
import type { PageResponse } from './taskApi'

/**
 * ユーザー宛て通知として画面に表示する情報。
 */
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

/**
 * 未読通知件数APIのレスポンス。
 */
export type UnreadCountResponse = {
  unreadCount: number
}

/**
 * 通知一覧をページングして取得する。
 */
export async function fetchNotifications(unreadOnly = false, page = 0, size = 20): Promise<PageResponse<NotificationItem>> {
  const response = await apiClient.get<PageResponse<NotificationItem>>('/api/notifications', {
    params: { page, size, unreadOnly },
  })
  return unwrapApiData(response.data)
}

/**
 * 未読通知件数を取得する。
 */
export async function fetchUnreadCount(): Promise<number> {
  const response = await apiClient.get<UnreadCountResponse>('/api/notifications/unread-count')
  // レスポンスが欠けていてもバッジ表示を壊さないよう0へ丸める。
  return unwrapApiData(response.data).unreadCount ?? 0
}

/**
 * 指定通知を既読にする。
 */
export async function markNotificationAsRead(notificationId: number | string): Promise<NotificationItem> {
  const response = await apiClient.patch<NotificationItem>(`/api/notifications/${notificationId}/read`)
  return unwrapApiData(response.data)
}

/**
 * 現在のユーザー宛て通知をすべて既読にする。
 */
export async function markAllNotificationsAsRead(): Promise<void> {
  await apiClient.patch('/api/notifications/read-all')
}
