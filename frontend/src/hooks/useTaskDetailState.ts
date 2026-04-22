import { useEffect, useState } from 'react'
import { resolveUserMessage } from '../lib/authApi'
import { fetchTaskById, type TaskItem } from '../lib/taskApi'

/**
 * タスク詳細画面のアクティビティ領域で表示するタブ。
 */
export type DetailTab = 'comments' | 'history'

/**
 * 詳細表示対象のタスクID。
 */
type Params = {
  selectedTaskId: string | null
}

/**
 * 選択中タスクの詳細、編集状態、コメント入力、アクティビティタブ状態を管理する。
 */
export function useTaskDetailState({ selectedTaskId }: Params) {
  const [selectedTask, setSelectedTask] = useState<TaskItem | null>(null)
  const [detailErrorMessage, setDetailErrorMessage] = useState('')
  const [isLoadingDetail, setIsLoadingDetail] = useState(false)
  const [isEditing, setIsEditing] = useState(false)
  const [commentDraft, setCommentDraft] = useState('')
  const [activeActivityTab, setActiveActivityTab] = useState<DetailTab>('comments')

  /**
   * 指定タスクの詳細情報を取得する。
   */
  const loadTaskDetail = async (taskId: string) => {
    setIsLoadingDetail(true)
    setDetailErrorMessage('')

    try {
      const task = await fetchTaskById(taskId)
      setSelectedTask(task)
      if (!task) {
        setDetailErrorMessage('タスクが見つかりません。')
      }
      return task
    } catch (error) {
      setSelectedTask(null)
      setDetailErrorMessage(resolveUserMessage(error))
      return null
    } finally {
      setIsLoadingDetail(false)
    }
  }

  useEffect(() => {
    if (selectedTaskId) {
      // 別タスクへ移動したときは、前タスクの編集状態や入力途中コメントを引き継がない。
      setIsEditing(false)
      setCommentDraft('')
      setActiveActivityTab('comments')
      void loadTaskDetail(selectedTaskId)
    } else {
      // 詳細対象がない画面では前回の詳細情報を残さない。
      setSelectedTask(null)
      setIsEditing(false)
      setCommentDraft('')
      setActiveActivityTab('comments')
    }
  }, [selectedTaskId])

  /**
   * 詳細取得や操作で表示しているエラーメッセージをクリアする。
   */
  const clearDetailErrorMessage = () => {
    setDetailErrorMessage('')
  }

  /**
   * タスク詳細に関する状態を初期化する。
   */
  const clearDetailState = () => {
    setSelectedTask(null)
    setDetailErrorMessage('')
    setIsLoadingDetail(false)
    setIsEditing(false)
    setCommentDraft('')
    setActiveActivityTab('comments')
  }

  return {
    selectedTask,
    setSelectedTask,
    detailErrorMessage,
    setDetailErrorMessage,
    isLoadingDetail,
    isEditing,
    commentDraft,
    setCommentDraft,
    activeActivityTab,
    setActiveActivityTab,
    startEditing: () => setIsEditing(true),
    cancelEditing: () => setIsEditing(false),
    loadTaskDetail,
    clearDetailErrorMessage,
    clearDetailState,
  }
}
