import type { ReactNode } from 'react'

/**
 * 認証後画面で共通利用するヘッダー、サイドバー、本文領域の表示情報と操作。
 */
type Props = {
  title: string
  description: string
  activePath: string
  onNavigate: (path: string) => void
  onLogout: () => void
  currentUserLabel: string
  unreadCount?: number
  actions?: ReactNode
  contentAreaClassName?: string
  contentBodyClassName?: string
  children: ReactNode
}

/**
 * タスク関連画面の共通レイアウト、グローバルナビゲーション、ログアウト導線を提供するシェル。
 */
export function TaskShell({
  title,
  description,
  activePath,
  onNavigate,
  onLogout,
  currentUserLabel,
  unreadCount = 0,
  actions,
  contentAreaClassName,
  contentBodyClassName,
  children,
}: Props) {
  // サイドバーの未読バッジは幅が崩れないよう、3桁以上を99+に丸める。
  const unreadBadgeLabel = unreadCount > 99 ? '99+' : unreadCount > 0 ? unreadCount : null
  const navItems = [
    { label: 'タスク一覧', path: '/tasks' },
    { label: 'タスク作成', path: '/tasks/new' },
    { label: '通知', path: '/notifications', badge: unreadBadgeLabel },
  ]

  return (
    <main className="workspace-shell">
      <header className="app-header">
        <div>
          <p className="app-header-title">TaskFlow</p>
        </div>

        <div className="app-header-actions">
          <span className="user-chip">{currentUserLabel || 'ログイン中ユーザー名'}</span>
          <button className="secondary-button header-logout-button" onClick={onLogout} type="button">
            ログアウト
          </button>
        </div>
      </header>

      <aside className="sidebar">
        <nav className="sidebar-nav">
          {navItems.map((item) => (
            <button
              key={item.path}
              className={activePath === item.path ? 'sidebar-link active' : 'sidebar-link'}
              onClick={() => onNavigate(item.path)}
              type="button"
            >
              <span>{item.label}</span>
              {item.badge ? <span className="nav-badge">{item.badge}</span> : null}
            </button>
          ))}
        </nav>
      </aside>

      <section className={contentAreaClassName ? `content-area ${contentAreaClassName}` : 'content-area'}>
        <header className="content-header">
          <div>
            <h1>{title}</h1>
            <p className="subtext">{description}</p>
          </div>
          {actions ? <div className="header-actions">{actions}</div> : null}
        </header>

        <div className={contentBodyClassName ? `content-body ${contentBodyClassName}` : 'content-body'}>{children}</div>
      </section>
    </main>
  )
}
