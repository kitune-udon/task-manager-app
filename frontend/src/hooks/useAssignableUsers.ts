import { useEffect, useMemo, useState } from 'react'
import { resolveUserMessage } from '../lib/authApi'
import type { TaskItem } from '../lib/taskApi'
import { fetchAssignableUsers } from '../lib/userApi'
import type { AssigneeOption } from './taskStateShared'

type Params = {
  isLoggedIn: boolean
  selectedTask: TaskItem | null
}

export function useAssignableUsers({ isLoggedIn, selectedTask }: Params) {
  const [assignableUsers, setAssignableUsers] = useState<Array<{ id: number | string; name: string; email: string }>>([])
  const [isLoadingAssignableUsers, setIsLoadingAssignableUsers] = useState(false)
  const [assigneeOptionsError, setAssigneeOptionsError] = useState('')

  const loadAssignableUsers = async () => {
    setIsLoadingAssignableUsers(true)
    setAssigneeOptionsError('')

    try {
      const users = await fetchAssignableUsers()
      setAssignableUsers(users)
    } catch (error) {
      setAssignableUsers([])
      setAssigneeOptionsError(resolveUserMessage(error))
    } finally {
      setIsLoadingAssignableUsers(false)
    }
  }

  useEffect(() => {
    if (isLoggedIn) {
      void loadAssignableUsers()
    } else {
      setAssignableUsers([])
      setAssigneeOptionsError('')
      setIsLoadingAssignableUsers(false)
    }
  }, [isLoggedIn])

  const assigneeOptions = useMemo<AssigneeOption[]>(() => {
    const options = assignableUsers.map((user) => ({
      value: String(user.id),
      label: `${user.name} (${user.email})`,
    }))

    const selectedAssignedUserId =
      selectedTask?.assignedUserId !== undefined && selectedTask?.assignedUserId !== null
        ? String(selectedTask.assignedUserId)
        : null

    if (selectedAssignedUserId && !options.some((option) => option.value === selectedAssignedUserId)) {
      options.unshift({
        value: selectedAssignedUserId,
        label: selectedTask?.assignedUserName ?? `ユーザーID: ${selectedAssignedUserId}`,
      })
    }

    return [{ label: '未選択', value: '' }, ...options]
  }, [assignableUsers, selectedTask?.assignedUserId, selectedTask?.assignedUserName])

  const clearAssignableUsersState = () => {
    setAssignableUsers([])
    setAssigneeOptionsError('')
    setIsLoadingAssignableUsers(false)
  }

  return {
    assigneeOptions,
    assigneeOptionsError,
    isLoadingAssignableUsers,
    loadAssignableUsers,
    clearAssignableUsersState,
  }
}
