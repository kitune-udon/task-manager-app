import './App.css'
import {
  EDITABLE_PRIORITY_OPTIONS,
  EDITABLE_STATUS_OPTIONS,
  PRIORITY_OPTIONS,
  STATUS_OPTIONS,
} from './app_taskOptions'
import { useAuthState } from './hooks/useAuthState'
import { useRouteState } from './hooks/useRouteState'
import { useTaskState } from './hooks/useTaskState'
import { AuthenticatedAppView } from './app_views/AuthenticatedAppView'
import { UnauthenticatedAppView } from './app_views/UnauthenticatedAppView'

function App() {
  const navigation = useRouteState()

  const auth = useAuthState({
    go: navigation.go,
  })

  const tasks = useTaskState({
    isLoggedIn: auth.isLoggedIn,
    route: navigation.route,
    selectedTaskId: navigation.selectedTaskId,
    go: navigation.go,
    resetMessages: auth.actions.resetMessages,
    setGlobalSuccessMessage: auth.actions.setSuccessMessage,
  })

  const handleLogout = () => {
    tasks.actions.clearTaskStateOnLogout()
    auth.actions.handleLogout()
  }

  if (!auth.isLoggedIn) {
    return (
      <UnauthenticatedAppView
        mode={auth.mode}
        errorMessage={auth.errorMessage}
        successMessage={auth.successMessage}
        isSubmitting={auth.isSubmitting}
        loginForm={auth.loginForm}
        registerForm={auth.registerForm}
      />
    )
  }

  return (
    <AuthenticatedAppView
      route={navigation.route}
      selectedTaskId={navigation.selectedTaskId}
      activePath={tasks.activePath}
      currentUserLabel={auth.currentUserLabel}
      successMessage={auth.successMessage}
      onNavigate={navigation.go}
      onLogout={handleLogout}
      tasks={tasks.tasks}
      filteredTasks={tasks.filteredTasks}
      selectedTask={tasks.selectedTask}
      taskErrorMessage={tasks.taskErrorMessage}
      detailErrorMessage={tasks.detailErrorMessage}
      isLoadingTasks={tasks.isLoadingTasks}
      isLoadingDetail={tasks.isLoadingDetail}
      isSubmittingTask={tasks.isSubmittingTask}
      isDeleting={tasks.isDeleting}
      statusFilter={tasks.statusFilter}
      priorityFilter={tasks.priorityFilter}
      commentDraft={tasks.commentDraft}
      onCommentDraftChange={tasks.setCommentDraft}
      onStatusFilterChange={tasks.setStatusFilter}
      onPriorityFilterChange={tasks.setPriorityFilter}
      onShowList={() => void tasks.actions.handleShowList()}
      onShowCreate={tasks.actions.handleShowCreate}
      onShowDetail={tasks.actions.handleShowDetail}
      onShowEdit={tasks.actions.handleShowEdit}
      onReload={() => void tasks.actions.loadTasks()}
      onDelete={() => void tasks.actions.handleDeleteTask()}
      createForm={tasks.createTaskForm}
      editForm={tasks.editTaskForm}
      onCreateSubmit={tasks.actions.handleCreateTask}
      onEditSubmit={tasks.actions.handleEditTask}
      statusOptions={STATUS_OPTIONS}
      priorityOptions={PRIORITY_OPTIONS}
      editableStatusOptions={EDITABLE_STATUS_OPTIONS}
      editablePriorityOptions={EDITABLE_PRIORITY_OPTIONS}
    />
  )
}

export default App
