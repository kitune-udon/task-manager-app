import { useEffect } from 'react'
import { useAssignableUsers } from './useAssignableUsers'
import { useTaskDetailState } from './useTaskDetailState'
import { useTaskListState } from './useTaskListState'
import { useTaskMutationState } from './useTaskMutationState'

/**
 * タスク状態hookが必要とする認証状態、選択中タスク、画面遷移、共通メッセージ操作。
 */
type Params = {
  isLoggedIn: boolean
  selectedTaskId: string | null
  routeTeamId: string | null
  routeFrom: string | null
  routeMode: 'view' | 'edit' | null
  go: (path: string, replace?: boolean) => void
  resetMessages: () => void
  setGlobalSuccessMessage: (value: string) => void
  refreshUnreadCount: () => Promise<void>
}

/**
 * タスク関連画面が利用する一覧・詳細・変更・担当者候補の状態と操作。
 */
export type UseTaskStateResult = {
  list: ReturnType<typeof useTaskListState>
  detail: ReturnType<typeof useTaskDetailState>
  mutation: ReturnType<typeof useTaskMutationState>
  assignableUsers: ReturnType<typeof useAssignableUsers>
  context: {
    teamId: string | null
    from: string | null
    taskListPath: string
  }
  actions: {
    reloadTasks: () => Promise<void>
    handleShowList: () => Promise<void>
    handleShowCreate: () => void
    handleShowDetail: (taskId: number | string) => void
    handleStartEdit: () => void
    handleCancelEdit: () => void
    handleCreateTask: ReturnType<typeof useTaskMutationState>['handleCreateTask']
    handleEditTask: ReturnType<typeof useTaskMutationState>['handleEditTask']
    handleDeleteTask: () => Promise<void>
    clearTaskStateOnLogout: () => void
  }
}

/**
 * タスク一覧、詳細、作成/更新/削除、担当者候補の各hookを束ね、画面遷移用アクションを提供する。
 */
export function useTaskState({
  isLoggedIn,
  selectedTaskId,
  routeTeamId,
  routeFrom,
  routeMode,
  go,
  resetMessages,
  setGlobalSuccessMessage,
  refreshUnreadCount,
}: Params): UseTaskStateResult {
  const list = useTaskListState({ isLoggedIn, teamId: routeTeamId })
  const detail = useTaskDetailState({ selectedTaskId })
  const taskListPath = routeFrom === 'notifications'
    ? '/notifications'
    : routeTeamId
      ? `/tasks?teamId=${encodeURIComponent(routeTeamId)}`
      : '/tasks'
  const assignableTeamId = routeTeamId ?? detail.selectedTask?.teamId ?? null
  const assignableUsers = useAssignableUsers({
    isLoggedIn,
    selectedTask: detail.selectedTask,
    teamId: assignableTeamId,
  })

  const buildTaskDetailPath = (taskId: number | string, mode: 'view' | 'edit' = 'view') => {
    const params = new URLSearchParams()
    if (routeTeamId) {
      params.set('teamId', routeTeamId)
    }
    if (routeFrom) {
      params.set('from', routeFrom)
    }

    const query = params.toString()
    const pathname = mode === 'edit' ? `/tasks/${taskId}/edit` : `/tasks/${taskId}`
    return query ? `${pathname}?${query}` : pathname
  }

  // mutation hookには、更新後に同期が必要な一覧・詳細・担当者候補・通知の操作を渡す。
  const mutation = useTaskMutationState({
    taskTeamId: routeTeamId,
    taskListPath,
    taskDetailPath: selectedTaskId ? buildTaskDetailPath(selectedTaskId) : null,
    selectedTaskId,
    selectedTask: detail.selectedTask,
    assigneeOptions: assignableUsers.assigneeOptions,
    loadTasks: list.loadTasks,
    reloadSelectedTask: detail.loadTaskDetail,
    setSelectedTask: detail.setSelectedTask,
    setDetailErrorMessage: detail.setDetailErrorMessage,
    resetMessages,
    setGlobalSuccessMessage,
    stopEditing: detail.cancelEditing,
    refreshUnreadCount,
    go,
  })

  useEffect(() => {
    if (!selectedTaskId || !detail.selectedTask) {
      return
    }

    if (routeMode === 'edit' && !detail.isEditing) {
      detail.startEditing()
      return
    }

    if (routeMode !== 'edit' && detail.isEditing) {
      detail.cancelEditing()
    }
  }, [detail, routeMode, selectedTaskId])

  /**
   * タスク一覧へ戻り、一覧を再取得する。
   */
  const handleShowList = async () => {
    resetMessages()
    list.clearTaskErrorMessage()
    detail.clearDetailErrorMessage()
    go(taskListPath)
    await list.loadTasks()
  }

  /**
   * タスク作成画面へ移動する。
   */
  const handleShowCreate = () => {
    resetMessages()
    list.clearTaskErrorMessage()
    mutation.resetCreateState()
    if (!routeTeamId) {
      go('/teams')
      return
    }

    go(`/tasks/new?teamId=${encodeURIComponent(routeTeamId)}`)
  }

  /**
   * 指定タスクの詳細画面へ移動する。
   */
  const handleShowDetail = (taskId: number | string) => {
    resetMessages()
    list.clearTaskErrorMessage()
    detail.clearDetailErrorMessage()
    go(buildTaskDetailPath(taskId))
  }

  /**
   * 詳細画面を編集モードへ切り替える。
   */
  const handleStartEdit = () => {
    resetMessages()
    detail.clearDetailErrorMessage()
    mutation.clearEditErrors()
    if (selectedTaskId) {
      go(buildTaskDetailPath(selectedTaskId, 'edit'))
    }
  }

  /**
   * 編集内容を破棄し、詳細表示へ戻る。
   */
  const handleCancelEdit = () => {
    mutation.resetEditForm()
    detail.cancelEditing()
    if (selectedTaskId) {
      go(buildTaskDetailPath(selectedTaskId), true)
    }
  }

  /**
   * ログアウト時にタスク関連の状態をすべて初期化する。
   */
  const clearTaskStateOnLogout = () => {
    // 次のログインユーザーに前ユーザーの一覧・詳細・フォーム状態を見せない。
    list.clearListState()
    detail.clearDetailState()
    assignableUsers.clearAssignableUsersState()
    mutation.clearMutationState()
  }

  return {
    list,
    detail,
    mutation,
    assignableUsers,
    context: {
      teamId: routeTeamId,
      from: routeFrom,
      taskListPath,
    },
    actions: {
      reloadTasks: list.loadTasks,
      handleShowList,
      handleShowCreate,
      handleShowDetail,
      handleStartEdit,
      handleCancelEdit,
      handleCreateTask: mutation.handleCreateTask,
      handleEditTask: mutation.handleEditTask,
      handleDeleteTask: mutation.handleDeleteTask,
      clearTaskStateOnLogout,
    },
  }
}
