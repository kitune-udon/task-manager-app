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
  return (
    <TaskShell
      title="タスク作成"
      description="新しいタスクを登録できます。"
      activePath={props.activePath}
      onNavigate={props.onNavigate}
      onLogout={props.onLogout}
      currentUserLabel={props.currentUserLabel}
      unreadCount={props.unreadCount}
      actions={
        <button className="secondary-button" onClick={props.onShowList} type="button">
          一覧へ戻る
        </button>
      }
    >
      {props.createErrorMessage ? <div className="status-box error-box">{props.createErrorMessage}</div> : null}
      {props.successMessage ? <div className="status-box success-box">{props.successMessage}</div> : null}
      <section className="panel section-panel form-panel">
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
      </section>
    </TaskShell>
  )
}
