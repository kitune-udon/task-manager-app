import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'
import { login, register, extractApiErrorMessage } from './lib/authApi'
import { UNAUTHORIZED_EVENT } from './lib/apiClient'
import { clearAuthToken, getAuthToken, saveAuthToken } from './lib/authStorage'
import { fetchTasks, type TaskItem } from './lib/taskApi'

type AuthMode = 'login' | 'register'

const STATUS_OPTIONS = [
  { label: 'すべて', value: 'ALL' },
  { label: 'TODO', value: 'TODO' },
  { label: 'IN_PROGRESS', value: 'IN_PROGRESS' },
  { label: 'DONE', value: 'DONE' },
]

const PRIORITY_OPTIONS = [
  { label: 'すべて', value: 'ALL' },
  { label: 'LOW', value: 'LOW' },
  { label: 'MEDIUM', value: 'MEDIUM' },
  { label: 'HIGH', value: 'HIGH' },
]

function formatDate(value?: string) {
  if (!value) {
    return '-'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat('ja-JP', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(date)
}

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
  const [tasks, setTasks] = useState<TaskItem[]>([])
  const [taskErrorMessage, setTaskErrorMessage] = useState('')
  const [isLoadingTasks, setIsLoadingTasks] = useState(false)
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [priorityFilter, setPriorityFilter] = useState('ALL')

  const isLoggedIn = useMemo(() => Boolean(token), [token])

  const loadTasks = async () => {
    setIsLoadingTasks(true)
    setTaskErrorMessage('')

    try {
      const taskList = await fetchTasks()
      setTasks(taskList)
    } catch (error) {
      setTaskErrorMessage(extractApiErrorMessage(error))
    } finally {
      setIsLoadingTasks(false)
    }
  }

  useEffect(() => {
    const handleUnauthorized = () => {
      setToken('')
      setLoginPassword('')
      setErrorMessage('認証期限が切れたため、再度ログインしてください。')
      setSuccessMessage('')
      setMode('login')
      setTasks([])
      setTaskErrorMessage('')
    }

    window.addEventListener(UNAUTHORIZED_EVENT, handleUnauthorized)
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, handleUnauthorized)
  }, [])

  useEffect(() => {
    if (isLoggedIn) {
      void loadTasks()
    }
  }, [isLoggedIn])

  const filteredTasks = useMemo(() => {
    return tasks.filter((task) => {
      const matchesStatus = statusFilter === 'ALL' || (task.status ?? '-') === statusFilter
      const matchesPriority = priorityFilter === 'ALL' || (task.priority ?? '-') === priorityFilter
      return matchesStatus && matchesPriority
    })
  }, [tasks, statusFilter, priorityFilter])

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
    setTasks([])
    setTaskErrorMessage('')
    resetMessages()
  }

  if (isLoggedIn) {
    return (
      <main className="app-shell page-shell">
        <section className="panel dashboard-panel">
          <div className="panel-header list-header">
            <div>
              <p className="eyebrow">Task Manager MVP</p>
              <h1>タスク一覧</h1>
              <p className="subtext">
                /api/tasks から取得した一覧を表示しています。status / priority で絞り込み可能です。
              </p>
            </div>
            <div className="header-actions">
              <button className="secondary-button" onClick={() => void loadTasks()} type="button">
                再読込
              </button>
              <button className="secondary-button" onClick={handleLogout} type="button">
                ログアウト
              </button>
            </div>
          </div>

          <div className="filter-grid">
            <label>
              <span>ステータス</span>
              <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
                {STATUS_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>

            <label>
              <span>優先度</span>
              <select value={priorityFilter} onChange={(event) => setPriorityFilter(event.target.value)}>
                {PRIORITY_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="summary-row">
            <div className="summary-card">
              <p className="summary-label">取得件数</p>
              <strong>{tasks.length}</strong>
            </div>
            <div className="summary-card">
              <p className="summary-label">表示件数</p>
              <strong>{filteredTasks.length}</strong>
            </div>
          </div>

          {taskErrorMessage && <div className="status-box error-box">{taskErrorMessage}</div>}
          {successMessage && <div className="status-box success-box">{successMessage}</div>}

          <div className="table-card">
            {isLoadingTasks ? (
              <p className="empty-message">タスクを読み込み中です...</p>
            ) : filteredTasks.length === 0 ? (
              <p className="empty-message">条件に一致するタスクはありません。</p>
            ) : (
              <div className="table-scroll">
                <table>
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>タイトル</th>
                      <th>ステータス</th>
                      <th>優先度</th>
                      <th>担当者</th>
                      <th>期限</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredTasks.map((task) => (
                      <tr key={String(task.id)}>
                        <td>{task.id}</td>
                        <td>
                          <div className="title-cell">
                            <strong>{task.title}</strong>
                            {task.description && <span>{task.description}</span>}
                          </div>
                        </td>
                        <td>
                          <span className="badge">{task.status ?? '-'}</span>
                        </td>
                        <td>
                          <span className="badge">{task.priority ?? '-'}</span>
                        </td>
                        <td>{task.assignedUserName ?? '-'}</td>
                        <td>{formatDate(task.dueDate)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
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
