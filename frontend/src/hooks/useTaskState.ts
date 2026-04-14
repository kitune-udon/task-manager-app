import type { ResolvedRoute } from '../app_navigation'
import { useAssignableUsers } from './useAssignableUsers'
import { useTaskDetailState } from './useTaskDetailState'
import { useTaskListState } from './useTaskListState'
import { useTaskMutationState } from './useTaskMutationState'

type Params = {
  isLoggedIn: boolean
  route: ResolvedRoute
  selectedTaskId: string | null
  go: (path: string, replace?: boolean) => void
  resetMessages: () => void
  setGlobalSuccessMessage: (value: string) => void
}

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
    handleShowEdit: () => void
    handleCreateTask: ReturnType<typeof useTaskMutationState>['handleCreateTask']
    handleEditTask: ReturnType<typeof useTaskMutationState>['handleEditTask']
    handleDeleteTask: () => Promise<void>
    clearTaskStateOnLogout: () => void
  }
}

export function useTaskState({
  isLoggedIn,
  route,
  selectedTaskId,
  go,
  resetMessages,
  setGlobalSuccessMessage,
}: Params): UseTaskStateResult {
  const list = useTaskListState({ isLoggedIn })
  const detail = useTaskDetailState({ selectedTaskId })
  const assignableUsers = useAssignableUsers({
    isLoggedIn,
    selectedTask: detail.selectedTask,
  })
  const mutation = useTaskMutationState({
    routePage: route.page,
    selectedTaskId,
    selectedTask: detail.selectedTask,
    assigneeOptions: assignableUsers.assigneeOptions,
    loadTasks: list.loadTasks,
    setSelectedTask: detail.setSelectedTask,
    setDetailErrorMessage: detail.setDetailErrorMessage,
    resetMessages,
    setGlobalSuccessMessage,
    go,
  })

  const handleShowList = async () => {
    resetMessages()
    list.clearTaskErrorMessage()
    detail.clearDetailErrorMessage()
    go('/tasks')
    await list.loadTasks()
  }

  const handleShowCreate = () => {
    resetMessages()
    list.clearTaskErrorMessage()
    mutation.resetCreateState()
    go('/tasks/new')
  }

  const handleShowDetail = (taskId: number | string) => {
    resetMessages()
    list.clearTaskErrorMessage()
    detail.clearDetailErrorMessage()
    go(`/tasks/${taskId}`)
  }

  const handleShowEdit = () => {
    if (!selectedTaskId) return
    resetMessages()
    detail.clearDetailErrorMessage()
    mutation.clearEditErrors()
    go(`/tasks/${selectedTaskId}/edit`)
  }

  const clearTaskStateOnLogout = () => {
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
      handleShowEdit,
      handleCreateTask: mutation.handleCreateTask,
      handleEditTask: mutation.handleEditTask,
      handleDeleteTask: mutation.handleDeleteTask,
      clearTaskStateOnLogout,
    },
  }
}
