import { useEffect, useMemo, useState, type FormEvent } from 'react'
import type { ResolvedRoute } from '../app_navigation'
import { extractFieldErrorsFromApiError, hasFieldErrors, resolveUserMessage } from '../lib/authApi'
import type { FieldErrors } from '../lib/apiError'
import { createTask, deleteTask, updateTask, type TaskItem } from '../lib/taskApi'
import {
  defaultTaskForm,
  toTaskFormBindings,
  toTaskFormState,
  type AssigneeOption,
  type TaskFormState,
} from './taskStateShared'

type Params = {
  routePage: ResolvedRoute['page']
  selectedTaskId: string | null
  selectedTask: TaskItem | null
  assigneeOptions: AssigneeOption[]
  loadTasks: () => Promise<void>
  setSelectedTask: (value: TaskItem | null) => void
  setDetailErrorMessage: (value: string) => void
  resetMessages: () => void
  setGlobalSuccessMessage: (value: string) => void
  go: (path: string, replace?: boolean) => void
}

function toTaskRequestPayload(form: TaskFormState) {
  return {
    title: form.title.trim(),
    description: form.description.trim() || undefined,
    status: form.status,
    priority: form.priority,
    dueDate: form.dueDate || undefined,
    assignedUserId: form.assignedUserId ? Number(form.assignedUserId) : undefined,
    teamId: undefined,
  }
}

export function useTaskMutationState({
  routePage,
  selectedTaskId,
  selectedTask,
  assigneeOptions,
  loadTasks,
  setSelectedTask,
  setDetailErrorMessage,
  resetMessages,
  setGlobalSuccessMessage,
  go,
}: Params) {
  const [createForm, setCreateForm] = useState<TaskFormState>(defaultTaskForm)
  const [editForm, setEditForm] = useState<TaskFormState>(defaultTaskForm)
  const [createFieldErrors, setCreateFieldErrors] = useState<FieldErrors>({})
  const [editFieldErrors, setEditFieldErrors] = useState<FieldErrors>({})
  const [createErrorMessage, setCreateErrorMessage] = useState('')
  const [isSubmittingTask, setIsSubmittingTask] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)

  const validateTaskForm = (title: string, status: string, priority: string, assignedUserId: string): FieldErrors => {
    const next: FieldErrors = {}

    if (!title.trim()) next.title = 'タイトルを入力してください。'
    else if (title.trim().length > 100) next.title = 'タイトルは100文字以内で入力してください。'
    if (!priority.trim()) next.priority = '優先度を選択してください。'
    if (!status.trim()) next.status = 'ステータスを選択してください。'
    if (assignedUserId && !assigneeOptions.some((option) => option.value === assignedUserId)) {
      next.assignedUserId = '担当者を選択してください。'
    }

    return next
  }

  const handleCreateTask = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    resetMessages()
    setCreateErrorMessage('')
    setCreateFieldErrors({})

    const validationErrors = validateTaskForm(
      createForm.title,
      String(createForm.status),
      String(createForm.priority),
      createForm.assignedUserId,
    )
    if (hasFieldErrors(validationErrors)) {
      setCreateFieldErrors(validationErrors)
      setCreateErrorMessage('入力内容を確認してください。')
      return
    }

    setIsSubmittingTask(true)
    try {
      await createTask(toTaskRequestPayload(createForm))
      setCreateForm(defaultTaskForm)
      setCreateFieldErrors({})
      await loadTasks()
      setSelectedTask(null)
      go('/tasks')
      setGlobalSuccessMessage('タスクを作成しました。')
    } catch (error) {
      const apiFieldErrors = extractFieldErrorsFromApiError(error)
      if (hasFieldErrors(apiFieldErrors)) {
        setCreateFieldErrors(apiFieldErrors)
      }
      setCreateErrorMessage(resolveUserMessage(error))
    } finally {
      setIsSubmittingTask(false)
    }
  }

  const handleEditTask = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    resetMessages()
    setDetailErrorMessage('')
    setEditFieldErrors({})

    if (!selectedTaskId) {
      setDetailErrorMessage('更新対象のタスクが選択されていません。')
      return
    }

    const validationErrors = validateTaskForm(
      editForm.title,
      String(editForm.status),
      String(editForm.priority),
      editForm.assignedUserId,
    )
    if (hasFieldErrors(validationErrors)) {
      setEditFieldErrors(validationErrors)
      setDetailErrorMessage('入力内容を確認してください。')
      return
    }

    setIsSubmittingTask(true)
    try {
      const updatedTask = await updateTask(selectedTaskId, toTaskRequestPayload(editForm))
      setSelectedTask(updatedTask)
      setEditFieldErrors({})
      await loadTasks()
      go(`/tasks/${selectedTaskId}`)
      setGlobalSuccessMessage('タスクを更新しました。')
    } catch (error) {
      const apiFieldErrors = extractFieldErrorsFromApiError(error)
      if (hasFieldErrors(apiFieldErrors)) {
        setEditFieldErrors(apiFieldErrors)
      }
      setDetailErrorMessage(resolveUserMessage(error))
    } finally {
      setIsSubmittingTask(false)
    }
  }

  const handleDeleteTask = async () => {
    if (!selectedTaskId) return
    if (!window.confirm('このタスクを削除しますか？')) return

    resetMessages()
    setDetailErrorMessage('')
    setIsDeleting(true)
    try {
      await deleteTask(selectedTaskId)
      setSelectedTask(null)
      await loadTasks()
      go('/tasks')
      setGlobalSuccessMessage('タスクを削除しました。')
    } catch (error) {
      setDetailErrorMessage(resolveUserMessage(error))
    } finally {
      setIsDeleting(false)
    }
  }

  useEffect(() => {
    if (routePage === 'edit' && selectedTask) {
      setEditFieldErrors({})
      setEditForm(toTaskFormState(selectedTask))
    }
  }, [routePage, selectedTask])

  const createTaskForm = useMemo(
    () => toTaskFormBindings(createForm, createFieldErrors, setCreateForm, setCreateFieldErrors),
    [createFieldErrors, createForm],
  )
  const editTaskForm = useMemo(
    () => toTaskFormBindings(editForm, editFieldErrors, setEditForm, setEditFieldErrors),
    [editFieldErrors, editForm],
  )

  const resetCreateState = () => {
    setCreateErrorMessage('')
    setCreateFieldErrors({})
    setCreateForm(defaultTaskForm)
  }

  const clearEditErrors = () => {
    setEditFieldErrors({})
  }

  const clearMutationState = () => {
    setCreateErrorMessage('')
    setCreateForm(defaultTaskForm)
    setEditForm(defaultTaskForm)
    setCreateFieldErrors({})
    setEditFieldErrors({})
    setIsSubmittingTask(false)
    setIsDeleting(false)
  }

  return {
    createTaskForm,
    editTaskForm,
    createErrorMessage,
    isSubmittingTask,
    isDeleting,
    handleCreateTask,
    handleEditTask,
    handleDeleteTask,
    resetCreateState,
    clearEditErrors,
    clearMutationState,
  }
}
