import { useEffect, useMemo, useState, type FormEvent, type Dispatch, type SetStateAction } from 'react'
import { toDateInputValue } from '../utils/format'
import {
  createTask,
  deleteTask,
  fetchTaskById,
  fetchTasks,
  updateTask,
  type TaskItem,
  type TaskPriority,
  type TaskStatus,
} from '../lib/taskApi'
import { extractFieldErrorsFromApiError, hasFieldErrors, resolveUserMessage } from '../lib/authApi'
import type { FieldErrors } from '../lib/apiError'
import type { ResolvedRoute } from '../app_navigation'

export type TaskFormState = {
  title: string
  description: string
  status: TaskStatus
  priority: TaskPriority
  dueDate: string
  assignedUserId: string
}

export type TaskFormBindings = TaskFormState & {
  fieldErrors: FieldErrors
  onTitleChange: (value: string) => void
  onDescriptionChange: (value: string) => void
  onStatusChange: (value: TaskStatus) => void
  onPriorityChange: (value: TaskPriority) => void
  onDueDateChange: (value: string) => void
  onAssignedUserIdChange: (value: string) => void
}

const defaultTaskForm: TaskFormState = {
  title: '',
  description: '',
  status: 'TODO',
  priority: 'MEDIUM',
  dueDate: '',
  assignedUserId: '',
}

type Params = {
  isLoggedIn: boolean
  route: ResolvedRoute
  selectedTaskId: string | null
  go: (path: string, replace?: boolean) => void
  resetMessages: () => void
  setGlobalSuccessMessage: (value: string) => void
}

function clearFieldError(setFieldErrors: Dispatch<SetStateAction<FieldErrors>>, field: string) {
  setFieldErrors((current) => {
    if (!current[field]) {
      return current
    }

    const next = { ...current }
    delete next[field]
    return next
  })
}

function toTaskFormBindings(
  form: TaskFormState,
  fieldErrors: FieldErrors,
  setForm: Dispatch<SetStateAction<TaskFormState>>,
  setFieldErrors: Dispatch<SetStateAction<FieldErrors>>,
): TaskFormBindings {
  return {
    ...form,
    fieldErrors,
    onTitleChange: (value) => {
      setForm((current) => ({ ...current, title: value }))
      clearFieldError(setFieldErrors, 'title')
    },
    onDescriptionChange: (value) => {
      setForm((current) => ({ ...current, description: value }))
      clearFieldError(setFieldErrors, 'description')
    },
    onStatusChange: (value) => {
      setForm((current) => ({ ...current, status: value }))
      clearFieldError(setFieldErrors, 'status')
    },
    onPriorityChange: (value) => {
      setForm((current) => ({ ...current, priority: value }))
      clearFieldError(setFieldErrors, 'priority')
    },
    onDueDateChange: (value) => {
      setForm((current) => ({ ...current, dueDate: value }))
      clearFieldError(setFieldErrors, 'dueDate')
    },
    onAssignedUserIdChange: (value) => {
      setForm((current) => ({ ...current, assignedUserId: value }))
      clearFieldError(setFieldErrors, 'assignedUserId')
    },
  }
}

