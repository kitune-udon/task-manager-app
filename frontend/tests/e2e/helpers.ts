import { expect, type Page } from '@playwright/test'

export type E2eUser = {
  name: string
  email: string
  password: string
}

function buildUniqueSuffix() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

export function createE2eUser(): E2eUser {
  const suffix = buildUniqueSuffix()

  return {
    name: `playwright-user-${suffix}`,
    email: `playwright-${suffix}@example.com`,
    password: 'password123',
  }
}

export function createTaskTitle(prefix: string) {
  return `${prefix}-${buildUniqueSuffix()}`
}

export async function registerUser(page: Page, user: E2eUser) {
  await page.goto('/signup')
  await page.getByLabel(/^ユーザー名/).fill(user.name)
  await page.getByLabel(/^メールアドレス/).fill(user.email)
  await page.getByLabel(/^パスワード(?!確認)/).fill(user.password)
  await page.getByLabel(/^パスワード確認/).fill(user.password)
  await page.getByRole('button', { name: '登録する' }).click()

  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByText('登録に成功しました。ログインしてください。')).toBeVisible()
}

export async function loginUser(page: Page, user: E2eUser) {
  await page.goto('/login')
  await page.getByLabel(/^メールアドレス/).fill(user.email)
  await page.getByLabel(/^パスワード(?!確認)/).fill(user.password)
  await page.getByRole('button', { name: 'ログイン' }).click()

  await expect(page).toHaveURL(/\/tasks$/)
  await expect(page.getByRole('heading', { name: 'タスク一覧' })).toBeVisible()
}

export async function registerAndLogin(page: Page, user: E2eUser) {
  await registerUser(page, user)
  await loginUser(page, user)
}
