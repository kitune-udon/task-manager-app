import type { FormEvent } from 'react'
import type { TaskPriority, TaskStatus } from '../lib/taskApi'
import type { TaskFormBindings } from '../hooks/useTaskState'

type Props = {
  form: TaskFormBindings
  isSubmitting: boolean
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  onCancel: () => void
  submitLabel: string
  submittingLabel: string
  cancelLabel?: string
  statusOptions: Array<{ label: string; value: TaskStatus }>
  priorityOptions: Array<{ label: string; value: TaskPriority }>
}

export function TaskForm({
  form,
  isSubmitting,
  onSubmit,
  onCancel,
  submitLabel,
  submittingLabel,
  cancelLabel = 'キャンセル',
  statusOptions,
  priorityOptions,
}: Props) {
  return (
    <form className="form-grid create-form" onSubmit={onSubmit}>
      <label>
        <span>タイトル</span>
        <input type="text" value={form.title} onChange={(e) => form.onTitleChange(e.target.value)} />
      </label>

      <label className="form-column-full">
        <span>説明</span>
        <textarea value={form.description} onChange={(e) => form.onDescriptionChange(e.target.value)} rows={4} />
      </label>

      <label>
        <span>ステータス</span>
        <select value={form.status} onChange={(e) => form.onStatusChange(e.target.value)}>
          {statusOptions.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </label>

      <label>
        <span>優先度</span>
        <select value={form.priority} onChange={(e) => form.onPriorityChange(e.target.value)}>
          {priorityOptions.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </label>

      <label>
        <span>期限</span>
        <input type="date" value={form.dueDate} onChange={(e) => form.onDueDateChange(e.target.value)} />
      </label>

      <label>
        <span>担当者ID（任意）</span>
        <input
          type="text"
          value={form.assignedUserId}
          onChange={(e) => form.onAssignedUserIdChange(e.target.value)}
          placeholder="例: 1"
        />
      </label>

      <div className="form-actions form-column-full split-actions">
        <button className="secondary-button" onClick={onCancel} type="button">
          {cancelLabel}
        </button>
        <button className="primary-button" disabled={isSubmitting} type="submit">
          {isSubmitting ? submittingLabel : submitLabel}
        </button>
      </div>
    </form>
  )
}
