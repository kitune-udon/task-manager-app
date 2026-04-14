import { useEffect, useMemo, useState } from 'react'
import { resolveUserMessage } from '../lib/authApi'
import { fetchTasks, type TaskItem } from '../lib/taskApi'

type Params = {
  isLoggedIn: boolean
}

export function useTaskListState({ isLoggedIn }: Params) {
  const [tasks, setTasks] = useState<TaskItem[]>([])
  const [taskErrorMessage, setTaskErrorMessage] = useState('')
  const [isLoadingTasks, setIsLoadingTasks] = useState(false)
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [priorityFilter, setPriorityFilter] = useState('ALL')

  const loadTasks = async () => {
    setIsLoadingTasks(true)
    setTaskErrorMessage('')

    try {
      const taskList = await fetchTasks()
      setTasks(taskList)
    } catch (error) {
      setTaskErrorMessage(resolveUserMessage(error))
    } finally {
      setIsLoadingTasks(false)
    }
  }

  const filteredTasks = useMemo(
    () =>
      tasks.filter((task) => {
        const matchesStatus = statusFilter === 'ALL' || (task.status ?? '-') === statusFilter
        const matchesPriority = priorityFilter === 'ALL' || (task.priority ?? '-') === priorityFilter
        return matchesStatus && matchesPriority
      }),
    [priorityFilter, statusFilter, tasks],
  )

  useEffect(() => {
    if (isLoggedIn) {
      void loadTasks()
    }
  }, [isLoggedIn])

  const clearTaskErrorMessage = () => {
    setTaskErrorMessage('')
  }

  const clearListState = () => {
    setTasks([])
    setTaskErrorMessage('')
    setIsLoadingTasks(false)
  }

  return {
    tasks,
    filteredTasks,
    taskErrorMessage,
    isLoadingTasks,
    statusFilter,
    priorityFilter,
    setStatusFilter,
    setPriorityFilter,
    loadTasks,
    clearTaskErrorMessage,
    clearListState,
  }
}
