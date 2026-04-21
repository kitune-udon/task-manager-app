import type { TaskPriority, TaskStatus } from './lib/taskApi'

/**
 * 一覧フィルタで利用するステータス選択肢。
 */
export const STATUS_OPTIONS = [
  { label: 'すべて', value: 'ALL' },
  { label: 'TODO', value: 'TODO' },
  { label: 'DOING', value: 'DOING' },
  { label: 'DONE', value: 'DONE' },
]

/**
 * 作成・編集フォームで利用するステータス選択肢。
 */
export const EDITABLE_STATUS_OPTIONS: Array<{ label: string; value: TaskStatus }> = [
  { label: 'TODO', value: 'TODO' },
  { label: 'DOING', value: 'DOING' },
  { label: 'DONE', value: 'DONE' },
]

/**
 * 一覧フィルタで利用する優先度選択肢。
 */
export const PRIORITY_OPTIONS = [
  { label: 'すべて', value: 'ALL' },
  { label: 'LOW', value: 'LOW' },
  { label: 'MEDIUM', value: 'MEDIUM' },
  { label: 'HIGH', value: 'HIGH' },
]

/**
 * 作成・編集フォームで利用する優先度選択肢。
 */
export const EDITABLE_PRIORITY_OPTIONS: Array<{ label: string; value: TaskPriority }> = [
  { label: 'LOW', value: 'LOW' },
  { label: 'MEDIUM', value: 'MEDIUM' },
  { label: 'HIGH', value: 'HIGH' },
]
