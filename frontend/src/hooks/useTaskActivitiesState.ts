import { useCallback, useEffect, useState } from 'react'
import { resolveUserMessage } from '../lib/authApi'
import { fetchActivities, type ActivityItem } from '../lib/activityApi'

type Params = {
  selectedTaskId: number | string | null
}

export function useTaskActivitiesState({ selectedTaskId }: Params) {
  const [activities, setActivities] = useState<ActivityItem[]>([])
  const [isLoadingActivities, setIsLoadingActivities] = useState(false)
  const [activityErrorMessage, setActivityErrorMessage] = useState('')

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
