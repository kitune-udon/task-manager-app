import type { Dispatch, SetStateAction } from 'react'
import type { FieldErrors } from '../lib/apiError'
import type { TaskItem, TaskPriority, TaskStatus } from '../lib/taskApi'
import { toDateInputValue } from '../utils/format'

/**
 * タスク作成・編集フォームで保持する入力値。
 */
export type TaskFormState = {
  title: string
  description: string
  status: TaskStatus
  priority: TaskPriority
  dueDate: string
  assignedUserId: string
}

/**
 * フォーム入力値に、フィールドエラーと各入力の変更ハンドラーを加えたUI向けバインディング。
 */
export type TaskFormBindings = TaskFormState & {
  fieldErrors: FieldErrors
  onTitleChange: (value: string) => void
  onDescriptionChange: (value: string) => void
  onStatusChange: (value: TaskStatus) => void
  onPriorityChange: (value: TaskPriority) => void
  onDueDateChange: (value: string) => void
  onAssignedUserIdChange: (value: string) => void
}

/**
 * 担当者selectに渡す表示名と値のペア。
 */
export type AssigneeOption = {
  label: string
  value: string
}

/**
 * 新規タスク作成時に利用するフォーム初期値。
 */
export const defaultTaskForm: TaskFormState = {
  title: '',
  description: '',
  status: 'TODO',
  priority: 'MEDIUM',
  dueDate: '',
  assignedUserId: '',
}

/**
 * 指定フィールドの入力エラーを削除する。
 *
 * <p>ユーザーが再入力したフィールドだけエラー表示を消し、他フィールドのエラーは残す。</p>
 */
function clearFieldError(setFieldErrors: Dispatch<SetStateAction<FieldErrors>>, field: string) {
  setFieldErrors((current) => {
    if (!current[field]) {
      return current
    }

    const next = { ...current }
    delete next[field]
    return next
  })
}

/**
 * フォーム状態とsetterから、TaskFormへ渡す入力値と変更ハンドラーを生成する。
 */
export function toTaskFormBindings(
  form: TaskFormState,
  fieldErrors: FieldErrors,
  setForm: Dispatch<SetStateAction<TaskFormState>>,
  setFieldErrors: Dispatch<SetStateAction<FieldErrors>>,
): TaskFormBindings {
  return {
    ...form,
    fieldErrors,
    onTitleChange: (value) => {
      setForm((current) => ({ ...current, title: value }))
      clearFieldError(setFieldErrors, 'title')
    },
    onDescriptionChange: (value) => {
      setForm((current) => ({ ...current, description: value }))
      clearFieldError(setFieldErrors, 'description')
    },
    onStatusChange: (value) => {
      setForm((current) => ({ ...current, status: value }))
      clearFieldError(setFieldErrors, 'status')
    },
    onPriorityChange: (value) => {
      setForm((current) => ({ ...current, priority: value }))
      clearFieldError(setFieldErrors, 'priority')
    },
    onDueDateChange: (value) => {
      setForm((current) => ({ ...current, dueDate: value }))
      clearFieldError(setFieldErrors, 'dueDate')
    },
    onAssignedUserIdChange: (value) => {
      setForm((current) => ({ ...current, assignedUserId: value }))
      clearFieldError(setFieldErrors, 'assignedUserId')
    },
  }
}

/**
 * APIから取得したタスクを編集フォームへ流し込める文字列ベースの状態へ変換する。
 */
export function toTaskFormState(task: TaskItem): TaskFormState {
  return {
    title: task.title ?? '',
    description: task.description ?? '',
    status: task.status ?? 'TODO',
    priority: task.priority ?? 'MEDIUM',
    dueDate: toDateInputValue(task.dueDate),
    assignedUserId:
      task.assignedUserId !== undefined && task.assignedUserId !== null ? String(task.assignedUserId) : '',
  }
}
