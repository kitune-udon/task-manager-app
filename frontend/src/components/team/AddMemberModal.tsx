import type { FormEvent } from 'react'
import type { FieldErrors } from '../../lib/apiError'
import type { AvailableUser, TeamRole } from '../../types/team'
import { formatTeamRole, formatTeamUserLabel } from '../../utils/teamDisplay'

const MEMBER_ROLE_OPTIONS: Array<{ label: string; value: Exclude<TeamRole, 'OWNER'> }> = [
  { label: formatTeamRole('MEMBER'), value: 'MEMBER' },
  { label: formatTeamRole('ADMIN'), value: 'ADMIN' },
]

/**
 * チームメンバー追加モーダルに表示する状態と操作。
 */
type Props = {
  isOpen: boolean
  availableUsers: AvailableUser[]
  selectedUserId: string
  selectedRole: Exclude<TeamRole, 'OWNER'>
  fieldErrors: FieldErrors
  errorMessage: string
  availableUsersError: string
  isLoadingAvailableUsers: boolean
  isSubmitting: boolean
  onUserChange: (value: string) => void
  onRoleChange: (value: Exclude<TeamRole, 'OWNER'>) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  onReloadAvailableUsers: () => void
  onClose: () => void
}

/**
 * 未所属ユーザーを選択してチームへ追加するモーダル。
 */
export function AddMemberModal({
  isOpen,
  availableUsers,
  selectedUserId,
  selectedRole,
  fieldErrors,
  errorMessage,
  availableUsersError,
  isLoadingAvailableUsers,
  isSubmitting,
  onUserChange,
  onRoleChange,
  onSubmit,
  onReloadAvailableUsers,
  onClose,
}: Props) {
  if (!isOpen) {
    return null
  }

  const cannotSubmit = isSubmitting || isLoadingAvailableUsers || Boolean(availableUsersError) || availableUsers.length === 0

  return (
    <div className="modal-backdrop" role="presentation">
      <section aria-modal="true" className="modal-panel" role="dialog">
        <header className="modal-header">
          <div>
            <h2>メンバー追加</h2>
            <p className="subtext">追加するユーザーとロールを選択してください。</p>
          </div>
          <button className="text-link-button modal-close-button" disabled={isSubmitting} onClick={onClose} type="button">
            閉じる
          </button>
        </header>

        {errorMessage ? <div className="status-box error-box">{errorMessage}</div> : null}
        {availableUsersError ? (
          <div className="status-box error-box modal-inline-status">
            <span>{availableUsersError}</span>
            <button className="secondary-button compact-button" onClick={onReloadAvailableUsers} type="button">
              再読み込み
            </button>
          </div>
        ) : null}

        <form className="form-grid auth-form" onSubmit={onSubmit}>
          <label>
            <span>ユーザー</span>
            <select
              className={fieldErrors.userId ? 'input-error' : ''}
              disabled={isLoadingAvailableUsers || Boolean(availableUsersError)}
              onChange={(event) => onUserChange(event.target.value)}
              value={selectedUserId}
            >
              <option value="">{isLoadingAvailableUsers ? '読み込み中...' : 'ユーザーを選択'}</option>
              {availableUsers.map((user) => (
                <option key={String(user.userId)} value={String(user.userId)}>
                  {formatTeamUserLabel(user.name, user.email)}
                </option>
              ))}
            </select>
            {fieldErrors.userId ? <span className="field-error">{fieldErrors.userId}</span> : null}
          </label>

          {!isLoadingAvailableUsers && !availableUsersError && availableUsers.length === 0 ? (
            <p className="empty-message">追加できるユーザーがいません。</p>
          ) : null}

          <label>
            <span>ロール</span>
            <select
              className={fieldErrors.role ? 'input-error' : ''}
              onChange={(event) => onRoleChange(event.target.value as Exclude<TeamRole, 'OWNER'>)}
              value={selectedRole}
            >
              {MEMBER_ROLE_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            {fieldErrors.role ? <span className="field-error">{fieldErrors.role}</span> : null}
          </label>

          <div className="form-actions split-actions">
            <button className="secondary-button" disabled={isSubmitting} onClick={onClose} type="button">
              キャンセル
            </button>
            <button className="primary-button" disabled={cannotSubmit} type="submit">
              {isSubmitting ? '追加中...' : '追加する'}
            </button>
          </div>
        </form>
      </section>
    </div>
  )
}
