import { apiClient } from './apiClient'
import { unwrapApiData, type ApiEnvelope } from './apiTypes'

/**
 * タスクの進行状態。
 */
export type TaskStatus = 'TODO' | 'DOING' | 'DONE' | string

/**
 * タスクの優先度。
 */
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | string

/**
 * タスクに関連するユーザーの表示情報。
 */
export type TaskUser = {
  id?: number | string | null
  name?: string | null
  email?: string | null
}

/**
 * 一覧・詳細画面で扱うタスク情報。
 */
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

/**
 * タスク作成APIへ送るリクエスト。
 */
export type CreateTaskRequest = {
  title: string
  description?: string
  status: TaskStatus
  priority: TaskPriority
  dueDate?: string
  assignedUserId?: number
  teamId?: number
}

/**
 * タスク更新APIへ送るリクエスト。
 */
export type UpdateTaskRequest = CreateTaskRequest & {
  /** 楽観ロックの競合検知に使う現在バージョン。 */
  version: number
}

/**
 * ページングAPIの共通レスポンス。
 */
export type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/**
 * APIレスポンスのユーザー関連フィールドを画面で扱いやすい形へ補正する。
 */
function normalizeTask(raw: TaskItem): TaskItem {
  const assignedUser = raw.assignedUser ?? null
  const createdBy = raw.createdBy ?? null

  return {
    ...raw,
    // バックエンドの返却形式がネスト/フラットのどちらでも画面側は同じプロパティを参照できるようにする。
    assignedUser,
    assignedUserId: raw.assignedUserId ?? assignedUser?.id ?? null,
    assignedUserName: raw.assignedUserName ?? assignedUser?.name ?? null,
    createdBy,
    createdById: raw.createdById ?? createdBy?.id ?? null,
    createdByName: raw.createdByName ?? createdBy?.name ?? null,
  }
}

/**
 * ログインユーザーが参照可能なタスク一覧を取得する。
 */
export async function fetchTasks(): Promise<TaskItem[]> {
  const response = await apiClient.get<TaskItem[]>('/api/tasks')
  const data = unwrapApiData(response.data)

  if (!Array.isArray(data)) {
    return []
  }

  return data.map((task) => normalizeTask(task))
}

/**
 * 指定IDのタスク詳細を取得する。
 */
export async function fetchTaskById(id: number | string): Promise<TaskItem | null> {
  const response = await apiClient.get<TaskItem | ApiEnvelope<TaskItem>>(`/api/tasks/${id}`)
  const data = unwrapApiData(response.data)

  if (!data) {
    return null
  }

  return normalizeTask(data)
}

/**
 * タスクを作成する。
 */
export async function createTask(request: CreateTaskRequest): Promise<TaskItem> {
  const response = await apiClient.post<TaskItem | ApiEnvelope<TaskItem>>('/api/tasks', request)
  return normalizeTask(unwrapApiData(response.data))
}

/**
 * タスクを更新する。
 */
export async function updateTask(id: number | string, request: UpdateTaskRequest): Promise<TaskItem> {
  const response = await apiClient.put<TaskItem | ApiEnvelope<TaskItem>>(`/api/tasks/${id}`, request)
  return normalizeTask(unwrapApiData(response.data))
}

/**
 * タスクを削除する。
 */
export async function deleteTask(id: number | string): Promise<void> {
  await apiClient.delete(`/api/tasks/${id}`)
}
