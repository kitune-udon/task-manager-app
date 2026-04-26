import { useEffect } from 'react'
import type { ResolvedRoute } from '../app_navigation'
import type { TaskPriority, TaskStatus } from '../lib/taskApi'
import type { UseTeamStateResult } from '../hooks/useTeamState'
import type { UseTaskStateResult } from '../hooks/useTaskState'
import { useNotificationState } from '../hooks/useNotificationState'
import { TaskCreatePage } from '../pages/TaskCreatePage'
import { TaskDetailPage } from '../pages/TaskDetailPage'
import { TaskListPage } from '../pages/TaskListPage'
import { TeamDetailPage } from '../pages/TeamDetailPage'
import { TeamListPage } from '../pages/TeamListPage'
import { NotificationPage } from '../pages/NotificationPage'

type TaskOption<T extends string> = Array<{ label: string; value: T }>

/**
 * 認証後レイアウトで共有するルーティング、ユーザー、タスク、通知の状態と操作。
 */
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
  teamState: UseTeamStateResult
  notificationState: ReturnType<typeof useNotificationState>
  statusOptions: TaskOption<TaskStatus>
  priorityOptions: TaskOption<TaskPriority>
  editableStatusOptions: TaskOption<TaskStatus>
  editablePriorityOptions: TaskOption<TaskPriority>
}

/**
 * 認証済みユーザー向けに、現在のルートへ対応するページへ状態とイベントハンドラーを配線する。
 */
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
  teamState,
  notificationState,
  statusOptions,
  priorityOptions,
  editableStatusOptions,
  editablePriorityOptions,
}: Props) {
  const { list, detail, mutation, assignableUsers, actions } = taskState
  const contextTeamId = taskState.context.teamId
  const contextTeam = contextTeamId
    ? teamState.list.teams.find((team) => String(team.id) === String(contextTeamId)) ?? null
    : null
  const isTeamContextReady = Boolean(contextTeamId) && !teamState.list.isLoadingTeams && Boolean(contextTeam)

  useEffect(() => {
    if (route.page !== 'create') {
      return
    }

    if (!contextTeamId) {
      teamState.actions.showTeamListError('タスクを作成するチームを選択してください')
      onNavigate('/teams', true)
      return
    }

    if (teamState.list.hasLoadedTeams && !teamState.list.isLoadingTeams && !contextTeam) {
      teamState.actions.showTeamListError('指定されたチームでタスクを作成できません')
      onNavigate('/teams', true)
    }
  }, [contextTeam, contextTeamId, onNavigate, route.page, teamState.actions, teamState.list.hasLoadedTeams, teamState.list.isLoadingTeams])

  // 画面ごとの表示責務は各Pageへ寄せ、このコンポーネントではルートに応じたprops配線に集中する。
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
          onShowTeamDetail={() => contextTeamId ? onNavigate(`/teams/${contextTeamId}`) : onNavigate('/teams')}
          contextTeamId={contextTeamId}
          contextTeamName={contextTeam?.name ?? null}
          isLoadingTeamContext={teamState.list.isLoadingTeams}
          canCreateInTeam={isTeamContextReady}
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
          onShowTeamDetail={(teamId) => onNavigate(`/teams/${teamId}`)}
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

    case 'teams':
      return (
        <TeamListPage
          activePath={activePath}
          currentUserLabel={currentUserLabel}
          successMessage={successMessage}
          teamState={teamState}
          onNavigate={onNavigate}
          onLogout={onLogout}
          unreadCount={notificationState.unreadCount}
        />
      )

    case 'teamDetail':
      return (
        <TeamDetailPage
          activePath={activePath}
          currentUserLabel={currentUserLabel}
          currentUserId={currentUserId}
          successMessage={successMessage}
          teamState={teamState}
          onNavigate={onNavigate}
          onLogout={onLogout}
          unreadCount={notificationState.unreadCount}
          teamId={route.teamId}
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
          onShowTeamDetail={(teamId) => onNavigate(`/teams/${teamId}`)}
          onReload={() => void actions.reloadTasks()}
          onShowDetail={actions.handleShowDetail}
          contextTeamId={taskState.context.teamId}
          teams={teamState.list.teams}
          isLoadingTeams={teamState.list.isLoadingTeams}
          tasks={list.tasks}
          filteredTasks={list.filteredTasks}
          taskErrorMessage={list.taskErrorMessage}
          successMessage={successMessage}
          isLoadingTasks={list.isLoadingTasks}
          statusFilter={list.statusFilter}
          priorityFilter={list.priorityFilter}
          teamFilter={list.teamFilter}
          onStatusFilterChange={list.setStatusFilter}
          onPriorityFilterChange={list.setPriorityFilter}
          onTeamFilterChange={list.setTeamFilter}
          statusOptions={statusOptions}
          priorityOptions={priorityOptions}
        />
      )
  }
}
