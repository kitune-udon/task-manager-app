import type { TeamMember } from '../../types/team'
import { formatTeamRole } from '../../utils/teamDisplay'

/**
 * メンバー削除確認ダイアログに表示する対象、状態、操作。
 */
type Props = {
  targetMember: TeamMember | null
  currentUserId: number | null
  errorMessage: string
  isSubmitting: boolean
  onConfirm: () => void
  onClose: () => void
}

/**
 * チームメンバー削除を確認するダイアログ。
 */
export function RemoveMemberDialog({
  targetMember,
  currentUserId,
  errorMessage,
  isSubmitting,
  onConfirm,
  onClose,
}: Props) {
  if (!targetMember) {
    return null
  }

  const isSelfAdminRemove = targetMember.role === 'ADMIN' && currentUserId !== null && String(targetMember.userId) === String(currentUserId)
  const confirmLabel = isSelfAdminRemove ? 'チームから外れる' : '削除する'
  const message = isSelfAdminRemove ? 'チームから外れますか？' : 'このメンバーをチームから外しますか？'

  return (
    <div className="modal-backdrop" role="presentation">
      <section aria-modal="true" className="modal-panel compact-modal-panel" role="dialog">
        <header className="modal-header">
          <div>
            <h2>{confirmLabel}</h2>
            <p className="subtext">{message}</p>
          </div>
        </header>

        {errorMessage ? <div className="status-box error-box">{errorMessage}</div> : null}

        <div className="member-confirm-card">
          <strong>{targetMember.name ?? targetMember.email ?? '-'}</strong>
          <span>{targetMember.email ?? '-'}</span>
          <span className="badge">{formatTeamRole(targetMember.role)}</span>
        </div>

        <div className="form-actions split-actions">
          <button className="secondary-button" disabled={isSubmitting} onClick={onClose} type="button">
            キャンセル
          </button>
          <button className="primary-button danger-button" disabled={isSubmitting} onClick={onConfirm} type="button">
            {isSubmitting ? '処理中...' : confirmLabel}
          </button>
        </div>
      </section>
    </div>
  )
}
