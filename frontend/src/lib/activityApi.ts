import { apiClient } from './apiClient'
import { unwrapApiData } from './apiTypes'
import type { PageResponse, TaskUser } from './taskApi'

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

export async function fetchActivities(taskId: number | string, size = 100): Promise<ActivityItem[]> {
  const response = await apiClient.get<PageResponse<ActivityItem>>(`/api/tasks/${taskId}/activities`, {
    params: { page: 0, size },
  })
  const data = unwrapApiData(response.data)
  return Array.isArray(data.content) ? data.content : []
}
