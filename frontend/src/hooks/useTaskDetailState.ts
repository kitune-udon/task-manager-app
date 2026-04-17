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
  const [isEditing, setIsEditing] = useState(false)

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
      void loadTaskDetail(selectedTaskId)
    } else {
      setSelectedTask(null)
      setIsEditing(false)
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
  }

  return {
    selectedTask,
    setSelectedTask,
    detailErrorMessage,
    setDetailErrorMessage,
    isLoadingDetail,
    isEditing,
    startEditing: () => setIsEditing(true),
    cancelEditing: () => setIsEditing(false),
    loadTaskDetail,
    clearDetailErrorMessage,
    clearDetailState,
  }
}
