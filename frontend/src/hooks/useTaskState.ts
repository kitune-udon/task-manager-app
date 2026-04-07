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
import { extractApiErrorMessage } from '../lib/authApi'
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

function toTaskFormBindings(
  form: TaskFormState,
  setForm: Dispatch<SetStateAction<TaskFormState>>,
): TaskFormBindings {
  return {
    ...form,
    onTitleChange: (value) => setForm((current) => ({ ...current, title: value })),
    onDescriptionChange: (value) => setForm((current) => ({ ...current, description: value })),
    onStatusChange: (value) => setForm((current) => ({ ...current, status: value })),
    onPriorityChange: (value) => setForm((current) => ({ ...current, priority: value })),
    onDueDateChange: (value) => setForm((current) => ({ ...current, dueDate: value })),
    onAssignedUserIdChange: (value) => setForm((current) => ({ ...current, assignedUserId: value })),
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
  const [isSubmittingTask, setIsSubmittingTask] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)

  const activePath = route.page === 'create' ? '/tasks/new' : '/tasks'

  const validateTaskForm = (title: string, status: string, priority: string, assignedUserId: string) => {
    if (!title.trim()) return 'タイトルを入力してください。'
    if (title.trim().length > 100) return 'タイトルは100文字以内で入力してください。'
    if (!priority.trim()) return '優先度を選択してください。'
    if (!status.trim()) return 'ステータスを選択してください。'
    if (assignedUserId && !/^\d+$/.test(assignedUserId)) return '担当者IDは数値で入力してください。'
    return ''
  }

  const loadTasks = async () => {
    setIsLoadingTasks(true)
    setTaskErrorMessage('')

    try {
      const taskList = await fetchTasks()
      setTasks(taskList)
    } catch (error) {
      setTaskErrorMessage(extractApiErrorMessage(error))
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
      setDetailErrorMessage(extractApiErrorMessage(error))
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
    go(`/tasks/${selectedTaskId}/edit`)
  }

  const handleCreateTask = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    resetMessages()
    setTaskErrorMessage('')

    const validationMessage = validateTaskForm(
      createForm.title,
      String(createForm.status),
      String(createForm.priority),
      createForm.assignedUserId,
    )
    if (validationMessage) {
      setTaskErrorMessage(validationMessage)
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
      await loadTasks()
      setSelectedTask(createdTask)
      go(`/tasks/${createdTask.id}`)
      setGlobalSuccessMessage('タスクを作成しました。')
    } catch (error) {
      setTaskErrorMessage(extractApiErrorMessage(error))
    } finally {
      setIsSubmittingTask(false)
    }
  }

  const handleEditTask = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    resetMessages()
    setDetailErrorMessage('')

    if (!selectedTaskId) {
      setDetailErrorMessage('更新対象のタスクが選択されていません。')
      return
    }

    const validationMessage = validateTaskForm(
      editForm.title,
      String(editForm.status),
      String(editForm.priority),
      editForm.assignedUserId,
    )
    if (validationMessage) {
      setDetailErrorMessage(validationMessage)
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
      await loadTasks()
      go(`/tasks/${selectedTaskId}`)
      setGlobalSuccessMessage('タスクを更新しました。')
    } catch (error) {
      setDetailErrorMessage(extractApiErrorMessage(error))
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
      setDetailErrorMessage(extractApiErrorMessage(error))
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

  const createTaskForm = useMemo(() => toTaskFormBindings(createForm, setCreateForm), [createForm])
  const editTaskForm = useMemo(() => toTaskFormBindings(editForm, setEditForm), [editForm])

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
