import { apiClient } from './apiClient'
import { unwrapApiData, type ApiEnvelope } from './apiTypes'
import type {
  AddTeamMemberRequest,
  AvailableUser,
  CreateTeamRequest,
  TeamDetail,
  TeamMember,
  TeamSummary,
  UpdateTeamMemberRoleRequest,
} from '../types/team'

/**
 * チームを作成する。
 */
export async function createTeam(request: CreateTeamRequest): Promise<TeamDetail> {
  const response = await apiClient.post<TeamDetail | ApiEnvelope<TeamDetail>>('/api/teams', request)
  return unwrapApiData(response.data)
}

/**
 * ログインユーザーが所属するチーム一覧を取得する。
 */
export async function fetchTeams(): Promise<TeamSummary[]> {
  const response = await apiClient.get<TeamSummary[] | ApiEnvelope<TeamSummary[]>>('/api/teams')
  const data = unwrapApiData(response.data)
  return Array.isArray(data) ? data : []
}

/**
 * 指定チームの詳細を取得する。
 */
export async function fetchTeamDetail(teamId: number | string): Promise<TeamDetail | null> {
  const response = await apiClient.get<TeamDetail | ApiEnvelope<TeamDetail>>(`/api/teams/${teamId}`)
  return unwrapApiData(response.data) ?? null
}

/**
 * 指定チームの所属メンバー一覧を取得する。
 */
export async function fetchTeamMembers(teamId: number | string): Promise<TeamMember[]> {
  const response = await apiClient.get<TeamMember[] | ApiEnvelope<TeamMember[]>>(`/api/teams/${teamId}/members`)
  const data = unwrapApiData(response.data)
  return Array.isArray(data) ? data : []
}

/**
 * 指定チームへ追加可能なユーザー一覧を取得する。
 */
export async function fetchAvailableUsers(teamId: number | string): Promise<AvailableUser[]> {
  const response = await apiClient.get<AvailableUser[] | ApiEnvelope<AvailableUser[]>>(`/api/teams/${teamId}/available-users`)
  const data = unwrapApiData(response.data)
  return Array.isArray(data) ? data : []
}

/**
 * 指定チームへメンバーを追加する。
 */
export async function addTeamMember(teamId: number | string, request: AddTeamMemberRequest): Promise<TeamMember> {
  const response = await apiClient.post<TeamMember | ApiEnvelope<TeamMember>>(`/api/teams/${teamId}/members`, request)
  return unwrapApiData(response.data)
}

/**
 * 指定チームメンバーのロールを変更する。
 */
export async function updateTeamMemberRole(
  teamId: number | string,
  memberId: number | string,
  request: UpdateTeamMemberRoleRequest,
): Promise<TeamMember> {
  const response = await apiClient.patch<TeamMember | ApiEnvelope<TeamMember>>(
    `/api/teams/${teamId}/members/${memberId}`,
    request,
  )
  return unwrapApiData(response.data)
}

/**
 * 指定チームからメンバーを削除する。
 */
export async function removeTeamMember(teamId: number | string, memberId: number | string): Promise<void> {
  await apiClient.delete(`/api/teams/${teamId}/members/${memberId}`)
}
