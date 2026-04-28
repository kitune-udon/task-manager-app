import type { TeamRole } from '../types/team'

/**
 * チームロールの表示名。
 */
export function formatTeamRole(role?: TeamRole | null) {
  switch (role) {
    case 'OWNER':
      return 'オーナー'
    case 'ADMIN':
      return '管理者'
    case 'MEMBER':
      return 'メンバー'
    default:
      return '-'
  }
}

/**
 * ユーザー表示名とメールアドレスを組み合わせたラベル。
 */
export function formatTeamUserLabel(name?: string | null, email?: string | null) {
  const resolvedName = name?.trim()
  const resolvedEmail = email?.trim()

  if (resolvedName && resolvedEmail) {
    return `${resolvedName} (${resolvedEmail})`
  }

  return resolvedName || resolvedEmail || '-'
}
