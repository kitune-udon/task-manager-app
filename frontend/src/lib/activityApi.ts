import { apiClient } from './apiClient'
import { unwrapApiData } from './apiTypes'
import type { PageResponse, TaskUser } from './taskApi'

/**
 * タスク履歴として表示するアクティビティログ。
 */
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

/**
 * 指定タスクのアクティビティ一覧を取得する。
 */
export async function fetchActivities(taskId: number | string, size = 100): Promise<ActivityItem[]> {
  // 詳細画面では直近履歴をまとめて表示するため、先頭ページを指定件数分だけ取得する。
  const response = await apiClient.get<PageResponse<ActivityItem>>(`/api/tasks/${taskId}/activities`, {
    params: { page: 0, size },
  })
  const data = unwrapApiData(response.data)
  return Array.isArray(data.content) ? data.content : []
}
