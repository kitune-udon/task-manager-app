import type { ResolvedRoute } from '../app_navigation'
import type { TaskPriority, TaskStatus } from '../lib/taskApi'
import type { UseTaskStateResult } from '../hooks/useTaskState'
import { useNotificationState } from '../hooks/useNotificationState'
import { TaskCreatePage } from '../pages/TaskCreatePage'
import { TaskDetailPage } from '../pages/TaskDetailPage'
import { TaskListPage } from '../pages/TaskListPage'
import { NotificationPage } from '../pages/NotificationPage'

type TaskOption<T extends string> = Array<{ label: string; value: T }>

type Props = {
  route: ResolvedRoute
  selectedTaskId: string | null
  activePath: string
  currentUserLabel: string
  currentUserId: number | null
  successMessage: string
  onNavigate: (path: string, replace?: boolean) => void
  onLogout: () => void
  taskState: UseTaskStateResult
  notificationState: ReturnType<typeof useNotificationState>
  statusOptions: TaskOption<TaskStatus>
  priorityOptions: TaskOption<TaskPriority>
  editableStatusOptions: TaskOption<TaskStatus>
  editablePriorityOptions: TaskOption<TaskPriority>
}

export function AuthenticatedAppView({
  route,
  selectedTaskId,
  activePath,
  currentUserLabel,
  currentUserId,
  successMessage,
  onNavigate,
  onLogout,
  taskState,
  notificationState,
  statusOptions,
  priorityOptions,
  editableStatusOptions,
  editablePriorityOptions,
}: Props) {
  const { list, detail, mutation, assignableUsers, actions } = taskState

  switch (route.page) {
    case 'create':
      return (
        <TaskCreatePage
          activePath={activePath}
          currentUserLabel={currentUserLabel}
          onNavigate={onNavigate}
          onLogout={onLogout}
          unreadCount={notificationState.unreadCount}
          onShowList={() => void actions.handleShowList()}
          createErrorMessage={mutation.createErrorMessage}
          successMessage={successMessage}
          isSubmitting={mutation.isSubmittingTask}
          form={mutation.createTaskForm}
          onSubmit={actions.handleCreateTask}
          statusOptions={editableStatusOptions}
          priorityOptions={editablePriorityOptions}
          assigneeOptions={assignableUsers.assigneeOptions}
          isLoadingAssigneeOptions={assignableUsers.isLoadingAssignableUsers}
          assigneeOptionsError={assignableUsers.assigneeOptionsError}
        />
      )

    case 'detail':
      return (
        <TaskDetailPage
          activePath={activePath}
          currentUserLabel={currentUserLabel}
          currentUserId={currentUserId}
          onNavigate={onNavigate}
          onLogout={onLogout}
          unreadCount={notificationState.unreadCount}
          onRefreshUnreadCount={notificationState.actions.loadUnreadCount}
          onShowList={() => void actions.handleShowList()}
          onStartEdit={actions.handleStartEdit}
          onCancelEdit={actions.handleCancelEdit}
          onReloadDetail={() => (selectedTaskId ? detail.loadTaskDetail(selectedTaskId) : Promise.resolve(null))}
          onDelete={() => void actions.handleDeleteTask()}
          isDeleting={mutation.isDeleting}
          isEditing={detail.isEditing}
          activeActivityTab={detail.activeActivityTab}
          onActivityTabChange={detail.setActiveActivityTab}
          commentDraft={detail.commentDraft}
          onCommentDraftChange={detail.setCommentDraft}
          detailErrorMessage={detail.detailErrorMessage}
          successMessage={successMessage}
          isLoadingDetail={detail.isLoadingDetail}
          selectedTask={detail.selectedTask}
          editForm={mutation.editTaskForm}
          onEditSubmit={actions.handleEditTask}
          isSubmitting={mutation.isSubmittingTask}
          statusOptions={editableStatusOptions}
          priorityOptions={editablePriorityOptions}
          assigneeOptions={assignableUsers.assigneeOptions}
          isLoadingAssigneeOptions={assignableUsers.isLoadingAssignableUsers}
          assigneeOptionsError={assignableUsers.assigneeOptionsError}
        />
      )

    case 'notifications':
      return (
        <NotificationPage
          activePath={activePath}
          currentUserLabel={currentUserLabel}
          onNavigate={onNavigate}
          onLogout={onLogout}
          unreadCount={notificationState.unreadCount}
          notifications={notificationState.notifications}
          unreadOnly={notificationState.unreadOnly}
          currentPage={notificationState.currentPage}
          totalPages={notificationState.totalPages}
          totalElements={notificationState.totalElements}
          isLoadingNotifications={notificationState.isLoadingNotifications}
          isMarkingAllRead={notificationState.isMarkingAllRead}
          activeNotificationId={notificationState.activeNotificationId}
          activeNotificationAction={notificationState.activeNotificationAction}
          notificationErrorMessage={notificationState.notificationErrorMessage}
          onUnreadOnlyChange={notificationState.actions.handleUnreadOnlyChange}
          onReload={() => void notificationState.actions.loadNotifications(notificationState.currentPage, notificationState.unreadOnly)}
          onPageChange={notificationState.actions.handlePageChange}
          onOpenNotification={(notification) => void notificationState.actions.handleOpenNotification(notification)}
          onMarkAsRead={(notification) => void notificationState.actions.handleMarkAsRead(notification)}
          onMarkAllRead={() => void notificationState.actions.handleMarkAllRead()}
        />
      )

    case 'list':
    default:
      return (
        <TaskListPage
          activePath={activePath}
          currentUserLabel={currentUserLabel}
          onNavigate={onNavigate}
          onLogout={onLogout}
          unreadCount={notificationState.unreadCount}
          onShowCreate={actions.handleShowCreate}
          onReload={() => void actions.reloadTasks()}
          onShowDetail={actions.handleShowDetail}
          tasks={list.tasks}
          filteredTasks={list.filteredTasks}
          taskErrorMessage={list.taskErrorMessage}
          successMessage={successMessage}
          isLoadingTasks={list.isLoadingTasks}
          statusFilter={list.statusFilter}
          priorityFilter={list.priorityFilter}
          onStatusFilterChange={list.setStatusFilter}
          onPriorityFilterChange={list.setPriorityFilter}
          statusOptions={statusOptions}
          priorityOptions={priorityOptions}
        />
      )
  }
}
