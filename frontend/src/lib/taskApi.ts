import { apiClient } from './apiClient'
import { unwrapApiData } from './apiTypes'
import type { ApiEnvelope } from './apiTypes'

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE' | string
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | string

export type TaskItem = {
  id: number | string
  title: string
  description?: string
  status?: TaskStatus
  priority?: TaskPriority
  assignedUserId?: number | string | null
  assignedUserName?: string | null
  assigneeName?: string
  assignee?: {
    id?: number | string
    name?: string
    email?: string
  }
  dueDate?: string
  createdAt?: string
  updatedAt?: string
}

export type CreateTaskRequest = {
  title: string
  description?: string
  status: TaskStatus
  priority: TaskPriority
  dueDate?: string
  assignedUserId?: number
}

function normalizeTask(raw: TaskItem): TaskItem {
  return {
    ...raw,
    assignedUserName: raw.assignedUserName ?? raw.assigneeName ?? raw.assignee?.name ?? null,
  }
}

export async function fetchTasks(): Promise<TaskItem[]> {
  const response = await apiClient.get<ApiEnvelope<TaskItem[]> | TaskItem[]>('/api/tasks')
  const data = unwrapApiData(response.data)

  if (!Array.isArray(data)) {
    return []
  }

  return data.map((task) => normalizeTask(task))
}

export async function fetchTaskById(id: number | string): Promise<TaskItem> {
  const response = await apiClient.get<ApiEnvelope<TaskItem> | TaskItem>(`/api/tasks/${id}`)
  const data = unwrapApiData(response.data)
  return normalizeTask(data)
}

export async function createTask(request: CreateTaskRequest): Promise<TaskItem> {
  const response = await apiClient.post<ApiEnvelope<TaskItem> | TaskItem>('/api/tasks', request)
  const data = unwrapApiData(response.data)

  return normalizeTask(data)
}
