import './App.css'
import {
  EDITABLE_PRIORITY_OPTIONS,
  EDITABLE_STATUS_OPTIONS,
  PRIORITY_OPTIONS,
  STATUS_OPTIONS,
} from './app_taskOptions'
import { useAuthState } from './hooks/useAuthState'
import { useNotificationState } from './hooks/useNotificationState'
import { useRouteState } from './hooks/useRouteState'
import { useTaskState } from './hooks/useTaskState'
import { AuthenticatedAppView } from './app_views/AuthenticatedAppView'
import { UnauthenticatedAppView } from './app_views/UnauthenticatedAppView'

/**
 * 認証、ルーティング、通知、タスクの状態を束ねて、ログイン状態に応じた画面を切り替えるルートコンポーネント。
 */
function App() {
  const navigation = useRouteState()

  const auth = useAuthState({
    go: navigation.go,
  })

  const notifications = useNotificationState({
    isLoggedIn: auth.isLoggedIn,
    go: navigation.go,
    routeKey: `${navigation.route.page}:${navigation.selectedTaskId ?? ''}`,
  })

  const tasks = useTaskState({
    isLoggedIn: auth.isLoggedIn,
    selectedTaskId: navigation.selectedTaskId,
    go: navigation.go,
    resetMessages: auth.actions.resetMessages,
    setGlobalSuccessMessage: auth.actions.setSuccessMessage,
    refreshUnreadCount: notifications.actions.loadUnreadCount,
  })

  /**
   * ログアウト時に認証状態だけでなく、ログインユーザーに紐づく画面状態もまとめて破棄する。
   */
  const handleLogout = () => {
    tasks.actions.clearTaskStateOnLogout()
    notifications.actions.clearNotificationState()
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
      currentUserId={auth.currentUserId}
      successMessage={auth.successMessage}
      onNavigate={navigation.go}
      onLogout={handleLogout}
      taskState={tasks}
      notificationState={notifications}
      statusOptions={STATUS_OPTIONS}
      priorityOptions={PRIORITY_OPTIONS}
      editableStatusOptions={EDITABLE_STATUS_OPTIONS}
      editablePriorityOptions={EDITABLE_PRIORITY_OPTIONS}
    />
  )
}

export default App
