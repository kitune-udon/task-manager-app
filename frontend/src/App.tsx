import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'
import { login, register, extractApiErrorMessage } from './lib/authApi'
import { UNAUTHORIZED_EVENT } from './lib/apiClient'
import { clearAuthToken, getAuthToken, saveAuthToken } from './lib/authStorage'
import { createTask, fetchTaskById, fetchTasks, type TaskItem } from './lib/taskApi'

type AuthMode = 'login' | 'register'
type AppPage = 'list' | 'create' | 'detail'

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

function formatDateTime(value?: string) {
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
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

function toApiDateTime(value: string) {
  if (!value) {
    return undefined
  }

  return `${value}T00:00:00`
}

function App() {
  const [mode, setMode] = useState<AuthMode>('login')
  const [page, setPage] = useState<AppPage>('list')
  const [token, setToken] = useState<string>(() => getAuthToken())
  const [loginEmail, setLoginEmail] = useState('tasktester@example.com')
  const [loginPassword, setLoginPassword] = useState('password123')
  const [registerName, setRegisterName] = useState('')
  const [registerEmail, setRegisterEmail] = useState('')
  const [registerPassword, setRegisterPassword] = useState('')
  const [registerPasswordConfirm, setRegisterPasswordConfirm] = useState('')
  const [createTitle, setCreateTitle] = useState('')
  const [createDescription, setCreateDescription] = useState('')
  const [createStatus, setCreateStatus] = useState('TODO')
  const [createPriority, setCreatePriority] = useState('MEDIUM')
  const [createDueDate, setCreateDueDate] = useState('')
  const [createAssignedUserId, setCreateAssignedUserId] = useState('')
  const [selectedTaskId, setSelectedTaskId] = useState<number | string | null>(null)
  const [selectedTask, setSelectedTask] = useState<TaskItem | null>(null)
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [tasks, setTasks] = useState<TaskItem[]>([])
  const [taskErrorMessage, setTaskErrorMessage] = useState('')
  const [isLoadingTasks, setIsLoadingTasks] = useState(false)
  const [isLoadingDetail, setIsLoadingDetail] = useState(false)
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

  const loadTaskDetail = async (taskId: number | string) => {
    setIsLoadingDetail(true)
    setTaskErrorMessage('')

    try {
      const task = await fetchTaskById(taskId)
      setSelectedTask(task)
    } catch (error) {
      setTaskErrorMessage(extractApiErrorMessage(error))
      setSelectedTask(null)
    } finally {
      setIsLoadingDetail(false)
    }
  }

  useEffect(() => {
    const handleUnauthorized = () => {
      setToken('')
      setLoginPassword('')
      setErrorMessage('認証期限が切れたため、再度ログインしてください。')
      setSuccessMessage('')
      setMode('login')
      setPage('list')
      setSelectedTaskId(null)
      setSelectedTask(null)
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

  useEffect(() => {
    if (isLoggedIn && page === 'detail' && selectedTaskId !== null) {
      void loadTaskDetail(selectedTaskId)
    }
  }, [isLoggedIn, page, selectedTaskId])

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

  const resetCreateForm = () => {
    setCreateTitle('')
    setCreateDescription('')
    setCreateStatus('TODO')
    setCreatePriority('MEDIUM')
    setCreateDueDate('')
    setCreateAssignedUserId('')
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

  const validateCreateForm = () => {
    if (!createTitle.trim()) {
      return 'タイトルを入力してください。'
    }
    if (!createPriority.trim()) {
      return '優先度を選択してください。'
    }
    if (!createStatus.trim()) {
      return 'ステータスを選択してください。'
    }
    if (createAssignedUserId && !/^\d+$/.test(createAssignedUserId)) {
      return '担当者IDは数値で入力してください。'
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
      setPage('list')
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

  const handleCreateTask = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    resetMessages()
    setTaskErrorMessage('')

    const validationMessage = validateCreateForm()
    if (validationMessage) {
      setTaskErrorMessage(validationMessage)
      return
    }

    setIsSubmitting(true)
    try {
      await createTask({
        title: createTitle.trim(),
        description: createDescription.trim() || undefined,
        status: createStatus,
        priority: createPriority,
        dueDate: toApiDateTime(createDueDate),
        assignedUserId: createAssignedUserId ? Number(createAssignedUserId) : undefined,
      })

      resetCreateForm()
      await loadTasks()
      setPage('list')
      setSuccessMessage('タスクを作成しました。')
    } catch (error) {
      setTaskErrorMessage(extractApiErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleShowCreate = () => {
    resetMessages()
    setTaskErrorMessage('')
    setPage('create')
  }

  const handleShowDetail = (taskId: number | string) => {
    resetMessages()
    setTaskErrorMessage('')
    setSelectedTaskId(taskId)
    setSelectedTask(null)
    setPage('detail')
  }

  const handleBackToList = async () => {
    resetMessages()
    setTaskErrorMessage('')
    setSelectedTaskId(null)
    setSelectedTask(null)
    setPage('list')
    await loadTasks()
  }

  const handleLogout = () => {
    clearAuthToken()
    setToken('')
    setLoginPassword('')
    setPage('list')
    setSelectedTaskId(null)
    setSelectedTask(null)
    setTasks([])
    setTaskErrorMessage('')
    resetMessages()
  }

  if (isLoggedIn && page === 'detail') {
    return (
      <main className="app-shell page-shell">
        <section className="panel dashboard-panel detail-panel">
          <div className="panel-header list-header">
            <div>
              <p className="eyebrow">Task Manager MVP</p>
              <h1>タスク詳細</h1>
              <p className="subtext">/api/tasks/{'{id}'} から取得したタスク詳細を表示しています。</p>
            </div>
            <div className="header-actions">
              <button className="secondary-button" onClick={() => void handleBackToList()} type="button">
                一覧へ戻る
              </button>
              <button className="secondary-button" onClick={handleLogout} type="button">
                ログアウト
              </button>
            </div>
          </div>

          {taskErrorMessage && <div className="status-box error-box">{taskErrorMessage}</div>}
          {successMessage && <div className="status-box success-box">{successMessage}</div>}

          <div className="table-card detail-card">
            {isLoadingDetail ? (
              <p className="empty-message">タスク詳細を読み込み中です...</p>
            ) : !selectedTask ? (
              <p className="empty-message">タスク詳細を取得できませんでした。</p>
            ) : (
              <div className="detail-grid">
                <div className="detail-item">
                  <span className="detail-label">ID</span>
                  <strong>{selectedTask.id}</strong>
                </div>
                <div className="detail-item detail-item-wide">
                  <span className="detail-label">タイトル</span>
                  <strong>{selectedTask.title}</strong>
                </div>
                <div className="detail-item detail-item-wide">
                  <span className="detail-label">説明</span>
                  <p>{selectedTask.description ?? '-'}</p>
                </div>
                <div className="detail-item">
                  <span className="detail-label">ステータス</span>
                  <span className="badge">{selectedTask.status ?? '-'}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">優先度</span>
                  <span className="badge">{selectedTask.priority ?? '-'}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">担当者</span>
                  <strong>{selectedTask.assignedUserName ?? '-'}</strong>
                </div>
                <div className="detail-item">
                  <span className="detail-label">期限</span>
                  <strong>{formatDate(selectedTask.dueDate)}</strong>
                </div>
                <div className="detail-item">
                  <span className="detail-label">作成日時</span>
                  <strong>{formatDateTime(selectedTask.createdAt)}</strong>
                </div>
                <div className="detail-item">
                  <span className="detail-label">更新日時</span>
                  <strong>{formatDateTime(selectedTask.updatedAt)}</strong>
                </div>
              </div>
            )}
          </div>

          <div className="detail-actions">
            <button className="secondary-button" type="button" disabled>
              編集
            </button>
            <button className="secondary-button danger-button" type="button" disabled>
              削除
            </button>
          </div>
        </section>
      </main>
    )
  }

  if (isLoggedIn && page === 'create') {
    return (
      <main className="app-shell page-shell">
        <section className="panel dashboard-panel form-panel">
          <div className="panel-header list-header">
            <div>
              <p className="eyebrow">Task Manager MVP</p>
              <h1>タスク作成</h1>
              <p className="subtext">タイトルや優先度などを入力し、/api/tasks へPOSTして新規タスクを作成します。</p>
            </div>
            <div className="header-actions">
              <button className="secondary-button" onClick={() => void handleBackToList()} type="button">
                一覧へ戻る
              </button>
              <button className="secondary-button" onClick={handleLogout} type="button">
                ログアウト
              </button>
            </div>
          </div>

          {taskErrorMessage && <div className="status-box error-box">{taskErrorMessage}</div>}
          {successMessage && <div className="status-box success-box">{successMessage}</div>}

          <form className="form-grid create-form" onSubmit={handleCreateTask}>
            <label>
              <span>タイトル</span>
              <input
                type="text"
                value={createTitle}
                onChange={(event) => setCreateTitle(event.target.value)}
                placeholder="例: API接続確認タスク"
              />
            </label>

            <label className="form-column-full">
              <span>説明</span>
              <textarea
                value={createDescription}
                onChange={(event) => setCreateDescription(event.target.value)}
                placeholder="タスクの背景や対応内容を入力"
                rows={4}
              />
            </label>

            <label>
              <span>ステータス</span>
              <select value={createStatus} onChange={(event) => setCreateStatus(event.target.value)}>
                {STATUS_OPTIONS.filter((option) => option.value !== 'ALL').map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>

            <label>
              <span>優先度</span>
              <select value={createPriority} onChange={(event) => setCreatePriority(event.target.value)}>
                {PRIORITY_OPTIONS.filter((option) => option.value !== 'ALL').map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>

            <label>
              <span>期限</span>
              <input type="date" value={createDueDate} onChange={(event) => setCreateDueDate(event.target.value)} />
            </label>

            <label>
              <span>担当者ID（任意）</span>
              <input
                type="text"
                inputMode="numeric"
                value={createAssignedUserId}
                onChange={(event) => setCreateAssignedUserId(event.target.value)}
                placeholder="例: 1"
              />
            </label>

            <div className="form-actions form-column-full">
              <button className="secondary-button" onClick={() => void handleBackToList()} type="button">
                キャンセル
              </button>
              <button className="primary-button" disabled={isSubmitting} type="submit">
                {isSubmitting ? '作成中...' : 'タスクを作成'}
              </button>
            </div>
          </form>
        </section>
      </main>
    )
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
              <button className="primary-button" onClick={handleShowCreate} type="button">
                タスク作成
              </button>
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
                      <th>操作</th>
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
                        <td>
                          <button className="secondary-button table-action-button" onClick={() => handleShowDetail(task.id)} type="button">
                            詳細
                          </button>
                        </td>
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
