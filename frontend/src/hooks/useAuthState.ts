import { useEffect, useMemo, useState, type FormEvent } from 'react'
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
  clearUserDisplayName,
  consumePostLoginRedirectPath,
  getAuthToken,
  getUserDisplayName,
  isProtectedPath,
  saveAuthToken,
  savePostLoginRedirectPath,
  saveUserDisplayName,
} from '../lib/authStorage'

export type AuthMode = 'login' | 'register'

const EMAIL_PATTERN = /^\S+@\S+\.\S+$/

type Params = {
  go: (path: string, replace?: boolean) => void
  onUnauthorized?: () => void
}

export function useAuthState({ go, onUnauthorized }: Params) {
  const resolveAuthMode = (pathname: string): AuthMode => (pathname === '/signup' ? 'register' : 'login')

  const [mode, setMode] = useState<AuthMode>(() => resolveAuthMode(window.location.pathname))
  const [token, setToken] = useState<string>(() => getAuthToken())
  const [currentUserLabel, setCurrentUserLabel] = useState<string>(() => getUserDisplayName())

  const [loginEmail, setLoginEmail] = useState('tasktester@example.com')
  const [loginPassword, setLoginPassword] = useState('password123')
  const [registerName, setRegisterName] = useState('')
  const [registerEmail, setRegisterEmail] = useState('')
  const [registerPassword, setRegisterPassword] = useState('')
  const [registerPasswordConfirm, setRegisterPasswordConfirm] = useState('')

  const [loginFieldErrors, setLoginFieldErrors] = useState<FieldErrors>({})
  const [registerFieldErrors, setRegisterFieldErrors] = useState<FieldErrors>({})
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const isLoggedIn = useMemo(() => Boolean(token), [token])

  const resetMessages = () => {
    setErrorMessage('')
    setSuccessMessage('')
  }

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

  const showLogin = () => {
    resetMessages()
    setLoginFieldErrors({})
    setRegisterFieldErrors({})
    setMode('login')
    if (window.location.pathname !== '/login') {
      go('/login', true)
    }
  }

  const showRegister = () => {
    resetMessages()
    setLoginFieldErrors({})
    setRegisterFieldErrors({})
    setMode('register')
    if (window.location.pathname !== '/signup') {
      go('/signup', true)
    }
  }

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
      setToken(resolvedToken)
      setCurrentUserLabel(resolvedUserName)
      setLoginFieldErrors({})
      const redirectPath = consumePostLoginRedirectPath()
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

  const handleLogout = () => {
    clearAuthToken()
    clearUserDisplayName()
    clearPostLoginRedirectPath()
    setToken('')
    setCurrentUserLabel('')
    setLoginPassword('')
    setLoginFieldErrors({})
    setRegisterFieldErrors({})
    resetMessages()
    go('/login', true)
  }

  useEffect(() => {
    const handleUnauthorized = () => {
      const currentPath = window.location.pathname
      if (isProtectedPath(currentPath)) {
        savePostLoginRedirectPath(currentPath)
      }
      clearUserDisplayName()
      setToken('')
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
    const syncAuthRoute = () => {
      const currentPath = window.location.pathname

      if (!isLoggedIn && isProtectedPath(currentPath)) {
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
