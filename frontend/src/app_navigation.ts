/**
 * URLから解決したアプリ内ページ種別。
 */
export type ResolvedRoute =
  | { page: 'list'; teamId?: string | null }
  | { page: 'create'; teamId?: string | null }
  | { page: 'detail'; taskId: string; mode?: 'view' | 'edit'; teamId?: string | null; from?: string | null }
  | { page: 'teams' }
  | { page: 'teamDetail'; teamId: string }
  | { page: 'notifications' }

/**
 * URLクエリからルートで利用するパラメータを取り出す。
 */
function parseSearch(search: string) {
  const params = new URLSearchParams(search.startsWith('?') ? search : `?${search}`)
  return {
    teamId: params.get('teamId'),
    from: params.get('from'),
  }
}

/**
 * URLパスをアプリ内ルートへ変換する。
 */
export function parseRoute(pathname: string, search = ''): ResolvedRoute {
  const query = parseSearch(search)

  if (pathname === '/tasks/new') return { page: 'create', teamId: query.teamId }
  if (pathname === '/teams') return { page: 'teams' }
  if (pathname === '/notifications') return { page: 'notifications' }

  const taskEditMatch = pathname.match(/^\/tasks\/([^/]+)\/edit$/)
  if (taskEditMatch) {
    return { page: 'detail', taskId: taskEditMatch[1], mode: 'edit', teamId: query.teamId, from: query.from }
  }

  const detailMatch = pathname.match(/^\/tasks\/([^/]+)$/)
  if (detailMatch) return { page: 'detail', taskId: detailMatch[1], mode: 'view', teamId: query.teamId, from: query.from }

  const teamDetailMatch = pathname.match(/^\/teams\/([^/]+)$/)
  if (teamDetailMatch) return { page: 'teamDetail', teamId: teamDetailMatch[1] }

  return { page: 'list', teamId: query.teamId }
}

/**
 * History APIでURLを変更し、ルート状態の購読者へ変更を通知する。
 */
export function navigateTo(pathname: string, replace = false) {
  const currentPath = `${window.location.pathname}${window.location.search}`

  if (currentPath === pathname) {
    // 同じURLへの遷移でも、画面側の再同期処理を動かせるように通知する。
    window.dispatchEvent(new PopStateEvent('popstate'))
    return
  }

  const method = replace ? 'replaceState' : 'pushState'
  window.history[method]({}, '', pathname)
  window.dispatchEvent(new PopStateEvent('popstate'))
}
