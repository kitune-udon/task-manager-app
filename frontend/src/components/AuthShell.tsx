import type { ReactNode } from 'react'

type Props = {
  pageTitle?: string
  headerNote?: string
  layoutLabel?: string
  children: ReactNode
}

export function AuthShell({ pageTitle, headerNote, layoutLabel, children }: Props) {
  return (
    <main className="auth-screen">
      <section className="auth-screen-frame">
        {pageTitle ? <div className="auth-page-title">{pageTitle}</div> : null}
        <div className="auth-layout-card">
          <header className="guest-header">
            <div>
              <p className="guest-brand">TaskFlow</p>
              {headerNote ? <p className="guest-subtitle">{headerNote}</p> : null}
            </div>
            {layoutLabel ? <p className="guest-layout-label">{layoutLabel}</p> : null}
          </header>
          <div className="guest-content">{children}</div>
        </div>
      </section>
    </main>
  )
}
