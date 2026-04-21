import { useEffect, useMemo, useState } from 'react'
import { navigateTo, parseRoute, type ResolvedRoute } from '../app_navigation'

/**
 * 現在のURLから解決したアプリ内ルートと、画面遷移用の操作を管理する。
 */
export function useRouteState() {
  const [route, setRoute] = useState<ResolvedRoute>(() => parseRoute(window.location.pathname))

  useEffect(() => {
    // ブラウザの戻る/進む操作でもReact側のルート状態をURLに同期する。
    const handlePopState = () => setRoute(parseRoute(window.location.pathname))
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

  // サイドバーでは詳細画面もタスク一覧セクションとして扱う。
  const activePath =
    route.page === 'create'
      ? '/tasks/new'
      : route.page === 'notifications'
        ? '/notifications'
        : '/tasks'

  return {
    route,
    go,
    selectedTaskId,
    activePath,
  }
}
