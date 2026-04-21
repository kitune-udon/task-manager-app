import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { extractFieldErrorsFromApiError, hasFieldErrors, resolveUserMessage } from '../lib/authApi'
import { extractApiErrorCode, type FieldErrors } from '../lib/apiError'
import { createTask, deleteTask, updateTask, type TaskItem } from '../lib/taskApi'
import {
  defaultTaskForm,
  toTaskFormBindings,
  toTaskFormState,
  type AssigneeOption,
  type TaskFormState,
} from './taskStateShared'

/**
 * タスク作成・更新・削除hookが必要とする選択中タスク、関連状態更新、画面遷移操作。
 */
type Params = {
  selectedTaskId: string | null
  selectedTask: TaskItem | null
  assigneeOptions: AssigneeOption[]
  loadTasks: () => Promise<void>
  reloadSelectedTask: (taskId: string) => Promise<TaskItem | null>
  setSelectedTask: (value: TaskItem | null) => void
  setDetailErrorMessage: (value: string) => void
  resetMessages: () => void
  setGlobalSuccessMessage: (value: string) => void
  stopEditing: () => void
  refreshUnreadCount: () => Promise<void>
  go: (path: string, replace?: boolean) => void
}

/**
 * フォーム状態をAPIへ送るタスク作成・更新リクエスト形式へ変換する。
 */
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

/**
 * タスクの作成フォーム、編集フォーム、削除処理と送信状態を管理する。
 */
export function useTaskMutationState({
  selectedTaskId,
  selectedTask,
  assigneeOptions,
  loadTasks,
  reloadSelectedTask,
  setSelectedTask,
  setDetailErrorMessage,
  resetMessages,
  setGlobalSuccessMessage,
  stopEditing,
  refreshUnreadCount,
  go,
}: Params) {
  const [createForm, setCreateForm] = useState<TaskFormState>(defaultTaskForm)
  const [editForm, setEditForm] = useState<TaskFormState>(defaultTaskForm)
  const [createFieldErrors, setCreateFieldErrors] = useState<FieldErrors>({})
  const [editFieldErrors, setEditFieldErrors] = useState<FieldErrors>({})
  const [createErrorMessage, setCreateErrorMessage] = useState('')
  const [isSubmittingTask, setIsSubmittingTask] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)

  /**
   * タスクフォームをクライアント側で検証する。
   */
  const validateTaskForm = (title: string, status: string, priority: string, assignedUserId: string): FieldErrors => {
    const next: FieldErrors = {}

    if (!title.trim()) next.title = 'タイトルを入力してください。'
    else if (title.trim().length > 100) next.title = 'タイトルは100文字以内で入力してください。'
    if (!priority.trim()) next.priority = '優先度を選択してください。'
    if (!status.trim()) next.status = 'ステータスを選択してください。'
    if (assignedUserId && !assigneeOptions.some((option) => option.value === assignedUserId)) {
      // 担当者候補に存在しないIDは、古い選択肢や不正な入力として扱う。
      next.assignedUserId = '担当者を選択してください。'
    }

    return next
  }

  /**
   * タスク作成フォームを送信し、成功時は一覧へ戻る。
   */
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
      // 作成後は一覧で新しいタスクを確認できるようにする。
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

  /**
   * タスク編集フォームを送信し、成功時は詳細表示へ戻る。
   */
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
        ...toTaskRequestPayload(editForm),
        version: Number(selectedTask?.version ?? 0),
      })
      setSelectedTask(updatedTask)
      setEditFieldErrors({})
      await Promise.all([loadTasks(), refreshUnreadCount()])
      stopEditing()
      setGlobalSuccessMessage('タスクを更新しました。')
    } catch (error) {
      const errorCode = extractApiErrorCode(error)
      const apiFieldErrors = extractFieldErrorsFromApiError(error)
      if (hasFieldErrors(apiFieldErrors)) {
        setEditFieldErrors(apiFieldErrors)
      }

      if (errorCode === 'ERR-TASK-007') {
        // 楽観ロック競合時は最新タスクを取得し、ユーザーに内容確認後の再編集を促す。
        const latestTask = await reloadSelectedTask(selectedTaskId)
        setEditFieldErrors({})

        if (latestTask) {
          setEditForm(toTaskFormState(latestTask))
          setDetailErrorMessage('他のユーザーによりタスクが更新されました。最新状態を再読み込みしました。内容を確認して再編集してください。')
          return
        }
      }

      setDetailErrorMessage(resolveUserMessage(error))
    } finally {
      setIsSubmittingTask(false)
    }
  }

  /**
   * 選択中タスクを削除する。
   */
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
    if (selectedTask) {
      // 詳細取得や競合解消で選択タスクが更新されたら、編集フォームも最新内容へ同期する。
      setEditFieldErrors({})
      setEditForm(toTaskFormState(selectedTask))
    }
  }, [selectedTask])

  const createTaskForm = useMemo(
    () => toTaskFormBindings(createForm, createFieldErrors, setCreateForm, setCreateFieldErrors),
    [createFieldErrors, createForm],
  )
  const editTaskForm = useMemo(
    () => toTaskFormBindings(editForm, editFieldErrors, setEditForm, setEditFieldErrors),
    [editFieldErrors, editForm],
  )

  /**
   * タスク作成フォームと作成エラーを初期化する。
   */
  const resetCreateState = () => {
    setCreateErrorMessage('')
    setCreateFieldErrors({})
    setCreateForm(defaultTaskForm)
  }

  /**
   * 編集フォームのフィールドエラーをクリアする。
   */
  const clearEditErrors = () => {
    setEditFieldErrors({})
  }

  /**
   * 編集フォームを現在選択中タスクの内容へ戻す。
   */
  const resetEditForm = () => {
    setEditFieldErrors({})
    setEditForm(selectedTask ? toTaskFormState(selectedTask) : defaultTaskForm)
  }

  /**
   * タスク変更操作に関する状態を初期化する。
   */
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
    resetEditForm,
    clearMutationState,
  }
}
