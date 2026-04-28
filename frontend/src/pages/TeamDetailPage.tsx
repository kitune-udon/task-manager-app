import { TaskShell } from '../components/TaskShell'
import { AddMemberModal } from '../components/team/AddMemberModal'
import { ChangeRoleModal } from '../components/team/ChangeRoleModal'
import { RemoveMemberDialog } from '../components/team/RemoveMemberDialog'
import type { UseTeamStateResult } from '../hooks/useTeamState'
import type { TeamMember } from '../types/team'
import { formatDateTime } from '../utils/format'

function roleBadgeClass(role?: string | null) {
  return `badge role-badge role-${String(role ?? 'empty').toLowerCase()}`
}

function roleLabel(role?: string | null) {
  return role ?? '-'
}

/**
 * チーム詳細画面に表示する詳細データ、メンバー管理状態、画面操作。
 */
type Props = {
  activePath: string
  currentUserLabel: string
  unreadCount: number
  currentUserId: number | null
  successMessage: string
  teamId: string
  teamState: UseTeamStateResult
  onNavigate: (path: string) => void
  onLogout: () => void
}

/**
 * チーム詳細、メンバー一覧、メンバー追加/変更/削除導線を表示するページ。
 */
export function TeamDetailPage(props: Props) {
  const { detail, addMember, changeRole, removeMember, actions } = props.teamState
  const team = detail.selectedTeam
  const canAddMembers = team?.myRole === 'OWNER' || team?.myRole === 'ADMIN'
  const canChangeRole = team?.myRole === 'OWNER'
  const canRemoveMembers = team?.myRole === 'OWNER' || team?.myRole === 'ADMIN'

  const renderMemberActions = (member: TeamMember) => {
    if (member.role === 'OWNER') {
      return <span className="empty-message">-</span>
    }

    const isSelf = props.currentUserId !== null && String(member.userId) === String(props.currentUserId)

    return (
      <div className="member-action-row">
        {canChangeRole ? (
          <button className="table-action-button" onClick={() => actions.openChangeRoleModal(member)} type="button">
            ロール変更
          </button>
        ) : null}
        {canRemoveMembers ? (
          <button className="table-action-button" onClick={() => actions.openRemoveMemberDialog(member)} type="button">
            {isSelf && member.role === 'ADMIN' ? 'チームから外れる' : '削除'}
          </button>
        ) : null}
        {!canChangeRole && !canRemoveMembers ? <span className="empty-message">-</span> : null}
      </div>
    )
  }

  return (
    <>
      <TaskShell
        title={team?.name ?? 'チーム詳細'}
        description={team?.description || '説明はありません。'}
        activePath={props.activePath}
        onNavigate={props.onNavigate}
        onLogout={props.onLogout}
        currentUserLabel={props.currentUserLabel}
        unreadCount={props.unreadCount}
        contentAreaClassName="team-detail-page"
        preHeader={
          <button className="context-link-button page-back-link" onClick={() => props.onNavigate('/teams')} type="button">
            チーム一覧へ戻る
          </button>
        }
      >
        {props.successMessage ? <div className="status-box success-box">{props.successMessage}</div> : null}

        {detail.teamDetailErrorMessage ? (
          <section className="panel section-panel">
            <div className="status-box error-box">{detail.teamDetailErrorMessage}</div>
            <div className="header-actions">
              <button className="secondary-button" onClick={() => props.onNavigate('/teams')} type="button">
                チーム一覧へ戻る
              </button>
              <button className="primary-button" onClick={() => void detail.loadSelectedTeam(props.teamId)} type="button">
                再読み込み
              </button>
            </div>
          </section>
        ) : detail.isLoadingTeamDetail && !team ? (
          <section className="panel section-panel">
            <p className="empty-message padded-message">チーム詳細を読み込み中です...</p>
          </section>
        ) : team ? (
          <>
            <section className="panel section-panel team-detail-overview-panel">
              <div className="team-detail-inline-meta">
                <span className="team-detail-meta-item-inline">
                  <span className="summary-label">自分のロール:</span>
                  <span className={roleBadgeClass(team.myRole)}>{roleLabel(team.myRole)}</span>
                </span>
                <span className="team-detail-meta-item-inline">
                  <span className="summary-label">メンバー数:</span>
                  <strong>{team.memberCount}名</strong>
                </span>
              </div>

              <div className="team-detail-action-row">
                {canAddMembers ? (
                  <button className="primary-button" onClick={actions.openAddMemberModal} type="button">
                    メンバーを追加
                  </button>
                ) : null}
                <button className="secondary-button" onClick={() => props.onNavigate(`/tasks?teamId=${team.id}`)} type="button">
                  このチームのタスクを見る
                </button>
              </div>

              <dl className="team-detail-date-list">
                <div>
                  <dt>作成日時</dt>
                  <dd>{formatDateTime(team.createdAt)}</dd>
                </div>
                <div>
                  <dt>更新日時</dt>
                  <dd>{formatDateTime(team.updatedAt)}</dd>
                </div>
              </dl>
            </section>

            <section className="panel section-panel">
              <div className="team-section-header">
                <div>
                  <h2>メンバー一覧</h2>
                  <p className="subtext">所属メンバーとロールを確認できます。</p>
                </div>
                <button className="secondary-button" onClick={() => void detail.loadMembers(props.teamId)} type="button">
                  再読み込み
                </button>
              </div>

              {detail.memberListErrorMessage ? (
                <div className="status-box error-box member-list-error">
                  <span>{detail.memberListErrorMessage}</span>
                  <button className="secondary-button compact-button" onClick={() => void detail.loadMembers(props.teamId)} type="button">
                    再読み込み
                  </button>
                </div>
              ) : null}

              <div className="table-card">
                {detail.isLoadingMembers ? (
                  <p className="empty-message padded-message">メンバーを読み込み中です...</p>
                ) : detail.members.length === 0 ? (
                  <p className="empty-message padded-message">メンバーはいません。</p>
                ) : (
                  <div className="table-scroll">
                    <table>
                      <thead>
                        <tr>
                          <th>名前</th>
                          <th>メールアドレス</th>
                          <th>ロール</th>
                          <th>参加日時</th>
                          <th>操作</th>
                        </tr>
                      </thead>
                      <tbody>
                        {detail.members.map((member) => (
                          <tr key={String(member.memberId)}>
                            <td>{member.name ?? '-'}</td>
                            <td>{member.email ?? '-'}</td>
                            <td>
                              <span className={roleBadgeClass(member.role)}>{roleLabel(member.role)}</span>
                            </td>
                            <td>{formatDateTime(member.joinedAt)}</td>
                            <td>{renderMemberActions(member)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </section>
          </>
        ) : null}
      </TaskShell>

      <AddMemberModal
        availableUsers={addMember.availableUsers}
        availableUsersError={addMember.availableUsersError}
        errorMessage={addMember.errorMessage}
        fieldErrors={addMember.fieldErrors}
        isLoadingAvailableUsers={addMember.isLoadingAvailableUsers}
        isOpen={addMember.isOpen}
        isSubmitting={addMember.isSubmitting}
        onClose={actions.closeAddMemberModal}
        onReloadAvailableUsers={actions.reloadAvailableUsers}
        onRoleChange={actions.setSelectedAddRole}
        onSubmit={actions.handleAddMember}
        onUserChange={actions.setSelectedAddUserId}
        selectedRole={addMember.selectedRole}
        selectedUserId={addMember.selectedUserId}
      />
      <ChangeRoleModal
        errorMessage={changeRole.errorMessage}
        fieldErrors={changeRole.fieldErrors}
        isSubmitting={changeRole.isSubmitting}
        onClose={actions.closeChangeRoleModal}
        onRoleChange={actions.setSelectedChangeRole}
        onSubmit={actions.handleChangeRole}
        selectedRole={changeRole.selectedRole}
        targetMember={changeRole.targetMember}
      />
      <RemoveMemberDialog
        currentUserId={props.currentUserId}
        errorMessage={removeMember.errorMessage}
        isSubmitting={removeMember.isSubmitting}
        onClose={actions.closeRemoveMemberDialog}
        onConfirm={actions.handleRemoveMember}
        targetMember={removeMember.targetMember}
      />
    </>
  )
}
