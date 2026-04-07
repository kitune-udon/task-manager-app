import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { login, register, extractApiErrorMessage } from '../lib/authApi'
import { UNAUTHORIZED_EVENT } from '../lib/apiClient'
import {
  clearAuthToken,
  clearUserDisplayName,
  getAuthToken,
  getUserDisplayName,
  saveAuthToken,
  saveUserDisplayName,
} from '../lib/authStorage'

export type AuthMode = 'login' | 'register'

type Params = {
  go: (path: string, replace?: boolean) => void
  onUnauthorized?: () => void
}

export function useAuthState({ go, onUnauthorized }: Params) {
  const [mode, setMode] = useState<AuthMode>('login')
  const [token, setToken] = useState<string>(() => getAuthToken())
  const [currentUserLabel, setCurrentUserLabel] = useState<string>(() => getUserDisplayName())

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

  const resetMessages = () => {
    setErrorMessage('')
    setSuccessMessage('')
  }

  const showLogin = () => {
    resetMessages()
    setMode('login')
  }

  const showRegister = () => {
    resetMessages()
    setMode('register')
  }

  const validateRegisterForm = () => {
    if (!registerName.trim()) return '名前を入力してください。'
    if (!registerEmail.trim()) return 'メールアドレスを入力してください。'
    if (!/^\S+@\S+\.\S+$/.test(registerEmail)) return 'メールアドレスの形式が不正です。'
    if (registerPassword.length < 8) return 'パスワードは8文字以上で入力してください。'
    if (registerPassword !== registerPasswordConfirm) return '確認用パスワードが一致しません。'
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
      const result = await login({ email: loginEmail.trim(), password: loginPassword })
      const resolvedToken = result.token ?? ''

      if (!resolvedToken) {
        setErrorMessage('トークンの取得に失敗しました。')
        return
      }

      saveAuthToken(resolvedToken)
      saveUserDisplayName(loginEmail.trim())
      setToken(resolvedToken)
      setCurrentUserLabel(loginEmail.trim())
      go('/tasks', true)
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
      saveUserDisplayName(result.name ?? registerName.trim() ?? registeredEmail)
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
    clearUserDisplayName()
    setToken('')
    setCurrentUserLabel('')
    setLoginPassword('')
    resetMessages()
    go('/', true)
  }

  useEffect(() => {
    const handleUnauthorized = () => {
      clearUserDisplayName()
      setToken('')
      setCurrentUserLabel('')
      setLoginPassword('')
      setErrorMessage('認証期限が切れたため、再度ログインしてください。')
      setSuccessMessage('')
      setMode('login')
      onUnauthorized?.()
      go('/', true)
    }

    window.addEventListener(UNAUTHORIZED_EVENT, handleUnauthorized)
    return () => {
      window.removeEventListener(UNAUTHORIZED_EVENT, handleUnauthorized)
    }
  }, [go, onUnauthorized])

  useEffect(() => {
    if (isLoggedIn && window.location.pathname === '/') {
      go('/tasks', true)
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
      onEmailChange: setLoginEmail,
      onPasswordChange: setLoginPassword,
      onSubmit: handleLogin,
      onShowRegister: showRegister,
    },
    registerForm: {
      name: registerName,
      email: registerEmail,
      password: registerPassword,
      passwordConfirm: registerPasswordConfirm,
      onNameChange: setRegisterName,
      onEmailChange: setRegisterEmail,
      onPasswordChange: setRegisterPassword,
      onPasswordConfirmChange: setRegisterPasswordConfirm,
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
