/**
 * URLから解決したアプリ内ページ種別。
 */
export type ResolvedRoute =
  | { page: 'list' }
  | { page: 'create' }
  | { page: 'detail'; taskId: string }
  | { page: 'notifications' }

/**
 * URLパスをアプリ内ルートへ変換する。
 */
export function parseRoute(pathname: string): ResolvedRoute {
  if (pathname === '/tasks/new') return { page: 'create' }
  if (pathname === '/notifications') return { page: 'notifications' }

  const detailMatch = pathname.match(/^\/tasks\/([^/]+)$/)
  if (detailMatch) return { page: 'detail', taskId: detailMatch[1] }

  return { page: 'list' }
}

/**
 * History APIでURLを変更し、ルート状態の購読者へ変更を通知する。
 */
export function navigateTo(pathname: string, replace = false) {
  if (window.location.pathname === pathname) {
    // 同じURLへの遷移でも、画面側の再同期処理を動かせるように通知する。
    window.dispatchEvent(new PopStateEvent('popstate'))
    return
  }

  const method = replace ? 'replaceState' : 'pushState'
  window.history[method]({}, '', pathname)
  window.dispatchEvent(new PopStateEvent('popstate'))
}
