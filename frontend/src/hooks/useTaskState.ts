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
  go,
  resetMessages,
  setGlobalSuccessMessage,
  refreshUnreadCount,
}: Params): UseTaskStateResult {
  const list = useTaskListState({ isLoggedIn })
  const detail = useTaskDetailState({ selectedTaskId })
  const assignableUsers = useAssignableUsers({
    isLoggedIn,
    selectedTask: detail.selectedTask,
  })
  // mutation hookには、更新後に同期が必要な一覧・詳細・担当者候補・通知の操作を渡す。
  const mutation = useTaskMutationState({
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

  /**
   * タスク一覧へ戻り、一覧を再取得する。
   */
  const handleShowList = async () => {
    resetMessages()
    list.clearTaskErrorMessage()
    detail.clearDetailErrorMessage()
    go('/tasks')
    await list.loadTasks()
  }

  /**
   * タスク作成画面へ移動する。
   */
  const handleShowCreate = () => {
    resetMessages()
    list.clearTaskErrorMessage()
    mutation.resetCreateState()
    go('/tasks/new')
  }

  /**
   * 指定タスクの詳細画面へ移動する。
   */
  const handleShowDetail = (taskId: number | string) => {
    resetMessages()
    list.clearTaskErrorMessage()
    detail.clearDetailErrorMessage()
    go(`/tasks/${taskId}`)
  }

  /**
   * 詳細画面を編集モードへ切り替える。
   */
  const handleStartEdit = () => {
    resetMessages()
    detail.clearDetailErrorMessage()
    mutation.clearEditErrors()
    detail.startEditing()
  }

  /**
   * 編集内容を破棄し、詳細表示へ戻る。
   */
  const handleCancelEdit = () => {
    mutation.resetEditForm()
    detail.cancelEditing()
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
