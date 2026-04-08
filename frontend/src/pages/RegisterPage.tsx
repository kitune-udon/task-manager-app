import type { FormEvent } from 'react'
import { AuthShell } from '../components/AuthShell'

type Props = {
  name: string
  email: string
  password: string
  passwordConfirm: string
  fieldErrors: Record<string, string>
  errorMessage: string
  successMessage: string
  isSubmitting: boolean
  onNameChange: (value: string) => void
  onEmailChange: (value: string) => void
  onPasswordChange: (value: string) => void
  onPasswordConfirmChange: (value: string) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  onShowLogin: () => void
}

export function RegisterPage(props: Props) {
  const {
    name,
    email,
    password,
    passwordConfirm,
    fieldErrors,
    errorMessage,
    successMessage,
    isSubmitting,
    onNameChange,
    onEmailChange,
    onPasswordChange,
    onPasswordConfirmChange,
    onSubmit,
    onShowLogin,
  } = props

  return (
    <AuthShell>
      <section className="auth-card auth-card-register">
        <div className="auth-card-header centered">
          <h1>新規登録</h1>
          <p className="subtext centered">アカウントを作成してタスク管理を始めます</p>
        </div>

        {errorMessage ? <div className="status-box warning-box">{errorMessage}</div> : null}
        {successMessage ? <div className="status-box success-box">{successMessage}</div> : null}

        <form className="form-grid auth-form" onSubmit={onSubmit}>
          <label>
            <span>ユーザー名 <em className="required-mark">*</em></span>
            <input
              className={fieldErrors.name ? 'input-error' : ''}
              type="text"
              value={name}
              onChange={(e) => onNameChange(e.target.value)}
            />
            {fieldErrors.name ? <span className="field-error">{fieldErrors.name}</span> : null}
          </label>

          <label>
            <span>メールアドレス <em className="required-mark">*</em></span>
            <input
              className={fieldErrors.email ? 'input-error' : ''}
              type="email"
              value={email}
              onChange={(e) => onEmailChange(e.target.value)}
            />
            {fieldErrors.email ? <span className="field-error">{fieldErrors.email}</span> : null}
          </label>

          <label>
            <span>パスワード <em className="required-mark">*</em></span>
            <input
              className={fieldErrors.password ? 'input-error' : ''}
              type="password"
              value={password}
              onChange={(e) => onPasswordChange(e.target.value)}
            />
            {fieldErrors.password ? <span className="field-error">{fieldErrors.password}</span> : null}
          </label>

          <label>
            <span>パスワード確認 <em className="required-mark">*</em></span>
            <input
              className={fieldErrors.passwordConfirm ? 'input-error' : ''}
              type="password"
              value={passwordConfirm}
              onChange={(e) => onPasswordConfirmChange(e.target.value)}
            />
            {fieldErrors.passwordConfirm ? <span className="field-error">{fieldErrors.passwordConfirm}</span> : null}
          </label>

          <button className="primary-button auth-submit-button" disabled={isSubmitting} type="submit">
            {isSubmitting ? '送信中...' : '登録する'}
          </button>
        </form>

        <div className="auth-bottom-text">
          すでにアカウントをお持ちですか？
          <button className="text-link-button inline" onClick={onShowLogin} type="button">
            ログインへ戻る
          </button>
        </div>
      </section>
    </AuthShell>
  )
}
