import { apiClient } from './apiClient'

export type AssignableUser = {
  id: number | string
  name: string
  email: string
}

export async function fetchAssignableUsers(): Promise<AssignableUser[]> {
  const response = await apiClient.get<AssignableUser[]>('/api/users')
  return Array.isArray(response.data) ? response.data : []
}
