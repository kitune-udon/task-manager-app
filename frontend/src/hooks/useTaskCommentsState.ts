import { useCallback, useEffect, useState } from 'react'
import { resolveUserMessage } from '../lib/authApi'
import { extractApiErrorCode } from '../lib/apiError'
import { createComment, deleteComment, fetchComments, updateComment, type TaskComment } from '../lib/commentApi'

/**
 * コメント状態hookが必要とする対象タスク、入力中コメント、関連データの再読み込み操作。
 */
type Params = {
  selectedTaskId: number | string | null
  commentDraft: string
  setCommentDraft: (value: string) => void
  onReloadDetail: () => Promise<unknown>
  onReloadActivities: () => Promise<unknown>
  onRefreshUnreadCount: () => Promise<void>
}

/**
 * 選択中タスクのコメント一覧、投稿、編集、削除状態を管理する。
 */
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

  /**
   * 指定タスクのコメント一覧を取得する。
   */
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
      // タスク未選択時は前回タスクのコメント一覧や編集中状態を残さない。
      setComments([])
      setCommentErrorMessage('')
      return
    }

    void loadComments(selectedTaskId)
  }, [loadComments, selectedTaskId])

  /**
   * 指定コメントを編集モードにする。
   */
  const startEditingComment = (comment: TaskComment) => {
    setEditingCommentId(comment.id)
    setEditingCommentContent(comment.content)
    setCommentErrorMessage('')
  }

  /**
   * コメント編集モードを解除する。
   */
  const cancelEditingComment = () => {
    setEditingCommentId(null)
    setEditingCommentContent('')
  }

  /**
   * 入力中のコメントを現在のタスクへ投稿する。
   */
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
      // コメント投稿は履歴・詳細・通知未読数にも影響するため、関連データをまとめて更新する。
      await Promise.all([loadComments(selectedTaskId), onReloadActivities(), onReloadDetail(), onRefreshUnreadCount()])
    } catch (error) {
      setCommentErrorMessage(resolveUserMessage(error))
    } finally {
      setIsSubmittingComment(false)
    }
  }

  /**
   * 編集中の内容でコメントを保存する。
   */
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
      // コメント本文の変更を一覧・履歴・詳細へ反映する。
      await Promise.all([loadComments(comment.taskId), onReloadActivities(), onReloadDetail()])
    } catch (error) {
      if (extractApiErrorCode(error) === 'ERR-COMMENT-006') {
        // 楽観ロック競合時は最新状態へ戻し、ユーザーに再編集を促す。
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

  /**
   * コメントを削除する。
   */
  const removeComment = async (comment: TaskComment) => {
    setActiveCommentId(comment.id)
    setCommentErrorMessage('')

    try {
      await deleteComment(comment.id)
      if (editingCommentId === comment.id) {
        cancelEditingComment()
      }
      // 削除後はコメント一覧だけでなく、履歴と詳細も最新化する。
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
