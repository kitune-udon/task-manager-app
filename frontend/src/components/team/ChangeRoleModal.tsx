import type { FormEvent } from 'react'
import type { FieldErrors } from '../../lib/apiError'
import type { TeamMember, TeamRole } from '../../types/team'
import { formatTeamRole } from '../../utils/teamDisplay'

const CHANGE_ROLE_OPTIONS: Array<{ label: string; value: Exclude<TeamRole, 'OWNER'> }> = [
  { label: formatTeamRole('ADMIN'), value: 'ADMIN' },
  { label: formatTeamRole('MEMBER'), value: 'MEMBER' },
]

/**
 * ロール変更モーダルに表示する対象メンバー、フォーム状態、操作。
 */
type Props = {
  targetMember: TeamMember | null
  selectedRole: Exclude<TeamRole, 'OWNER'>
  fieldErrors: FieldErrors
  errorMessage: string
  isSubmitting: boolean
  onRoleChange: (value: Exclude<TeamRole, 'OWNER'>) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  onClose: () => void
}

/**
 * チームメンバーの ADMIN / MEMBER ロールを変更するモーダル。
 */
export function ChangeRoleModal({
  targetMember,
  selectedRole,
  fieldErrors,
  errorMessage,
  isSubmitting,
  onRoleChange,
  onSubmit,
  onClose,
}: Props) {
  if (!targetMember) {
    return null
  }

  return (
    <div className="modal-backdrop" role="presentation">
      <section aria-modal="true" className="modal-panel" role="dialog">
        <header className="modal-header">
          <div>
            <h2>ロール変更</h2>
            <p className="subtext">{targetMember.name ?? targetMember.email ?? '対象メンバー'}</p>
          </div>
          <button className="text-link-button modal-close-button" disabled={isSubmitting} onClick={onClose} type="button">
            閉じる
          </button>
        </header>

        {errorMessage ? <div className="status-box error-box">{errorMessage}</div> : null}

        <form className="form-grid auth-form" onSubmit={onSubmit}>
          <div className="detail-field-stack">
            <span className="summary-label">メールアドレス</span>
            <strong>{targetMember.email ?? '-'}</strong>
          </div>
          <div className="detail-field-stack">
            <span className="summary-label">現在のロール</span>
            <strong>{formatTeamRole(targetMember.role)}</strong>
          </div>

          <label>
            <span>変更後ロール</span>
            <select
              className={fieldErrors.role ? 'input-error' : ''}
              onChange={(event) => onRoleChange(event.target.value as Exclude<TeamRole, 'OWNER'>)}
              value={selectedRole}
            >
              {CHANGE_ROLE_OPTIONS.map((option) => (
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
            <button className="primary-button" disabled={isSubmitting} type="submit">
              {isSubmitting ? '変更中...' : '変更する'}
            </button>
          </div>
        </form>
      </section>
    </div>
  )
}
