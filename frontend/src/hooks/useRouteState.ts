import { useEffect, useMemo, useState } from 'react'
import { navigateTo, parseRoute, type ResolvedRoute } from '../app_navigation'

/**
 * 現在のURLから解決したアプリ内ルートと、画面遷移用の操作を管理する。
 */
export function useRouteState() {
  const [route, setRoute] = useState<ResolvedRoute>(() => parseRoute(window.location.pathname, window.location.search))

  useEffect(() => {
    // ブラウザの戻る/進む操作でもReact側のルート状態をURLに同期する。
    const handlePopState = () => setRoute(parseRoute(window.location.pathname, window.location.search))
    window.addEventListener('popstate', handlePopState)

    return () => {
      window.removeEventListener('popstate', handlePopState)
    }
  }, [])

  /**
   * アプリ内のURLを変更する。
   */
  const go = (path: string, replace = false) => navigateTo(path, replace)

  /**
   * 詳細画面で選択中のタスクID。
   */
  const selectedTaskId = useMemo(
    () => (route.page === 'detail' ? route.taskId : null),
    [route],
  )

  /**
   * 詳細画面で選択中のチームID。
   */
  const selectedTeamId = useMemo(
    () => (route.page === 'teamDetail' ? route.teamId : null),
    [route],
  )

  /**
   * タスク画面がURLクエリで保持しているチームID。
   */
  const taskTeamId = useMemo(
    () => (route.page === 'list' || route.page === 'create' || route.page === 'detail' ? route.teamId ?? null : null),
    [route],
  )

  /**
   * タスク詳細の戻り元。
   */
  const routeFrom = useMemo(
    () => (route.page === 'detail' ? route.from ?? null : null),
    [route],
  )

  /**
   * タスク詳細ルートの表示モード。
   */
  const taskMode = useMemo(
    () => (route.page === 'detail' ? route.mode ?? 'view' : null),
    [route],
  )

  // サイドバーでは詳細画面もタスク一覧セクションとして扱う。
  const activePath = route.page === 'teams' || route.page === 'teamDetail'
    ? '/teams'
    : route.page === 'notifications'
      ? '/notifications'
      : '/tasks'

  return {
    route,
    go,
    selectedTaskId,
    selectedTeamId,
    taskTeamId,
    routeFrom,
    taskMode,
    activePath,
  }
}
