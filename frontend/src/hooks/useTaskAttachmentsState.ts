import { useCallback, useEffect, useState } from 'react'
import { resolveUserMessage } from '../lib/authApi'
import {
  deleteAttachment,
  downloadAttachment,
  fetchAttachments,
  uploadAttachment,
  type TaskAttachment,
} from '../lib/attachmentApi'

/**
 * 添付ファイル状態hookが必要とする対象タスクと、関連データの再読み込み操作。
 */
type Params = {
  selectedTaskId: number | string | null
  onReloadDetail: () => Promise<unknown>
  onReloadActivities: () => Promise<unknown>
  onRefreshUnreadCount: () => Promise<void>
}

/**
 * 選択中タスクの添付ファイル一覧、アップロード、削除、ダウンロード状態を管理する。
 */
export function useTaskAttachmentsState({
  selectedTaskId,
  onReloadDetail,
  onReloadActivities,
  onRefreshUnreadCount,
}: Params) {
  const [attachments, setAttachments] = useState<TaskAttachment[]>([])
  const [isLoadingAttachments, setIsLoadingAttachments] = useState(false)
  const [attachmentErrorMessage, setAttachmentErrorMessage] = useState('')
  const [isUploadingAttachment, setIsUploadingAttachment] = useState(false)
  const [activeAttachmentId, setActiveAttachmentId] = useState<number | null>(null)

  /**
   * 指定タスクの添付ファイル一覧を取得する。
   */
  const loadAttachments = useCallback(async (taskId = selectedTaskId) => {
    if (!taskId) {
      setAttachments([])
      setAttachmentErrorMessage('')
      return []
    }

    setIsLoadingAttachments(true)
    setAttachmentErrorMessage('')

    try {
      const nextAttachments = await fetchAttachments(taskId)
      setAttachments(nextAttachments)
      return nextAttachments
    } catch (error) {
      setAttachments([])
      setAttachmentErrorMessage(resolveUserMessage(error))
      return []
    } finally {
      setIsLoadingAttachments(false)
    }
  }, [selectedTaskId])

  useEffect(() => {
    setIsUploadingAttachment(false)
    setActiveAttachmentId(null)

    if (!selectedTaskId) {
      // タスク未選択時は前回タスクの添付一覧やエラー表示を残さない。
      setAttachments([])
      setAttachmentErrorMessage('')
      return
    }

    void loadAttachments(selectedTaskId)
  }, [loadAttachments, selectedTaskId])

  /**
   * 選択されたファイルを現在のタスクへアップロードする。
   */
  const uploadSelectedFile = async (file: File | null) => {
    if (!selectedTaskId || !file) {
      return
    }

    setIsUploadingAttachment(true)
    setAttachmentErrorMessage('')

    try {
      await uploadAttachment(selectedTaskId, file)
      // 添付追加は詳細情報・履歴・通知未読数にも影響するため、関連データをまとめて更新する。
      await Promise.all([loadAttachments(selectedTaskId), onReloadActivities(), onReloadDetail(), onRefreshUnreadCount()])
    } catch (error) {
      setAttachmentErrorMessage(resolveUserMessage(error))
    } finally {
      setIsUploadingAttachment(false)
    }
  }

  /**
   * 添付ファイルを削除し、関連データを再取得する。
   */
  const removeAttachment = async (attachment: TaskAttachment) => {
    setActiveAttachmentId(attachment.id)
    setAttachmentErrorMessage('')

    try {
      await deleteAttachment(attachment.id)
      // 削除後は添付一覧だけでなく、詳細情報と履歴も最新化する。
      await Promise.all([loadAttachments(attachment.taskId), onReloadActivities(), onReloadDetail()])
    } catch (error) {
      setAttachmentErrorMessage(resolveUserMessage(error))
    } finally {
      setActiveAttachmentId(null)
    }
  }

  /**
   * 添付ファイルをダウンロードする。
   */
  const downloadAttachmentFile = async (attachment: TaskAttachment) => {
    setActiveAttachmentId(attachment.id)
    setAttachmentErrorMessage('')

    try {
      await downloadAttachment(attachment.id, attachment.originalFileName)
    } catch (error) {
      setAttachmentErrorMessage(resolveUserMessage(error))
    } finally {
      setActiveAttachmentId(null)
    }
  }

  return {
    attachments,
    isLoadingAttachments,
    attachmentErrorMessage,
    isUploadingAttachment,
    activeAttachmentId,
    loadAttachments,
    uploadSelectedFile,
    removeAttachment,
    downloadAttachmentFile,
  }
}
