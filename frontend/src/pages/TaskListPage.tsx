import type { TaskItem } from '../lib/taskApi'
import type { TeamSummary } from '../types/team'
import { TaskShell } from '../components/TaskShell'
import { formatDate } from '../utils/format'

function badgeClass(base: string, value?: string | null) {
  const normalized = value ? String(value).toLowerCase() : 'empty'
  return `${base} ${base}-${normalized}`
}

/**
 * タスク一覧画面に表示する一覧データ、フィルタ状態、画面操作。
 */
type Props = {
  activePath: string
  currentUserLabel: string
  unreadCount: number
  onNavigate: (path: string) => void
  onLogout: () => void
  onShowCreate: () => void
  onShowTeamDetail: (teamId: number | string) => void
  onReload: () => void
  onShowDetail: (taskId: number | string) => void
  contextTeamId: string | null
  teams: TeamSummary[]
  isLoadingTeams: boolean
  tasks: TaskItem[]
  filteredTasks: TaskItem[]
  taskErrorMessage: string
  successMessage: string
  isLoadingTasks: boolean
  statusFilter: string
  priorityFilter: string
  teamFilter: string
  onStatusFilterChange: (value: string) => void
  onPriorityFilterChange: (value: string) => void
  onTeamFilterChange: (value: string) => void
  statusOptions: Array<{ label: string; value: string }>
  priorityOptions: Array<{ label: string; value: string }>
}

/**
 * タスク一覧、フィルタ、表示文脈、詳細画面への導線を表示するページ。
 */
export function TaskListPage(props: Props) {
  const contextTeam = props.contextTeamId
    ? props.teams.find((team) => String(team.id) === String(props.contextTeamId))
    : null
  const contextTeamName = contextTeam?.name ?? props.tasks.find((task) => String(task.teamId) === String(props.contextTeamId))?.teamName
  const contextLabel = props.contextTeamId ? contextTeamName ?? props.contextTeamId : '所属全チーム'

  return (
    <TaskShell
      title="タスク一覧"
      description={props.contextTeamId ? `${contextLabel}のタスクを表示しています` : '所属全チームのタスクを表示しています'}
      activePath={props.activePath}
      onNavigate={props.onNavigate}
      onLogout={props.onLogout}
      currentUserLabel={props.currentUserLabel}
      unreadCount={props.unreadCount}
      actions={
        <>
          {props.contextTeamId ? (
            <button className="primary-button" onClick={props.onShowCreate} type="button">タスクを作成する</button>
          ) : null}
          <button className="secondary-button" onClick={props.onReload} type="button">再読込</button>
        </>
      }
      preHeader={
        <div className="task-context-banner">
          <span className="context-summary">
            <span className="summary-label">{props.contextTeamId ? 'チーム:' : '表示対象:'}</span>
            <strong>{contextLabel}</strong>
          </span>
          {props.contextTeamId ? (
            <button className="context-link-button" onClick={() => props.onShowTeamDetail(props.contextTeamId as string)} type="button">
              チーム詳細へ戻る
            </button>
          ) : null}
        </div>
      }
    >
      {props.taskErrorMessage ? <div className="status-box error-box">{props.taskErrorMessage}</div> : null}
      {props.successMessage ? <div className="status-box success-box">{props.successMessage}</div> : null}

      <section className="panel section-panel">
        <div className="filter-grid compact-filter-grid">
          {!props.contextTeamId ? (
            <label>
              <span>チーム</span>
              <select disabled={props.isLoadingTeams} value={props.teamFilter} onChange={(e) => props.onTeamFilterChange(e.target.value)}>
                <option value="ALL">すべて</option>
                {props.teams.map((team) => (
                  <option key={String(team.id)} value={String(team.id)}>
                    {team.name}
                  </option>
                ))}
              </select>
            </label>
          ) : null}
          <label>
            <span>ステータス</span>
            <select value={props.statusFilter} onChange={(e) => props.onStatusFilterChange(e.target.value)}>
              {props.statusOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
            </select>
          </label>
          <label>
            <span>優先度</span>
            <select value={props.priorityFilter} onChange={(e) => props.onPriorityFilterChange(e.target.value)}>
              {props.priorityOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
            </select>
          </label>
        </div>

        <div className="table-card task-table-card">
          {props.isLoadingTasks ? (
            <p className="empty-message padded-message">タスクを読み込み中です...</p>
          ) : props.filteredTasks.length === 0 ? (
            <p className="empty-message padded-message">条件に一致するタスクはありません。</p>
          ) : (
            <div className="table-scroll">
              <table>
                <thead>
                  <tr><th>タスク名</th><th>チーム</th><th>ステータス</th><th>優先度</th><th>担当者</th><th>期限</th></tr>
                </thead>
                <tbody>
                  {props.filteredTasks.map((task) => (
                    <tr key={String(task.id)}>
                      <td>
                        <div className="title-cell">
                          <span className="task-title-meta">#{task.id}</span>
                          <button className="task-title-link-button" onClick={() => props.onShowDetail(task.id)} type="button">
                            {task.title}
                          </button>
                          {task.description ? <span>{task.description}</span> : null}
                        </div>
                      </td>
                      <td><span className="team-name-cell">{task.teamName ?? '-'}</span></td>
                      <td><span className={badgeClass('status-badge', task.status)}>{task.status ?? '-'}</span></td>
                      <td><span className={badgeClass('priority-badge', task.priority)}>{task.priority ?? '-'}</span></td>
                      <td>{task.assignedUserName ?? '-'}</td>
                      <td>{formatDate(task.dueDate)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </section>
    </TaskShell>
  )
}