export function useTaskState({
  isLoggedIn,
  route,
  selectedTaskId,
  go,
  resetMessages,
  setGlobalSuccessMessage,
}: Params) {
  const [tasks, setTasks] = useState<TaskItem[]>([])
  const [taskErrorMessage, setTaskErrorMessage] = useState('')
  const [isLoadingTasks, setIsLoadingTasks] = useState(false)

  const [selectedTask, setSelectedTask] = useState<TaskItem | null>(null)
  const [detailErrorMessage, setDetailErrorMessage] = useState('')
  const [isLoadingDetail, setIsLoadingDetail] = useState(false)

  const [statusFilter, setStatusFilter] = useState('ALL')
  const [priorityFilter, setPriorityFilter] = useState('ALL')
  const [commentDraft, setCommentDraft] = useState('')

  const [createForm, setCreateForm] = useState<TaskFormState>(defaultTaskForm)
  const [editForm, setEditForm] = useState<TaskFormState>(defaultTaskForm)
  const [createFieldErrors, setCreateFieldErrors] = useState<FieldErrors>({})
  const [editFieldErrors, setEditFieldErrors] = useState<FieldErrors>({})
  const [isSubmittingTask, setIsSubmittingTask] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)

  const activePath = route.page === 'create' ? '/tasks/new' : '/tasks'

  const validateTaskForm = (title: string, status: string, priority: string, assignedUserId: string): FieldErrors => {
    const next: FieldErrors = {}

    if (!title.trim()) next.title = 'タイトルを入力してください。'
    else if (title.trim().length > 100) next.title = 'タイトルは100文字以内で入力してください。'
    if (!priority.trim()) next.priority = '優先度を選択してください。'
    if (!status.trim()) next.status = 'ステータスを選択してください。'
    if (assignedUserId && !/^\d+$/.test(assignedUserId)) next.assignedUserId = '担当者IDは数値で入力してください。'

    return next
  }

  const loadTasks = async () => {
    setIsLoadingTasks(true)
    setTaskErrorMessage('')

    try {
      const taskList = await fetchTasks()
      setTasks(taskList)
    } catch (error) {
      setTaskErrorMessage(resolveUserMessage(error))
    } finally {
      setIsLoadingTasks(false)
    }
  }

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

  const resetTaskViewMessages = () => {
    setTaskErrorMessage('')
    setDetailErrorMessage('')
  }

  const handleShowList = async () => {
    resetMessages()
    resetTaskViewMessages()
    go('/tasks')
    await loadTasks()
  }

  const handleShowCreate = () => {
    resetMessages()
    setTaskErrorMessage('')
    setCreateFieldErrors({})
    setCreateForm(defaultTaskForm)
    go('/tasks/new')
  }

  const handleShowDetail = (taskId: number | string) => {
    resetMessages()
    resetTaskViewMessages()
    go(`/tasks/${taskId}`)
  }

  const handleShowEdit = () => {
    if (!selectedTaskId) return
    resetMessages()
    setDetailErrorMessage('')
    setEditFieldErrors({})
    go(`/tasks/${selectedTaskId}/edit`)
  }

  const handleCreateTask = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    resetMessages()
    setTaskErrorMessage('')
    setCreateFieldErrors({})

    const validationErrors = validateTaskForm(
      createForm.title,
      String(createForm.status),
      String(createForm.priority),
      createForm.assignedUserId,
    )
    if (hasFieldErrors(validationErrors)) {
      setCreateFieldErrors(validationErrors)
      setTaskErrorMessage('入力内容を確認してください。')
      return
    }

    setIsSubmittingTask(true)
    try {
      const createdTask = await createTask({
        title: createForm.title.trim(),
        description: createForm.description.trim() || undefined,
        status: createForm.status,
        priority: createForm.priority,
        dueDate: createForm.dueDate || undefined,
        assignedUserId: createForm.assignedUserId ? Number(createForm.assignedUserId) : undefined,
        teamId: undefined,
      })
      setCreateForm(defaultTaskForm)
      setCreateFieldErrors({})
      await loadTasks()
      setSelectedTask(createdTask)
      go(`/tasks/${createdTask.id}`)
      setGlobalSuccessMessage('タスクを作成しました。')
    } catch (error) {
      const apiFieldErrors = extractFieldErrorsFromApiError(error)
      if (hasFieldErrors(apiFieldErrors)) {
        setCreateFieldErrors(apiFieldErrors)
      }
      setTaskErrorMessage(resolveUserMessage(error))
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
      const updatedTask = await updateTask(selectedTaskId, {
        title: editForm.title.trim(),
        description: editForm.description.trim() || undefined,
        status: editForm.status,
        priority: editForm.priority,
        dueDate: editForm.dueDate || undefined,
        assignedUserId: editForm.assignedUserId ? Number(editForm.assignedUserId) : undefined,
        teamId: undefined,
      })
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

  const filteredTasks = useMemo(
    () =>
      tasks.filter((task) => {
        const matchesStatus = statusFilter === 'ALL' || (task.status ?? '-') === statusFilter
        const matchesPriority = priorityFilter === 'ALL' || (task.priority ?? '-') === priorityFilter
        return matchesStatus && matchesPriority
      }),
    [priorityFilter, statusFilter, tasks],
  )

  const createTaskForm = useMemo(
    () => toTaskFormBindings(createForm, createFieldErrors, setCreateForm, setCreateFieldErrors),
    [createFieldErrors, createForm],
  )
  const editTaskForm = useMemo(
    () => toTaskFormBindings(editForm, editFieldErrors, setEditForm, setEditFieldErrors),
    [editFieldErrors, editForm],
  )

  useEffect(() => {
    if (isLoggedIn) {
      void loadTasks()
    }
  }, [isLoggedIn])

  useEffect(() => {
    if (selectedTaskId) {
      void loadTaskDetail(selectedTaskId)
    } else {
      setSelectedTask(null)
    }
  }, [selectedTaskId])

  useEffect(() => {
    if (route.page === 'edit' && selectedTask) {
      setEditFieldErrors({})
      setEditForm({
        title: selectedTask.title ?? '',
        description: selectedTask.description ?? '',
        status: selectedTask.status ?? 'TODO',
        priority: selectedTask.priority ?? 'MEDIUM',
        dueDate: toDateInputValue(selectedTask.dueDate),
        assignedUserId:
          selectedTask.assignedUserId !== undefined && selectedTask.assignedUserId !== null
            ? String(selectedTask.assignedUserId)
            : '',
      })
    }
  }, [route.page, selectedTask])

  const clearTaskStateOnLogout = () => {
    setSelectedTask(null)
    setTasks([])
    setTaskErrorMessage('')
    setDetailErrorMessage('')
    setCommentDraft('')
    setCreateForm(defaultTaskForm)
    setEditForm(defaultTaskForm)
    setCreateFieldErrors({})
    setEditFieldErrors({})
  }

  return {
    activePath,
    tasks,
    filteredTasks,
    selectedTask,
    taskErrorMessage,
    detailErrorMessage,
    isLoadingTasks,
    isLoadingDetail,
    isSubmittingTask,
    isDeleting,
    statusFilter,
    priorityFilter,
    commentDraft,
    createTaskForm,
    editTaskForm,
    setStatusFilter,
    setPriorityFilter,
    setCommentDraft,
    actions: {
      loadTasks,
      handleShowList,
      handleShowCreate,
      handleShowDetail,
      handleShowEdit,
      handleCreateTask,
      handleEditTask,
      handleDeleteTask,
      clearTaskStateOnLogout,
    },
  }
}
