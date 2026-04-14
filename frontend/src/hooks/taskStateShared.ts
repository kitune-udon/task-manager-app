import type { Dispatch, SetStateAction } from 'react'
import type { FieldErrors } from '../lib/apiError'
import type { TaskItem, TaskPriority, TaskStatus } from '../lib/taskApi'
import { toDateInputValue } from '../utils/format'

export type TaskFormState = {
  title: string
  description: string
  status: TaskStatus
  priority: TaskPriority
  dueDate: string
  assignedUserId: string
}

export type TaskFormBindings = TaskFormState & {
  fieldErrors: FieldErrors
  onTitleChange: (value: string) => void
  onDescriptionChange: (value: string) => void
  onStatusChange: (value: TaskStatus) => void
  onPriorityChange: (value: TaskPriority) => void
  onDueDateChange: (value: string) => void
  onAssignedUserIdChange: (value: string) => void
}

export type AssigneeOption = {
  label: string
  value: string
}

export const defaultTaskForm: TaskFormState = {
  title: '',
  description: '',
  status: 'TODO',
  priority: 'MEDIUM',
  dueDate: '',
  assignedUserId: '',
}

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
