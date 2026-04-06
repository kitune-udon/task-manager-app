import { FormEvent, useEffect, useMemo, useState } from 'react'
import './App.css'
import { login, register, extractApiErrorMessage } from './lib/authApi'
import { UNAUTHORIZED_EVENT } from './lib/apiClient'
import { clearAuthToken, getAuthToken, saveAuthToken } from './lib/authStorage'

type AuthMode = 'login' | 'register'

function App() {
  const [mode, setMode] = useState<AuthMode>('login')
  const [token, setToken] = useState<string>(() => getAuthToken())
  const [loginEmail, setLoginEmail] = useState('tasktester@example.com')
  const [loginPassword, setLoginPassword] = useState('password123')
  const [registerName, setRegisterName] = useState('')
  const [registerEmail, setRegisterEmail] = useState('')
  const [registerPassword, setRegisterPassword] = useState('')
  const [registerPasswordConfirm, setRegisterPasswordConfirm] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const isLoggedIn = useMemo(() => Boolean(token), [token])

  useEffect(() => {
    const handleUnauthorized = () => {
      setToken('')
      setLoginPassword('')
      setErrorMessage('認証期限が切れたため、再度ログインしてください。')
      setSuccessMessage('')
      setMode('login')
    }

    window.addEventListener(UNAUTHORIZED_EVENT, handleUnauthorized)
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, handleUnauthorized)
  }, [])

  const resetMessages = () => {
    setErrorMessage('')
    setSuccessMessage('')
  }

  const validateRegisterForm = () => {
    if (!registerName.trim()) {
      return '名前を入力してください。'
    }
    if (!registerEmail.trim()) {
      return 'メールアドレスを入力してください。'
    }
    if (!/^\S+@\S+\.\S+$/.test(registerEmail)) {
      return 'メールアドレスの形式が不正です。'
    }
    if (registerPassword.length < 8) {
      return 'パスワードは8文字以上で入力してください。'
    }
    if (registerPassword !== registerPasswordConfirm) {
      return '確認用パスワードが一致しません。'
    }
    return ''
  }

  const handleLogin = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    resetMessages()

    if (!loginEmail.trim()) {
      setErrorMessage('メールアドレスを入力してください。')
      return
    }
    if (!loginPassword.trim()) {
      setErrorMessage('パスワードを入力してください。')
      return
    }

    setIsSubmitting(true)
    try {
      const result = await login({
        email: loginEmail.trim(),
        password: loginPassword,
      })

      const resolvedToken = result.token ?? ''
      if (!resolvedToken) {
        setErrorMessage('トークンの取得に失敗しました。')
        return
      }

      saveAuthToken(resolvedToken)
      setToken(resolvedToken)
      setSuccessMessage('ログインに成功しました。')
    } catch (error) {
      setErrorMessage(extractApiErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleRegister = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    resetMessages()

    const validationMessage = validateRegisterForm()
    if (validationMessage) {
      setErrorMessage(validationMessage)
      return
    }

    setIsSubmitting(true)
    try {
      const result = await register({
        name: registerName.trim(),
        email: registerEmail.trim(),
        password: registerPassword,
      })

      const registeredEmail = result.email ?? registerEmail.trim()
      setSuccessMessage(`登録に成功しました。ログインしてください。(${registeredEmail})`)
      setMode('login')
      setLoginEmail(registeredEmail)
      setLoginPassword('')
      setRegisterName('')
      setRegisterEmail('')
      setRegisterPassword('')
      setRegisterPasswordConfirm('')
    } catch (error) {
      setErrorMessage(extractApiErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleLogout = () => {
    clearAuthToken()
    setToken('')
    setLoginPassword('')
    resetMessages()
  }

  if (isLoggedIn) {
    return (
      <main className="app-shell">
        <section className="panel dashboard-panel">
          <div className="panel-header">
            <div>
              <p className="eyebrow">Task Manager MVP</p>
              <h1>タスク一覧画面（仮）</h1>
            </div>
            <button className="secondary-button" onClick={handleLogout}>
              ログアウト
            </button>
          </div>

          <div className="status-box success-box">
            APIクライアントを共通化しました。JWT は interceptor で自動付与され、401 はログイン画面へ戻るようになっています。
          </div>

          <div className="token-block">
            <p className="token-label">保存済みトークン（先頭のみ表示）</p>
            <code>{token.slice(0, 40)}...</code>
          </div>
        </section>
      </main>
    )
  }

  return (
    <main className="app-shell">
      <section className="panel auth-panel">
        <div className="panel-header auth-header">
          <div>
            <p className="eyebrow">Task Manager MVP</p>
            <h1>{mode === 'login' ? 'ログイン' : '新規登録'}</h1>
            <p className="subtext">
              {mode === 'login'
                ? 'axios ベースの共通 API クライアント経由でログインします。'
                : '名前・メールアドレス・パスワードを入力してアカウントを作成します。'}
            </p>
          </div>
          <div className="mode-switch">
            <button
              className={mode === 'login' ? 'tab-button active' : 'tab-button'}
              onClick={() => {
                resetMessages()
                setMode('login')
              }}
              type="button"
            >
              ログイン
            </button>
            <button
              className={mode === 'register' ? 'tab-button active' : 'tab-button'}
              onClick={() => {
                resetMessages()
                setMode('register')
              }}
              type="button"
            >
              新規登録
            </button>
          </div>
        </div>

        {errorMessage && <div className="status-box error-box">{errorMessage}</div>}
        {successMessage && <div className="status-box success-box">{successMessage}</div>}

        {mode === 'login' ? (
          <form className="form-grid" onSubmit={handleLogin}>
            <label>
              <span>メールアドレス</span>
              <input
                type="email"
                value={loginEmail}
                onChange={(event) => setLoginEmail(event.target.value)}
                placeholder="example@example.com"
              />
            </label>

            <label>
              <span>パスワード</span>
              <input
                type="password"
                value={loginPassword}
                onChange={(event) => setLoginPassword(event.target.value)}
                placeholder="8文字以上"
              />
            </label>

            <button className="primary-button" disabled={isSubmitting} type="submit">
              {isSubmitting ? '送信中...' : 'ログイン'}
            </button>
          </form>
        ) : (
          <form className="form-grid" onSubmit={handleRegister}>
            <label>
              <span>名前</span>
              <input
                type="text"
                value={registerName}
                onChange={(event) => setRegisterName(event.target.value)}
                placeholder="山田 太郎"
              />
            </label>

            <label>
              <span>メールアドレス</span>
              <input
                type="email"
                value={registerEmail}
                onChange={(event) => setRegisterEmail(event.target.value)}
                placeholder="example@example.com"
              />
            </label>

            <label>
              <span>パスワード</span>
              <input
                type="password"
                value={registerPassword}
                onChange={(event) => setRegisterPassword(event.target.value)}
                placeholder="8文字以上"
              />
            </label>

            <label>
              <span>パスワード（確認）</span>
              <input
                type="password"
                value={registerPasswordConfirm}
                onChange={(event) => setRegisterPasswordConfirm(event.target.value)}
                placeholder="確認用パスワード"
              />
            </label>

            <button className="primary-button" disabled={isSubmitting} type="submit">
              {isSubmitting ? '送信中...' : '新規登録'}
            </button>
          </form>
        )}
      </section>
    </main>
  )
}

export default App
