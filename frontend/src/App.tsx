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
      activePath={navigation.activePath}
      currentUserLabel={auth.currentUserLabel}
      successMessage={auth.successMessage}
      onNavigate={navigation.go}
      onLogout={handleLogout}
      taskState={tasks}
      statusOptions={STATUS_OPTIONS}
      priorityOptions={PRIORITY_OPTIONS}
      editableStatusOptions={EDITABLE_STATUS_OPTIONS}
      editablePriorityOptions={EDITABLE_PRIORITY_OPTIONS}
    />
  )
}

export default App
