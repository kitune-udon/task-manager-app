import { useEffect, useState } from 'react'
import { resolveUserMessage } from '../lib/authApi'
import { fetchTaskById, type TaskItem } from '../lib/taskApi'

export type DetailTab = 'comments' | 'history'

type Params = {
  selectedTaskId: string | null
}

export function useTaskDetailState({ selectedTaskId }: Params) {
  const [selectedTask, setSelectedTask] = useState<TaskItem | null>(null)
  const [detailErrorMessage, setDetailErrorMessage] = useState('')
  const [isLoadingDetail, setIsLoadingDetail] = useState(false)
  const [isEditing, setIsEditing] = useState(false)
  const [commentDraft, setCommentDraft] = useState('')
  const [activeActivityTab, setActiveActivityTab] = useState<DetailTab>('comments')

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
      setIsEditing(false)
      setCommentDraft('')
      setActiveActivityTab('comments')
      void loadTaskDetail(selectedTaskId)
    } else {
      setSelectedTask(null)
      setIsEditing(false)
      setCommentDraft('')
      setActiveActivityTab('comments')
    }
  }, [selectedTaskId])

  const clearDetailErrorMessage = () => {
    setDetailErrorMessage('')
  }

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
