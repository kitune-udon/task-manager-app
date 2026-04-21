import { useCallback, useEffect, useState } from 'react'
import { resolveUserMessage } from '../lib/authApi'
import { fetchActivities, type ActivityItem } from '../lib/activityApi'

/**
 * アクティビティ一覧を取得する対象タスク。
 */
type Params = {
  selectedTaskId: number | string | null
}

/**
 * 選択中タスクのアクティビティ一覧、読み込み状態、エラー状態を管理する。
 */
export function useTaskActivitiesState({ selectedTaskId }: Params) {
  const [activities, setActivities] = useState<ActivityItem[]>([])
  const [isLoadingActivities, setIsLoadingActivities] = useState(false)
  const [activityErrorMessage, setActivityErrorMessage] = useState('')

  /**
   * 指定タスクのアクティビティ一覧を取得する。
   */
  const loadActivities = useCallback(async (taskId = selectedTaskId) => {
    if (!taskId) {
      setActivities([])
      setActivityErrorMessage('')
      return []
    }

    setIsLoadingActivities(true)
    setActivityErrorMessage('')

    try {
      const nextActivities = await fetchActivities(taskId)
      setActivities(nextActivities)
      return nextActivities
    } catch (error) {
      setActivities([])
      setActivityErrorMessage(resolveUserMessage(error))
      return []
    } finally {
      setIsLoadingActivities(false)
    }
  }, [selectedTaskId])

  useEffect(() => {
    setIsLoadingActivities(false)

    if (!selectedTaskId) {
      // タスク未選択時は前回タスクの履歴やエラー表示を残さない。
      setActivities([])
      setActivityErrorMessage('')
      return
    }

    void loadActivities(selectedTaskId)
  }, [loadActivities, selectedTaskId])

  return {
    activities,
    isLoadingActivities,
    activityErrorMessage,
    loadActivities,
  }
}
