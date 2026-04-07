import type { TaskPriority, TaskStatus } from './lib/taskApi'

export const STATUS_OPTIONS = [
  { label: 'すべて', value: 'ALL' },
  { label: 'TODO', value: 'TODO' },
  { label: 'DOING', value: 'DOING' },
  { label: 'DONE', value: 'DONE' },
]

export const EDITABLE_STATUS_OPTIONS: Array<{ label: string; value: TaskStatus }> = [
  { label: 'TODO', value: 'TODO' },
  { label: 'DOING', value: 'DOING' },
  { label: 'DONE', value: 'DONE' },
]

export const PRIORITY_OPTIONS = [
  { label: 'すべて', value: 'ALL' },
  { label: 'LOW', value: 'LOW' },
  { label: 'MEDIUM', value: 'MEDIUM' },
  { label: 'HIGH', value: 'HIGH' },
]

export const EDITABLE_PRIORITY_OPTIONS: Array<{ label: string; value: TaskPriority }> = [
  { label: 'LOW', value: 'LOW' },
  { label: 'MEDIUM', value: 'MEDIUM' },
  { label: 'HIGH', value: 'HIGH' },
]
