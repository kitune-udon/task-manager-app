import { useCallback, useEffect, useMemo, useState } from 'react'
import { resolveUserMessage } from '../lib/authApi'
import { fetchTasks, type TaskItem } from '../lib/taskApi'

/**
 * タスク一覧取得を開始できる認証状態。
 */
type Params = {
  isLoggedIn: boolean
  teamId?: string | null
}

/**
 * タスク一覧、一覧フィルタ、読み込み状態、エラー状態を管理する。
 */
export function useTaskListState({ isLoggedIn, teamId = null }: Params) {
  const [tasks, setTasks] = useState<TaskItem[]>([])
  const [taskErrorMessage, setTaskErrorMessage] = useState('')
  const [isLoadingTasks, setIsLoadingTasks] = useState(false)
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [priorityFilter, setPriorityFilter] = useState('ALL')
  const [teamFilter, setTeamFilter] = useState('ALL')

  /**
   * 参照可能なタスク一覧を取得する。
   */
  const loadTasks = useCallback(async () => {
    setIsLoadingTasks(true)
    setTaskErrorMessage('')

    try {
      const taskList = await fetchTasks({ teamId })
      setTasks(taskList)
    } catch (error) {
      setTaskErrorMessage(resolveUserMessage(error))
    } finally {
      setIsLoadingTasks(false)
    }
  }, [teamId])

  /**
   * 現在のステータス・優先度フィルタを適用したタスク一覧。
   */
  const filteredTasks = useMemo(
    () =>
      tasks.filter((task) => {
        const matchesStatus = statusFilter === 'ALL' || (task.status ?? '-') === statusFilter
        const matchesPriority = priorityFilter === 'ALL' || (task.priority ?? '-') === priorityFilter
        const matchesTeam = teamId || teamFilter === 'ALL' || String(task.teamId ?? '') === teamFilter
        return matchesStatus && matchesPriority && matchesTeam
      }),
    [priorityFilter, statusFilter, tasks, teamFilter, teamId],
  )

  useEffect(() => {
    if (isLoggedIn) {
      // ログイン後に初回一覧を取得する。
      void loadTasks()
    }
  }, [isLoggedIn, loadTasks])

  useEffect(() => {
    if (teamId) {
      setTeamFilter('ALL')
    }
  }, [teamId])

  /**
   * タスク一覧のエラーメッセージをクリアする。
   */
  const clearTaskErrorMessage = () => {
    setTaskErrorMessage('')
  }

  /**
   * タスク一覧に関する状態を初期化する。
   */
  const clearListState = () => {
    setTasks([])
    setTaskErrorMessage('')
    setIsLoadingTasks(false)
    setStatusFilter('ALL')
    setPriorityFilter('ALL')
    setTeamFilter('ALL')
  }

  return {
    tasks,
    filteredTasks,
    taskErrorMessage,
    isLoadingTasks,
    statusFilter,
    priorityFilter,
    teamFilter,
    setStatusFilter,
    setPriorityFilter,
    setTeamFilter,
    loadTasks,
    clearTaskErrorMessage,
    clearListState,
  }
}
