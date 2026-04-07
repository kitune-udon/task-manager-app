import type { ReactNode } from 'react'

type Props = {
  pageTitle: string
  headerNote: string
  children: ReactNode
}

export function AuthShell({ pageTitle, headerNote, children }: Props) {
  return (
    <main className="auth-screen">
      <section className="auth-screen-frame">
        <div className="auth-page-title">{pageTitle}</div>
        <div className="auth-layout-card">
          <header className="guest-header">
            <div>
              <p className="guest-brand">TaskFlow</p>
              <p className="guest-subtitle">{headerNote}</p>
            </div>
            <p className="guest-layout-label">未ログイン共通レイアウト</p>
          </header>
          <div className="guest-content">{children}</div>
        </div>
      </section>
    </main>
  )
}
