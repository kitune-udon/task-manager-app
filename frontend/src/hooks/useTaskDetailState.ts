import { useEffect, useState } from 'react'
import { resolveUserMessage } from '../lib/authApi'
import { fetchTaskById, type TaskItem } from '../lib/taskApi'

type Params = {
  selectedTaskId: string | null
}

export function useTaskDetailState({ selectedTaskId }: Params) {
  const [selectedTask, setSelectedTask] = useState<TaskItem | null>(null)
  const [detailErrorMessage, setDetailErrorMessage] = useState('')
  const [isLoadingDetail, setIsLoadingDetail] = useState(false)
  const [commentDraft, setCommentDraft] = useState('')

  const loadTaskDetail = async (taskId: string) => {
    setIsLoadingDetail(true)
    setDetailErrorMessage('')

    try {
      const task = await fetchTaskById(taskId)
      setSelectedTask(task)
      if (!task) {
        setDetailErrorMessage('対象タスクが見つかりません。')
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
      void loadTaskDetail(selectedTaskId)
    } else {
      setSelectedTask(null)
    }
  }, [selectedTaskId])

  const clearDetailErrorMessage = () => {
    setDetailErrorMessage('')
  }

  const clearDetailState = () => {
    setSelectedTask(null)
    setDetailErrorMessage('')
    setIsLoadingDetail(false)
    setCommentDraft('')
  }

  return {
    selectedTask,
    setSelectedTask,
    detailErrorMessage,
    setDetailErrorMessage,
    isLoadingDetail,
    commentDraft,
    setCommentDraft,
    loadTaskDetail,
    clearDetailErrorMessage,
    clearDetailState,
  }
}
