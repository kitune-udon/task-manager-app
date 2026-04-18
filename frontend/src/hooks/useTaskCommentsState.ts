import { useCallback, useEffect, useState } from 'react'
import { resolveUserMessage } from '../lib/authApi'
import { extractApiErrorCode } from '../lib/apiError'
import { createComment, deleteComment, fetchComments, updateComment, type TaskComment } from '../lib/commentApi'

type Params = {
  selectedTaskId: number | string | null
  commentDraft: string
  setCommentDraft: (value: string) => void
  onReloadDetail: () => Promise<unknown>
  onReloadActivities: () => Promise<unknown>
  onRefreshUnreadCount: () => Promise<void>
}

export function useTaskCommentsState({
  selectedTaskId,
  commentDraft,
  setCommentDraft,
  onReloadDetail,
  onReloadActivities,
  onRefreshUnreadCount,
}: Params) {
  const [comments, setComments] = useState<TaskComment[]>([])
  const [isLoadingComments, setIsLoadingComments] = useState(false)
  const [commentErrorMessage, setCommentErrorMessage] = useState('')
  const [editingCommentId, setEditingCommentId] = useState<number | null>(null)
  const [editingCommentContent, setEditingCommentContent] = useState('')
  const [isSubmittingComment, setIsSubmittingComment] = useState(false)
  const [activeCommentId, setActiveCommentId] = useState<number | null>(null)

  const loadComments = useCallback(async (taskId = selectedTaskId) => {
    if (!taskId) {
      setComments([])
      setCommentErrorMessage('')
      return []
    }

    setIsLoadingComments(true)
    setCommentErrorMessage('')

    try {
      const nextComments = await fetchComments(taskId)
      setComments(nextComments)
      return nextComments
    } catch (error) {
      setComments([])
      setCommentErrorMessage(resolveUserMessage(error))
      return []
    } finally {
      setIsLoadingComments(false)
    }
  }, [selectedTaskId])

  useEffect(() => {
    setIsSubmittingComment(false)
    setActiveCommentId(null)
    setEditingCommentId(null)
    setEditingCommentContent('')

    if (!selectedTaskId) {
      setComments([])
      setCommentErrorMessage('')
      return
    }

    void loadComments(selectedTaskId)
  }, [loadComments, selectedTaskId])

  const startEditingComment = (comment: TaskComment) => {
    setEditingCommentId(comment.id)
    setEditingCommentContent(comment.content)
    setCommentErrorMessage('')
  }

  const cancelEditingComment = () => {
    setEditingCommentId(null)
    setEditingCommentContent('')
  }

  const submitComment = async () => {
    if (!selectedTaskId) {
      return
    }

    const content = commentDraft.trim()
    if (!content) {
      setCommentErrorMessage('コメント内容を入力してください。')
      return
    }

    setIsSubmittingComment(true)
    setCommentErrorMessage('')

    try {
      await createComment(selectedTaskId, { content })
      setCommentDraft('')
      await Promise.all([loadComments(selectedTaskId), onReloadActivities(), onReloadDetail(), onRefreshUnreadCount()])
    } catch (error) {
      setCommentErrorMessage(resolveUserMessage(error))
    } finally {
      setIsSubmittingComment(false)
    }
  }

  const saveEditedComment = async (comment: TaskComment) => {
    const content = editingCommentContent.trim()
    if (!content) {
      setCommentErrorMessage('コメント内容を入力してください。')
      return
    }

    setActiveCommentId(comment.id)
    setCommentErrorMessage('')

    try {
      await updateComment(comment.id, {
        content,
        version: Number(comment.version ?? 0),
      })
      cancelEditingComment()
      await Promise.all([loadComments(comment.taskId), onReloadActivities(), onReloadDetail()])
    } catch (error) {
      if (extractApiErrorCode(error) === 'ERR-COMMENT-006') {
        cancelEditingComment()
        await Promise.all([loadComments(comment.taskId), onReloadActivities()])
        setCommentErrorMessage('他のユーザーによりコメントが更新されました。最新状態を再読み込みしました。内容を確認して再編集してください。')
        return
      }

      setCommentErrorMessage(resolveUserMessage(error))
    } finally {
      setActiveCommentId(null)
    }
  }

  const removeComment = async (comment: TaskComment) => {
    setActiveCommentId(comment.id)
    setCommentErrorMessage('')

    try {
      await deleteComment(comment.id)
      if (editingCommentId === comment.id) {
        cancelEditingComment()
      }
      await Promise.all([loadComments(comment.taskId), onReloadActivities(), onReloadDetail()])
    } catch (error) {
      setCommentErrorMessage(resolveUserMessage(error))
    } finally {
      setActiveCommentId(null)
    }
  }

  return {
    comments,
    isLoadingComments,
    commentErrorMessage,
    editingCommentId,
    editingCommentContent,
    isSubmittingComment,
    activeCommentId,
    setEditingCommentContent,
    loadComments,
    submitComment,
    startEditingComment,
    cancelEditingComment,
    saveEditedComment,
    removeComment,
  }
}
