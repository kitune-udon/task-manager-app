import type { FormEvent } from 'react'
import { AuthShell } from '../components/AuthShell'

type Props = {
  email: string
  password: string
  fieldErrors: Record<string, string>
  errorMessage: string
  successMessage: string
  isSubmitting: boolean
  onEmailChange: (value: string) => void
  onPasswordChange: (value: string) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  onShowRegister: () => void
}

export function LoginPage(props: Props) {
  const {
    email,
    password,
    fieldErrors,
    errorMessage,
    successMessage,
    isSubmitting,
    onEmailChange,
    onPasswordChange,
    onSubmit,
    onShowRegister,
  } = props

  return (
    <AuthShell>
      <section className="auth-card auth-card-login">
        <div className="auth-card-header centered">
          <h1>TaskFlow</h1>
          <p className="subtext centered">チーム向けタスク管理サービス</p>
        </div>

        {errorMessage ? <div className="status-box error-box">{errorMessage}</div> : null}
        {successMessage ? <div className="status-box success-box">{successMessage}</div> : null}

        <form className="form-grid auth-form" onSubmit={onSubmit}>
          <label>
            <span>メールアドレス</span>
            <input
              className={fieldErrors.email ? 'input-error' : ''}
              type="email"
              value={email}
              onChange={(e) => onEmailChange(e.target.value)}
            />
            {fieldErrors.email ? <span className="field-error">{fieldErrors.email}</span> : null}
          </label>

          <label>
            <span>パスワード</span>
            <input
              className={fieldErrors.password ? 'input-error' : ''}
              type="password"
              value={password}
              onChange={(e) => onPasswordChange(e.target.value)}
            />
            {fieldErrors.password ? <span className="field-error">{fieldErrors.password}</span> : null}
          </label>

          <button className="primary-button auth-submit-button" disabled={isSubmitting} type="submit">
            {isSubmitting ? '送信中...' : 'ログイン'}
          </button>
        </form>

        <div className="auth-inline-links">
          <button className="text-link-button" onClick={onShowRegister} type="button">
            新規登録
          </button>
        </div>
      </section>
    </AuthShell>
  )
}
