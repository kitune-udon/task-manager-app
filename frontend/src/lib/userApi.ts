import { apiClient } from './apiClient'

/**
 * タスク担当者として選択できるユーザー。
 */
export type AssignableUser = {
  id: number | string
  name: string
  email: string
}

/**
 * タスク担当者候補のユーザー一覧を取得する。
 */
export async function fetchAssignableUsers(): Promise<AssignableUser[]> {
  const response = await apiClient.get<AssignableUser[]>('/api/users')
  // 想定外のレスポンスでもselect生成側を壊さないよう空配列へ丸める。
  return Array.isArray(response.data) ? response.data : []
}
