import type { FormEvent } from 'react'
import type { ResolvedRoute } from '../app_navigation'
import type { TaskPriority, TaskStatus } from '../lib/taskApi'
import type { TaskFormBindings } from '../hooks/useTaskState'
import { TaskCreatePage } from '../pages/TaskCreatePage'
import { TaskDetailPage } from '../pages/TaskDetailPage'
import { TaskEditPage } from '../pages/TaskEditPage'
import { TaskListPage } from '../pages/TaskListPage'
import type { TaskItem } from '../lib/taskApi'

type TaskOption<T extends string> = Array<{ label: string; value: T }>

type Props = {
  route: ResolvedRoute
  selectedTaskId: string | null
  activePath: string
  currentUserLabel: string
  successMessage: string
  onNavigate: (path: string, replace?: boolean) => void
  onLogout: () => void
  tasks: TaskItem[]
  filteredTasks: TaskItem[]
  selectedTask: TaskItem | null
  taskErrorMessage: string
  detailErrorMessage: string
  isLoadingTasks: boolean
  isLoadingDetail: boolean
  isSubmittingTask: boolean
  isDeleting: boolean
  statusFilter: string
  priorityFilter: string
  commentDraft: string
  onCommentDraftChange: (value: string) => void
  onStatusFilterChange: (value: string) => void
  onPriorityFilterChange: (value: string) => void
  onShowList: () => void
  onShowCreate: () => void
  onShowDetail: (taskId: number | string) => void
  onShowEdit: () => void
  onReload: () => void
  onDelete: () => void
  createForm: TaskFormBindings
  editForm: TaskFormBindings
  onCreateSubmit: (event: FormEvent<HTMLFormElement>) => void
  onEditSubmit: (event: FormEvent<HTMLFormElement>) => void
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
  tasks,
  filteredTasks,
  selectedTask,
  taskErrorMessage,
  detailErrorMessage,
  isLoadingTasks,
  isLoadingDetail,
  isSubmittingTask,
  isDeleting,
  statusFilter,
  priorityFilter,
  commentDraft,
  onCommentDraftChange,
  onStatusFilterChange,
  onPriorityFilterChange,
  onShowList,
  onShowCreate,
  onShowDetail,
  onShowEdit,
  onReload,
  onDelete,
  createForm,
  editForm,
  onCreateSubmit,
  onEditSubmit,
  statusOptions,
  priorityOptions,
  editableStatusOptions,
  editablePriorityOptions,
}: Props) {
  switch (route.page) {
    case 'create':
      return (
        <TaskCreatePage
          activePath={activePath}
          currentUserLabel={currentUserLabel}
          onNavigate={onNavigate}
          onLogout={onLogout}
          onShowList={onShowList}
          taskErrorMessage={taskErrorMessage}
          successMessage={successMessage}
          isSubmitting={isSubmittingTask}
          form={createForm}
          onSubmit={onCreateSubmit}
          statusOptions={editableStatusOptions}
          priorityOptions={editablePriorityOptions}
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
          detailErrorMessage={detailErrorMessage}
          successMessage={successMessage}
          isSubmitting={isSubmittingTask}
          isLoadingDetail={isLoadingDetail}
          form={editForm}
          onSubmit={onEditSubmit}
          statusOptions={editableStatusOptions}
          priorityOptions={editablePriorityOptions}
        />
      )

    case 'detail':
      return (
        <TaskDetailPage
          activePath={activePath}
          currentUserLabel={currentUserLabel}
          onNavigate={onNavigate}
          onLogout={onLogout}
          onShowList={onShowList}
          onShowEdit={onShowEdit}
          onDelete={onDelete}
          isDeleting={isDeleting}
          detailErrorMessage={detailErrorMessage}
          successMessage={successMessage}
          isLoadingDetail={isLoadingDetail}
          selectedTask={selectedTask}
          commentDraft={commentDraft}
          onCommentDraftChange={onCommentDraftChange}
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
          onShowCreate={onShowCreate}
          onReload={onReload}
          onShowDetail={onShowDetail}
          tasks={tasks}
          filteredTasks={filteredTasks}
          taskErrorMessage={taskErrorMessage}
          successMessage={successMessage}
          isLoadingTasks={isLoadingTasks}
          statusFilter={statusFilter}
          priorityFilter={priorityFilter}
          onStatusFilterChange={onStatusFilterChange}
          onPriorityFilterChange={onPriorityFilterChange}
          statusOptions={statusOptions}
          priorityOptions={priorityOptions}
        />
      )
  }
}
