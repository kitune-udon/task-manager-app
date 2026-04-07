import { useEffect, useMemo, useState } from 'react'
import { navigateTo, parseRoute, type ResolvedRoute } from '../app_navigation'

export function useRouteState() {
  const [route, setRoute] = useState<ResolvedRoute>(() => parseRoute(window.location.pathname))

  useEffect(() => {
    const handlePopState = () => setRoute(parseRoute(window.location.pathname))
    window.addEventListener('popstate', handlePopState)

    return () => {
      window.removeEventListener('popstate', handlePopState)
    }
  }, [])

  const go = (path: string, replace = false) => navigateTo(path, replace)

  const selectedTaskId = useMemo(
    () => (route.page === 'detail' || route.page === 'edit' ? route.taskId : null),
    [route],
  )

  const activePath = route.page === 'create' ? '/tasks/new' : '/tasks'

  return {
    route,
    go,
    selectedTaskId,
    activePath,
  }
}
