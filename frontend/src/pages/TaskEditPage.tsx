import type { FormEvent } from 'react'
import type { TaskPriority, TaskStatus } from '../lib/taskApi'
import type { TaskFormBindings } from '../hooks/useTaskState'
import { TaskShell } from '../components/TaskShell'
import { TaskForm } from '../components/TaskForm'

type Props = {
  activePath: string
  currentUserLabel: string
  onNavigate: (path: string) => void
  onLogout: () => void
  onBackDetail: () => void
  detailErrorMessage: string
  successMessage: string
  isSubmitting: boolean
  isLoadingDetail: boolean
  form: TaskFormBindings
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  statusOptions: Array<{ label: string; value: TaskStatus }>
  priorityOptions: Array<{ label: string; value: TaskPriority }>
}

export function TaskEditPage(props: Props) {
  return (
    <TaskShell
      title="タスク編集"
      description="既存データをフォームに反映し、PUT /api/tasks/{taskId} で更新します。"
      activePath={props.activePath}
      onNavigate={props.onNavigate}
      onLogout={props.onLogout}
      currentUserLabel={props.currentUserLabel}
      actions={
        <button className="secondary-button" onClick={props.onBackDetail} type="button">
          詳細へ戻る
        </button>
      }
    >
      {props.detailErrorMessage ? <div className="status-box error-box">{props.detailErrorMessage}</div> : null}
      {props.successMessage ? <div className="status-box success-box">{props.successMessage}</div> : null}
      <section className="panel section-panel form-panel">
        {props.isLoadingDetail ? (
          <p className="empty-message">タスクを読み込み中です...</p>
        ) : (
          <TaskForm
            form={props.form}
            isSubmitting={props.isSubmitting}
            onSubmit={props.onSubmit}
            onCancel={props.onBackDetail}
            submitLabel="更新する"
            submittingLabel="更新中..."
            statusOptions={props.statusOptions}
            priorityOptions={props.priorityOptions}
          />
        )}
      </section>
    </TaskShell>
  )
}
