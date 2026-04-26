import { TaskShell } from '../components/TaskShell'
import { CreateTeamModal } from '../components/team/CreateTeamModal'
import type { UseTeamStateResult } from '../hooks/useTeamState'
import { formatDateTime } from '../utils/format'
import { formatTeamRole } from '../utils/teamDisplay'

function roleBadgeClass(role?: string | null) {
  return `badge role-badge role-${String(role ?? 'empty').toLowerCase()}`
}

/**
 * チーム一覧画面に表示する一覧データ、作成モーダル状態、画面操作。
 */
type Props = {
  activePath: string
  currentUserLabel: string
  unreadCount: number
  successMessage: string
  teamState: UseTeamStateResult
  onNavigate: (path: string) => void
  onLogout: () => void
}

/**
 * 所属チーム一覧、チーム作成、詳細画面への導線を表示するページ。
 */
export function TeamListPage(props: Props) {
  const { list, create, actions } = props.teamState
  const hasTeams = list.teams.length > 0

  return (
    <>
      <TaskShell
        title="チーム一覧"
        description="所属しているチームを選択して、詳細やタスクを確認できます。"
        activePath={props.activePath}
        onNavigate={props.onNavigate}
        onLogout={props.onLogout}
        currentUserLabel={props.currentUserLabel}
        unreadCount={props.unreadCount}
        actions={
          <>
            <button className="primary-button" onClick={actions.openCreateTeamModal} type="button">
              チームを作成する
            </button>
            <button className="secondary-button" onClick={() => void list.loadTeams()} type="button">
              再読込
            </button>
          </>
        }
      >
        {list.teamErrorMessage ? <div className="status-box error-box">{list.teamErrorMessage}</div> : null}
        {props.successMessage ? <div className="status-box success-box">{props.successMessage}</div> : null}

        <section className="panel section-panel">
          {list.isLoadingTeams ? (
            <p className="empty-message padded-message">チームを読み込み中です...</p>
          ) : !hasTeams ? (
            <div className="empty-state-panel">
              <h2>所属しているチームがありません</h2>
              <p className="empty-message">まずはチームを作成してください</p>
              <button className="primary-button" onClick={actions.openCreateTeamModal} type="button">
                チームを作成する
              </button>
            </div>
          ) : (
            <div className="team-card-grid">
              {list.teams.map((team) => (
                <article className="team-card" key={String(team.id)}>
                  <div className="team-card-header">
                    <div>
                      <h2>{team.name}</h2>
                      <p className="team-description">{team.description || '説明はありません。'}</p>
                    </div>
                    <span className={roleBadgeClass(team.myRole)}>{formatTeamRole(team.myRole)}</span>
                  </div>

                  <dl className="team-meta-grid">
                    <div>
                      <dt>メンバー数</dt>
                      <dd>{team.memberCount}</dd>
                    </div>
                    <div>
                      <dt>更新日時</dt>
                      <dd>{formatDateTime(team.updatedAt)}</dd>
                    </div>
                  </dl>

                  <div className="team-card-actions">
                    <button className="table-action-button" onClick={() => actions.handleShowTeamDetail(team.id)} type="button">
                      詳細を見る
                    </button>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      </TaskShell>

      <CreateTeamModal
        errorMessage={create.errorMessage}
        fieldErrors={create.form.fieldErrors}
        form={create.form}
        isOpen={create.isOpen}
        isSubmitting={create.isSubmitting}
        onClose={actions.closeCreateTeamModal}
        onDescriptionChange={create.form.onDescriptionChange}
        onNameChange={create.form.onNameChange}
        onSubmit={actions.handleCreateTeam}
      />
    </>
  )
}
