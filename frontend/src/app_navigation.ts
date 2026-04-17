export type ResolvedRoute =
  | { page: 'list' }
  | { page: 'create' }
  | { page: 'detail'; taskId: string }
  | { page: 'notifications' }

export function parseRoute(pathname: string): ResolvedRoute {
  if (pathname === '/tasks/new') return { page: 'create' }
  if (pathname === '/notifications') return { page: 'notifications' }

  const detailMatch = pathname.match(/^\/tasks\/([^/]+)$/)
  if (detailMatch) return { page: 'detail', taskId: detailMatch[1] }

  return { page: 'list' }
}

export function navigateTo(pathname: string, replace = false) {
  if (window.location.pathname === pathname) {
    window.dispatchEvent(new PopStateEvent('popstate'))
    return
  }

  const method = replace ? 'replaceState' : 'pushState'
  window.history[method]({}, '', pathname)
  window.dispatchEvent(new PopStateEvent('popstate'))
}
