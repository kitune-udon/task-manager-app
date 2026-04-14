import type { ResolvedRoute } from '../app_navigation'
import type { TaskPriority, TaskStatus } from '../lib/taskApi'
import type { UseTaskStateResult } from '../hooks/useTaskState'
import { TaskCreatePage } from '../pages/TaskCreatePage'
import { TaskDetailPage } from '../pages/TaskDetailPage'
import { TaskEditPage } from '../pages/TaskEditPage'
import { TaskListPage } from '../pages/TaskListPage'

type TaskOption<T extends string> = Array<{ label: string; value: T }>

type Props = {
  route: ResolvedRoute
  selectedTaskId: string | null
  activePath: string
  currentUserLabel: string
  successMessage: string
  onNavigate: (path: string, replace?: boolean) => void
  onLogout: () => void
  taskState: UseTaskStateResult
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
  successMessage,
  onNavigate,
  onLogout,
  taskState,
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

    case 'edit':
      return (
        <TaskEditPage
          activePath={activePath}
          currentUserLabel={currentUserLabel}
          onNavigate={onNavigate}
          onLogout={onLogout}
          onBackDetail={() => {
            if (selectedTaskId) {
              onNavigate(`/tasks/${selectedTaskId}`)
            }
          }}
          detailErrorMessage={detail.detailErrorMessage}
          successMessage={successMessage}
          isSubmitting={mutation.isSubmittingTask}
          isLoadingDetail={detail.isLoadingDetail}
          form={mutation.editTaskForm}
          onSubmit={actions.handleEditTask}
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
          onNavigate={onNavigate}
          onLogout={onLogout}
          onShowList={() => void actions.handleShowList()}
          onShowEdit={actions.handleShowEdit}
          onDelete={() => void actions.handleDeleteTask()}
          isDeleting={mutation.isDeleting}
          detailErrorMessage={detail.detailErrorMessage}
          successMessage={successMessage}
          isLoadingDetail={detail.isLoadingDetail}
          selectedTask={detail.selectedTask}
          commentDraft={detail.commentDraft}
          onCommentDraftChange={detail.setCommentDraft}
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
