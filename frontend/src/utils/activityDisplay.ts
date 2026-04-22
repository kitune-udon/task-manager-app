import { formatDate } from './format'

// バックエンドのイベント種別を、履歴・通知画面で表示する短い日本語ラベルへ変換する。
const EVENT_TYPE_LABELS: Record<string, string> = {
  TASK_CREATED: 'タスク追加',
  TASK_UPDATED: 'タスク更新',
  TASK_DELETED: 'タスク削除',
  COMMENT_CREATED: 'コメント追加',
  COMMENT_UPDATED: 'コメント更新',
  COMMENT_DELETED: 'コメント削除',
  ATTACHMENT_UPLOADED: '添付追加',
  ATTACHMENT_DELETED: '添付削除',
}

// タスク更新履歴の変更フィールド名を、ユーザーに見せる項目名へ変換する。
const FIELD_LABELS: Record<string, string> = {
  title: 'タイトル',
  description: '説明',
  status: 'ステータス',
  priority: '優先度',
  dueDate: '期限',
  assignedUserId: '担当者',
}

// タスクステータスの内部値を、履歴差分で読みやすい表示名へ変換する。
const STATUS_LABELS: Record<string, string> = {
  TODO: '未着手',
  DOING: '進行中',
  DONE: '完了',
}

// タスク優先度の内部値を、履歴差分で読みやすい表示名へ変換する。
const PRIORITY_LABELS: Record<string, string> = {
  LOW: '低',
  MEDIUM: '中',
  HIGH: '高',
}

/**
 * タスク更新履歴で表示する1項目分の変更差分。
 */
export type ActivityDisplayChange = {
  field: string
  fieldLabel: string
  oldValueLabel: string
  newValueLabel: string
}

type DetailRecord = Record<string, unknown>

/**
 * activity detailJsonを安全に参照できるオブジェクトか判定する。
 */
function isDetailRecord(value: unknown): value is DetailRecord {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

/**
 * 未設定として表示すべき空値を表示ラベルへ変換する。
 */
function normalizeEmptyValue(value: unknown) {
  if (value === null || value === undefined) {
    return '未設定'
  }

  if (typeof value === 'string' && value.trim() === '') {
    return '未設定'
  }

  return null
}

/**
 * アクティビティイベント種別を画面表示用ラベルへ変換する。
 */
export function formatActivityEventTypeLabel(eventType: string) {
  return EVENT_TYPE_LABELS[eventType] ?? eventType
}

/**
 * detailJson内のフィールド名を画面表示用ラベルへ変換する。
 */
export function formatFieldLabel(field: string) {
  return FIELD_LABELS[field] ?? field
}

/**
 * アクティビティ詳細値をフィールド種別に応じた表示文字列へ変換する。
 */
export function formatActivityValue(field: string, value: unknown) {
  const emptyValue = normalizeEmptyValue(value)
  if (emptyValue) {
    return emptyValue
  }

  if (field === 'status' && typeof value === 'string') {
    return STATUS_LABELS[value] ?? value
  }

  if (field === 'priority' && typeof value === 'string') {
    return PRIORITY_LABELS[value] ?? value
  }

  if (field === 'dueDate') {
    return typeof value === 'string' ? formatDate(value) : String(value)
  }

  if (typeof value === 'string') {
    return value
  }

  if (typeof value === 'number' || typeof value === 'boolean') {
    return String(value)
  }

  return JSON.stringify(value)
}

/**
 * タスク更新履歴のdetailJsonから、表示可能な変更差分を抽出する。
 */
export function extractTaskUpdateChanges(detailJson: unknown): ActivityDisplayChange[] {
  if (!isDetailRecord(detailJson) || !Array.isArray(detailJson.changes)) {
    return []
  }

  return detailJson.changes
    .filter(isDetailRecord)
    .map((change) => {
      const field = typeof change.field === 'string' ? change.field : ''
      if (!field) {
        return null
      }

      return {
        field,
        fieldLabel: formatFieldLabel(field),
        oldValueLabel: formatActivityValue(field, change.oldValue),
        newValueLabel: formatActivityValue(field, change.newValue),
      }
    })
    .filter((change): change is ActivityDisplayChange => change !== null)
}

/**
 * 添付関連履歴のdetailJsonから対象ファイル名を抽出する。
 */
export function extractAttachmentFileName(detailJson: unknown) {
  if (!isDetailRecord(detailJson) || typeof detailJson.fileName !== 'string') {
    return null
  }

  const fileName = detailJson.fileName.trim()
  return fileName ? fileName : null
}

/**
 * 通知一覧に表示する補足行をイベント種別ごとに生成する。
 */
export function buildNotificationDetailLines(eventType: string, detailJson: unknown, maxTaskUpdateItems = 2) {
  if (eventType === 'TASK_UPDATED') {
    const changes = extractTaskUpdateChanges(detailJson)
    if (changes.length === 0) {
      return []
    }

    const detailLines = changes
      .slice(0, maxTaskUpdateItems)
      .map((change) => `${change.fieldLabel}: ${change.oldValueLabel} → ${change.newValueLabel}`)

    const remainingCount = changes.length - maxTaskUpdateItems
    if (remainingCount > 0) {
      // 通知行が長くなりすぎないよう、表示上限を超えた変更は件数だけ示す。
      detailLines.push(`他${remainingCount}件`)
    }

    return detailLines
  }

  if (eventType === 'ATTACHMENT_UPLOADED' || eventType === 'ATTACHMENT_DELETED') {
    const fileName = extractAttachmentFileName(detailJson)
    return fileName ? [`対象ファイル: ${fileName}`] : []
  }

  return []
}
