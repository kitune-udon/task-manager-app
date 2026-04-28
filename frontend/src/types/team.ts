/**
 * チーム内での権限ロール。
 */
export type TeamRole = 'OWNER' | 'ADMIN' | 'MEMBER'

/**
 * 所属チーム一覧で扱うチーム概要。
 */
export type TeamSummary = {
  id: number | string
  name: string
  description?: string | null
  myRole: TeamRole
  memberCount: number
  updatedAt?: string | null
}

/**
 * チーム詳細画面で扱うチーム情報。
 */
export type TeamDetail = TeamSummary & {
  createdAt?: string | null
}

/**
 * チーム所属メンバー。
 */
export type TeamMember = {
  memberId: number | string
  userId: number | string
  name?: string | null
  email?: string | null
  role: TeamRole
  joinedAt?: string | null
}

/**
 * チームへ追加可能なユーザー。
 */
export type AvailableUser = {
  userId: number | string
  name?: string | null
  email?: string | null
}

/**
 * チーム作成APIへ送るリクエスト。
 */
export type CreateTeamRequest = {
  name: string
  description?: string
}

/**
 * チームメンバー追加APIへ送るリクエスト。
 */
export type AddTeamMemberRequest = {
  userId: number | string
  role: TeamRole
}

/**
 * チームメンバーのロール変更APIへ送るリクエスト。
 */
export type UpdateTeamMemberRoleRequest = {
  role: TeamRole
}
