import type { FormEvent } from 'react'
import type { TaskPriority, TaskStatus } from '../lib/taskApi'
import type { AssigneeOption, TaskFormBindings } from '../hooks/taskStateShared'
import { TaskShell } from '../components/TaskShell'
import { TaskForm } from '../components/TaskForm'

/**
 * タスク作成画面に表示するフォーム状態、選択肢、メッセージ、画面操作。
 */
type Props = {
  activePath: string
  currentUserLabel: string
  unreadCount: number
  onNavigate: (path: string) => void
  onLogout: () => void
  onShowList: () => void
  onShowTeamDetail: () => void
  contextTeamId: string | null
  contextTeamName: string | null
  isLoadingTeamContext: boolean
  canCreateInTeam: boolean
  createErrorMessage: string
  successMessage: string
  isSubmitting: boolean
  form: TaskFormBindings
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  statusOptions: Array<{ label: string; value: TaskStatus }>
  priorityOptions: Array<{ label: string; value: TaskPriority }>
  assigneeOptions: AssigneeOption[]
  isLoadingAssigneeOptions: boolean
  assigneeOptionsError: string
}

/**
 * 共通タスクフォームを使って新規タスクを作成するページ。
 */
export function TaskCreatePage(props: Props) {
  const missingTeamContext = !props.contextTeamId
  const invalidTeamContext = Boolean(props.contextTeamId) && !props.isLoadingTeamContext && !props.canCreateInTeam

  return (
    <TaskShell
      title="タスク作成"
      description={props.contextTeamName ? `チーム: ${props.contextTeamName}` : 'チームを選択してタスクを登録できます。'}
      activePath={props.activePath}
      onNavigate={props.onNavigate}
      onLogout={props.onLogout}
      currentUserLabel={props.currentUserLabel}
      unreadCount={props.unreadCount}
      actions={
        <>
          <button className="secondary-button" onClick={props.onShowList} type="button">
            一覧へ戻る
          </button>
          {props.contextTeamId ? (
            <button className="secondary-button" onClick={props.onShowTeamDetail} type="button">
              チーム詳細へ戻る
            </button>
          ) : null}
        </>
      }
    >
      {props.createErrorMessage ? <div className="status-box error-box">{props.createErrorMessage}</div> : null}
      {props.successMessage ? <div className="status-box success-box">{props.successMessage}</div> : null}
      <section className="panel section-panel form-panel">
        {missingTeamContext ? (
          <div className="empty-state-panel">
            <h2>タスクを作成するチームを選択してください</h2>
            <button className="primary-button" onClick={() => props.onNavigate('/teams')} type="button">
              チーム一覧へ
            </button>
          </div>
        ) : props.isLoadingTeamContext ? (
          <p className="empty-message padded-message">チーム情報を読み込み中です...</p>
        ) : invalidTeamContext ? (
          <div className="empty-state-panel">
            <h2>指定されたチームでタスクを作成できません</h2>
            <button className="primary-button" onClick={() => props.onNavigate('/teams')} type="button">
              チーム一覧へ
            </button>
          </div>
        ) : (
          <>
            <div className="task-context-banner inline-context-banner">
              <span className="summary-label">チーム</span>
              <strong>{props.contextTeamName ?? props.contextTeamId}</strong>
            </div>
            <TaskForm
              form={props.form}
              isSubmitting={props.isSubmitting}
              onSubmit={props.onSubmit}
              onCancel={props.onShowList}
              submitLabel="タスクを作成"
              submittingLabel="作成中..."
              statusOptions={props.statusOptions}
              priorityOptions={props.priorityOptions}
              assigneeOptions={props.assigneeOptions}
              isLoadingAssigneeOptions={props.isLoadingAssigneeOptions}
              assigneeOptionsError={props.assigneeOptionsError}
            />
          </>
        )}
      </section>
    </TaskShell>
  )
}
