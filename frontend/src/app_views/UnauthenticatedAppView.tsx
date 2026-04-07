import type { FormEvent } from 'react'
import type { AuthMode } from '../hooks/useAuthState'
import { LoginPage } from '../pages/LoginPage'
import { RegisterPage } from '../pages/RegisterPage'

type Props = {
  mode: AuthMode
  errorMessage: string
  successMessage: string
  isSubmitting: boolean
  loginForm: {
    email: string
    password: string
    fieldErrors: Record<string, string>
    onEmailChange: (value: string) => void
    onPasswordChange: (value: string) => void
    onSubmit: (event: FormEvent<HTMLFormElement>) => void
    onShowRegister: () => void
  }
  registerForm: {
    name: string
    email: string
    password: string
    passwordConfirm: string
    fieldErrors: Record<string, string>
    onNameChange: (value: string) => void
    onEmailChange: (value: string) => void
    onPasswordChange: (value: string) => void
    onPasswordConfirmChange: (value: string) => void
    onSubmit: (event: FormEvent<HTMLFormElement>) => void
    onShowLogin: () => void
  }
}

export function UnauthenticatedAppView({
  mode,
  errorMessage,
  successMessage,
  isSubmitting,
  loginForm,
  registerForm,
}: Props) {
  if (mode === 'login') {
    return (
      <LoginPage
        email={loginForm.email}
        password={loginForm.password}
        fieldErrors={loginForm.fieldErrors}
        errorMessage={errorMessage}
        successMessage={successMessage}
        isSubmitting={isSubmitting}
        onEmailChange={loginForm.onEmailChange}
        onPasswordChange={loginForm.onPasswordChange}
        onSubmit={loginForm.onSubmit}
        onShowRegister={loginForm.onShowRegister}
      />
    )
  }

  return (
    <RegisterPage
      name={registerForm.name}
      email={registerForm.email}
      password={registerForm.password}
      passwordConfirm={registerForm.passwordConfirm}
      fieldErrors={registerForm.fieldErrors}
      errorMessage={errorMessage}
      successMessage={successMessage}
      isSubmitting={isSubmitting}
      onNameChange={registerForm.onNameChange}
      onEmailChange={registerForm.onEmailChange}
      onPasswordChange={registerForm.onPasswordChange}
      onPasswordConfirmChange={registerForm.onPasswordConfirmChange}
      onSubmit={registerForm.onSubmit}
      onShowLogin={registerForm.onShowLogin}
    />
  )
}
