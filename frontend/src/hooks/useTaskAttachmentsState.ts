import { useCallback, useEffect, useState } from 'react'
import { resolveUserMessage } from '../lib/authApi'
import {
  deleteAttachment,
  downloadAttachment,
  fetchAttachments,
  uploadAttachment,
  type TaskAttachment,
} from '../lib/attachmentApi'

type Params = {
  selectedTaskId: number | string | null
  onReloadDetail: () => Promise<unknown>
  onReloadActivities: () => Promise<unknown>
  onRefreshUnreadCount: () => Promise<void>
}

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
      setAttachments([])
      setAttachmentErrorMessage('')
      return
    }

    void loadAttachments(selectedTaskId)
  }, [loadAttachments, selectedTaskId])

  const uploadSelectedFile = async (file: File | null) => {
    if (!selectedTaskId || !file) {
      return
    }

    setIsUploadingAttachment(true)
    setAttachmentErrorMessage('')

    try {
      await uploadAttachment(selectedTaskId, file)
      await Promise.all([loadAttachments(selectedTaskId), onReloadActivities(), onReloadDetail(), onRefreshUnreadCount()])
    } catch (error) {
      setAttachmentErrorMessage(resolveUserMessage(error))
    } finally {
      setIsUploadingAttachment(false)
    }
  }

  const removeAttachment = async (attachment: TaskAttachment) => {
    setActiveAttachmentId(attachment.id)
    setAttachmentErrorMessage('')

    try {
      await deleteAttachment(attachment.id)
      await Promise.all([loadAttachments(attachment.taskId), onReloadActivities(), onReloadDetail()])
    } catch (error) {
      setAttachmentErrorMessage(resolveUserMessage(error))
    } finally {
      setActiveAttachmentId(null)
    }
  }

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
