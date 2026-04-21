import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react'
import {
  resolveUserMessage,
  extractFieldErrorsFromApiError,
  hasFieldErrors,
  login,
  register,
} from '../lib/authApi'
import { UNAUTHORIZED_EVENT } from '../lib/apiClient'
import type { FieldErrors } from '../lib/apiError'
import {
  clearAuthToken,
  clearPostLoginRedirectPath,
  clearUserId,
  clearUserDisplayName,
  consumePostLoginRedirectPath,
  getAuthToken,
  getUserId,
  getUserDisplayName,
  isProtectedPath,
  saveAuthToken,
  savePostLoginRedirectPath,
  saveUserId,
  saveUserDisplayName,
} from '../lib/authStorage'

export type AuthMode = 'login' | 'register'

const EMAIL_PATTERN = /^\S+@\S+\.\S+$/

/**
 * 認証状態hookが画面遷移を行うために受け取る操作。
 */
type Params = {
  go: (path: string, replace?: boolean) => void
  onUnauthorized?: () => void
}

/**
 * ログイン/登録フォーム、認証トークン、認証ルート遷移をまとめて管理する。
 */
export function useAuthState({ go, onUnauthorized }: Params) {
  /**
   * URLから未認証画面の表示モードを解決する。
   */
  const resolveAuthMode = (pathname: string): AuthMode => (pathname === '/signup' ? 'register' : 'login')

  const [mode, setMode] = useState<AuthMode>(() => resolveAuthMode(window.location.pathname))
  const [token, setToken] = useState<string>(() => getAuthToken())
  const [currentUserId, setCurrentUserId] = useState<number | null>(() => getUserId())
  const [currentUserLabel, setCurrentUserLabel] = useState<string>(() => getUserDisplayName())

  const [loginEmail, setLoginEmail] = useState('')
  const [loginPassword, setLoginPassword] = useState('')
  const [registerName, setRegisterName] = useState('')
  const [registerEmail, setRegisterEmail] = useState('')
  const [registerPassword, setRegisterPassword] = useState('')
  const [registerPasswordConfirm, setRegisterPasswordConfirm] = useState('')

  const [loginFieldErrors, setLoginFieldErrors] = useState<FieldErrors>({})
  const [registerFieldErrors, setRegisterFieldErrors] = useState<FieldErrors>({})
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const isLoggingOutRef = useRef(false)

  const isLoggedIn = useMemo(() => Boolean(token), [token])

  /**
   * 画面上部に表示する認証系メッセージをクリアする。
   */
  const resetMessages = () => {
    setErrorMessage('')
    setSuccessMessage('')
  }

  /**
   * ログインフォームの指定フィールドのエラーをクリアする。
   */
  const clearLoginFieldError = (field: string) => {
    setLoginFieldErrors((current) => {
      if (!current[field]) {
        return current
      }

      const next = { ...current }
      delete next[field]
      return next
    })
  }

  /**
   * 登録フォームの指定フィールドのエラーをクリアする。
   */
  const clearRegisterFieldError = (field: string) => {
    setRegisterFieldErrors((current) => {
      if (!current[field]) {
        return current
      }

      const next = { ...current }
      delete next[field]
      return next
    })
  }

  /**
   * ログイン画面へ切り替える。
   */
  const showLogin = () => {
    resetMessages()
    setLoginFieldErrors({})
    setRegisterFieldErrors({})
    setMode('login')
    if (window.location.pathname !== '/login') {
      go('/login', true)
    }
  }

  /**
   * ユーザー登録画面へ切り替える。
   */
  const showRegister = () => {
    resetMessages()
    setLoginFieldErrors({})
    setRegisterFieldErrors({})
    setMode('register')
    if (window.location.pathname !== '/signup') {
      go('/signup', true)
    }
  }

  /**
   * ログインフォームをクライアント側で検証する。
   */
  const validateLoginForm = (): FieldErrors => {
    const next: FieldErrors = {}
    const trimmedLoginEmail = loginEmail.trim()

    if (!trimmedLoginEmail) {
      next.email = 'メールアドレスを入力してください。'
    } else if (!EMAIL_PATTERN.test(trimmedLoginEmail)) {
      next.email = 'メールアドレスの形式が不正です。'
    }

    if (!loginPassword.trim()) {
      next.password = 'パスワードを入力してください。'
    }

    return next
  }

  /**
   * 登録フォームをクライアント側で検証する。
   */
  const validateRegisterForm = (): FieldErrors => {
    const next: FieldErrors = {}
    const trimmedRegisterEmail = registerEmail.trim()

    if (!registerName.trim()) {
      next.name = '名前を入力してください。'
    }

    if (!trimmedRegisterEmail) {
      next.email = 'メールアドレスを入力してください。'
    } else if (!EMAIL_PATTERN.test(trimmedRegisterEmail)) {
      next.email = 'メールアドレスの形式が不正です。'
    }

    if (registerPassword.length < 8) {
      next.password = 'パスワードは8文字以上で入力してください。'
    }

    if (!registerPasswordConfirm.trim()) {
      next.passwordConfirm = '確認用パスワードを入力してください。'
    } else if (registerPassword !== registerPasswordConfirm) {
      next.passwordConfirm = '確認用パスワードが一致しません。'
    }

    return next
  }

  /**
   * ログインAPIを呼び出し、成功時は認証情報を保存して保護ページへ遷移する。
   */
  const handleLogin = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    resetMessages()
    setLoginFieldErrors({})

    const localFieldErrors = validateLoginForm()
    if (hasFieldErrors(localFieldErrors)) {
      setLoginFieldErrors(localFieldErrors)
      setErrorMessage('入力内容を確認してください。')
      return
    }

    setIsSubmitting(true)
    try {
      const result = await login({ email: loginEmail.trim(), password: loginPassword })
      const resolvedToken = result.token ?? ''
      const resolvedUserName = result.user?.name?.trim() || result.user?.email?.trim() || loginEmail.trim()

      if (!resolvedToken) {
        setErrorMessage('トークンの取得に失敗しました。')
        return
      }

      saveAuthToken(resolvedToken)
      saveUserDisplayName(resolvedUserName)
      if (typeof result.user?.id === 'number') {
        saveUserId(result.user.id)
        setCurrentUserId(result.user.id)
      } else {
        clearUserId()
        setCurrentUserId(null)
      }
      setToken(resolvedToken)
      setCurrentUserLabel(resolvedUserName)
      setLoginFieldErrors({})
      const redirectPath = consumePostLoginRedirectPath()
      // 認証切れなどで保存していた遷移先があれば、ログイン後に元の保護ページへ戻す。
      go(redirectPath || '/tasks', true)
    } catch (error) {
      const apiFieldErrors = extractFieldErrorsFromApiError(error)
      if (hasFieldErrors(apiFieldErrors)) {
        setLoginFieldErrors(apiFieldErrors)
      }
      setErrorMessage(resolveUserMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  /**
   * 登録APIを呼び出し、成功時はログイン画面へ戻して登録済みメールアドレスを引き継ぐ。
   */
  const handleRegister = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    resetMessages()
    setRegisterFieldErrors({})

    const localFieldErrors = validateRegisterForm()
    if (hasFieldErrors(localFieldErrors)) {
      setRegisterFieldErrors(localFieldErrors)
      setErrorMessage('入力内容を確認してください。')
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
      go('/login', true)
      setLoginFieldErrors({})
      setRegisterFieldErrors({})
      setLoginEmail(registeredEmail)
      setLoginPassword('')
      saveUserDisplayName(result.name ?? registerName.trim() ?? registeredEmail)
      setRegisterName('')
      setRegisterEmail('')
      setRegisterPassword('')
      setRegisterPasswordConfirm('')
    } catch (error) {
      const apiFieldErrors = extractFieldErrorsFromApiError(error)
      if (hasFieldErrors(apiFieldErrors)) {
        setRegisterFieldErrors(apiFieldErrors)
      }
      setErrorMessage(resolveUserMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  /**
   * 保存済み認証情報と画面上の認証状態を破棄し、ログイン画面へ戻す。
   */
  const handleLogout = () => {
    isLoggingOutRef.current = true
    clearAuthToken()
    clearUserId()
    clearUserDisplayName()
    clearPostLoginRedirectPath()
    setToken('')
    setCurrentUserId(null)
    setCurrentUserLabel('')
    setLoginPassword('')
    setLoginFieldErrors({})
    setRegisterFieldErrors({})
    resetMessages()
    go('/login', true)
  }

  useEffect(() => {
    /**
     * APIクライアントが通知する401イベントを受け取り、再ログイン導線へ切り替える。
     */
    const handleUnauthorized = () => {
      const currentPath = window.location.pathname
      if (isProtectedPath(currentPath)) {
        // 再ログイン後に元のページへ戻せるよう、保護ページだけ退避する。
        savePostLoginRedirectPath(currentPath)
      }
      clearUserDisplayName()
      clearUserId()
      setToken('')
      setCurrentUserId(null)
      setCurrentUserLabel('')
      setLoginPassword('')
      setLoginFieldErrors({})
      setRegisterFieldErrors({})
      setErrorMessage('認証期限が切れたため、再度ログインしてください。')
      setSuccessMessage('')
      setMode('login')
      onUnauthorized?.()
      go('/login', true)
    }

    window.addEventListener(UNAUTHORIZED_EVENT, handleUnauthorized)
    return () => {
      window.removeEventListener(UNAUTHORIZED_EVENT, handleUnauthorized)
    }
  }, [go, onUnauthorized])

  useEffect(() => {
    /**
     * 現在の認証状態とURLを同期し、保護ページへの未認証アクセスをログイン画面へ寄せる。
     */
    const syncAuthRoute = () => {
      const currentPath = window.location.pathname

      if (isLoggingOutRef.current) {
        setMode('login')
        if (currentPath !== '/login') {
          go('/login', true)
          return
        }
        isLoggingOutRef.current = false
        return
      }

      if (!isLoggedIn && isProtectedPath(currentPath)) {
        // 直接URL入力や戻る操作で保護ページに来た場合も、ログイン後の戻り先として保存する。
        savePostLoginRedirectPath(currentPath)
        setMode('login')
        if (currentPath !== '/login') {
          go('/login', true)
        }
        return
      }

      if (!isLoggedIn) {
        setMode(resolveAuthMode(currentPath))
        return
      }

      if (currentPath === '/login' || currentPath === '/signup' || currentPath === '/') {
        const redirectPath = consumePostLoginRedirectPath()
        // ログイン済みユーザーには認証画面を見せず、作業画面へ戻す。
        go(redirectPath || '/tasks', true)
      }
    }

    syncAuthRoute()
    window.addEventListener('popstate', syncAuthRoute)
    return () => {
      window.removeEventListener('popstate', syncAuthRoute)
    }
  }, [go, isLoggedIn])

  return {
    mode,
    isLoggedIn,
    currentUserLabel,
    currentUserId,
    errorMessage,
    successMessage,
    isSubmitting,
    loginForm: {
      email: loginEmail,
      password: loginPassword,
      fieldErrors: loginFieldErrors,
      onEmailChange: (value: string) => {
        setLoginEmail(value)
        clearLoginFieldError('email')
      },
      onPasswordChange: (value: string) => {
        setLoginPassword(value)
        clearLoginFieldError('password')
      },
      onSubmit: handleLogin,
      onShowRegister: showRegister,
    },
    registerForm: {
      name: registerName,
      email: registerEmail,
      password: registerPassword,
      passwordConfirm: registerPasswordConfirm,
      fieldErrors: registerFieldErrors,
      onNameChange: (value: string) => {
        setRegisterName(value)
        clearRegisterFieldError('name')
      },
      onEmailChange: (value: string) => {
        setRegisterEmail(value)
        clearRegisterFieldError('email')
      },
      onPasswordChange: (value: string) => {
        setRegisterPassword(value)
        clearRegisterFieldError('password')
      },
      onPasswordConfirmChange: (value: string) => {
        setRegisterPasswordConfirm(value)
        clearRegisterFieldError('passwordConfirm')
      },
      onSubmit: handleRegister,
      onShowLogin: showLogin,
    },
    actions: {
      resetMessages,
      showLogin,
      showRegister,
      handleLogout,
      setErrorMessage,
      setSuccessMessage,
    },
  }
}
