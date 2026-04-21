import type { ReactNode } from 'react'

/**
 * ログイン/登録画面で共通利用する見出し、補足文、本文スロット。
 */
type Props = {
  pageTitle?: string
  headerNote?: string
  layoutLabel?: string
  children: ReactNode
}

/**
 * 未認証画面のブランドヘッダーとカード状レイアウトを提供する共通シェル。
 */
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
