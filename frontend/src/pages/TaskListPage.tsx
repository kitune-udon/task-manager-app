import type { TaskItem } from '../lib/taskApi'
import { TaskShell } from '../components/TaskShell'
import { formatDate } from '../utils/format'

type Props = {
  activePath: string
  currentUserLabel: string
  unreadCount: number
  onNavigate: (path: string) => void
  onLogout: () => void
  onShowCreate: () => void
  onReload: () => void
  onShowDetail: (taskId: number | string) => void
  tasks: TaskItem[]
  filteredTasks: TaskItem[]
  taskErrorMessage: string
  successMessage: string
  isLoadingTasks: boolean
  statusFilter: string
  priorityFilter: string
  onStatusFilterChange: (value: string) => void
  onPriorityFilterChange: (value: string) => void
  statusOptions: Array<{ label: string; value: string }>
  priorityOptions: Array<{ label: string; value: string }>
}

export function TaskListPage(props: Props) {
  return (
    <TaskShell
      title="タスク一覧"
      description="登録済みのタスクを一覧で確認できます。"
      activePath={props.activePath}
      onNavigate={props.onNavigate}
      onLogout={props.onLogout}
      currentUserLabel={props.currentUserLabel}
      unreadCount={props.unreadCount}
      actions={
        <>
          <button className="primary-button" onClick={props.onShowCreate} type="button">タスク作成</button>
          <button className="secondary-button" onClick={props.onReload} type="button">再読込</button>
        </>
      }
    >
      {props.taskErrorMessage ? <div className="status-box error-box">{props.taskErrorMessage}</div> : null}
      {props.successMessage ? <div className="status-box success-box">{props.successMessage}</div> : null}

      <div className="summary-row">
        <div className="summary-card"><p className="summary-label">取得件数</p><strong>{props.tasks.length}</strong></div>
        <div className="summary-card"><p className="summary-label">表示件数</p><strong>{props.filteredTasks.length}</strong></div>
      </div>

      <section className="panel section-panel">
        <div className="filter-grid compact-filter-grid">
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

        <div className="table-card">
          {props.isLoadingTasks ? (
            <p className="empty-message">タスクを読み込み中です...</p>
          ) : props.filteredTasks.length === 0 ? (
            <p className="empty-message">条件に一致するタスクはありません。</p>
          ) : (
            <div className="table-scroll">
              <table>
                <thead>
                  <tr><th>ID</th><th>タイトル</th><th>ステータス</th><th>優先度</th><th>担当者</th><th>期限</th><th>操作</th></tr>
                </thead>
                <tbody>
                  {props.filteredTasks.map((task) => (
                    <tr key={String(task.id)}>
                      <td>{task.id}</td>
                      <td><div className="title-cell"><strong>{task.title}</strong>{task.description ? <span>{task.description}</span> : null}</div></td>
                      <td><span className="badge">{task.status ?? '-'}</span></td>
                      <td><span className="badge">{task.priority ?? '-'}</span></td>
                      <td>{task.assignedUserName ?? '-'}</td>
                      <td>{formatDate(task.dueDate)}</td>
                      <td><button className="table-action-button" onClick={() => props.onShowDetail(task.id)} type="button">詳細</button></td>
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
