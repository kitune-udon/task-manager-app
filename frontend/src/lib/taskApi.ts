import { apiClient } from './apiClient'
import { unwrapApiData } from './apiTypes'
import type { ApiEnvelope } from './apiTypes'

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

export type UpdateTaskRequest = CreateTaskRequest

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
  const response = await apiClient.get<ApiEnvelope<TaskItem[]> | TaskItem[]>('/api/tasks')
  const data = unwrapApiData(response.data)

  if (!Array.isArray(data)) {
    return []
  }

  return data.map((task) => normalizeTask(task))
}

export async function fetchTaskById(id: number | string): Promise<TaskItem | null> {
  const response = await apiClient.get<ApiEnvelope<TaskItem> | TaskItem>(`/api/tasks/${id}`)
  const data = unwrapApiData(response.data)

  if (!data) {
    return null
  }

  return normalizeTask(data)
}

export async function createTask(request: CreateTaskRequest): Promise<TaskItem> {
  const response = await apiClient.post<ApiEnvelope<TaskItem> | TaskItem>('/api/tasks', request)
  const data = unwrapApiData(response.data)
  return normalizeTask(data)
}

export async function updateTask(id: number | string, request: UpdateTaskRequest): Promise<TaskItem> {
  const response = await apiClient.put<ApiEnvelope<TaskItem> | TaskItem>(`/api/tasks/${id}`, request)
  const data = unwrapApiData(response.data)
  return normalizeTask(data)
}

export async function deleteTask(id: number | string): Promise<void> {
  await apiClient.delete(`/api/tasks/${id}`)
}
