import type { ReactNode } from 'react'

type Props = {
  title: string
  description: string
  activePath: string
  onNavigate: (path: string) => void
  onLogout: () => void
  currentUserLabel: string
  actions?: ReactNode
  children: ReactNode
}

export function TaskShell({
  title,
  description,
  activePath,
  onNavigate,
  onLogout,
  currentUserLabel,
  actions,
  children,
}: Props) {
  const navItems = [
    { label: 'タスク一覧', path: '/tasks' },
    { label: 'タスク作成', path: '/tasks/new' },
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
              {item.label}
            </button>
          ))}
        </nav>
      </aside>

      <section className="content-area">
        <header className="content-header">
          <div>
            <h1>{title}</h1>
            <p className="subtext">{description}</p>
          </div>
          {actions ? <div className="header-actions">{actions}</div> : null}
        </header>

        {children}
      </section>
    </main>
  )
}
