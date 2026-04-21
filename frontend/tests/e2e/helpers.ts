import { expect, type Page } from '@playwright/test'

/**
 * E2Eテストで作成・ログインするユーザー情報。
 */
export type E2eUser = {
  name: string
  email: string
  password: string
}

/**
 * テストデータの衝突を避けるための一意suffixを生成する。
 */
function buildUniqueSuffix() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

/**
 * 登録テストで利用する一意なユーザー情報を生成する。
 */
export function createE2eUser(): E2eUser {
  const suffix = buildUniqueSuffix()

  return {
    name: `playwright-user-${suffix}`,
    email: `playwright-${suffix}@example.com`,
    password: 'password123',
  }
}

/**
 * E2Eテスト用の一意なタスクタイトルを生成する。
 */
export function createTaskTitle(prefix: string) {
  return `${prefix}-${buildUniqueSuffix()}`
}

/**
 * UIからユーザー登録を実行し、ログイン画面へ戻ることを確認する。
 */
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

/**
 * UIからログインを実行し、タスク一覧画面へ遷移することを確認する。
 */
export async function loginUser(page: Page, user: E2eUser) {
  await page.goto('/login')
  await page.getByLabel(/^メールアドレス/).fill(user.email)
  await page.getByLabel(/^パスワード(?!確認)/).fill(user.password)
  await page.getByRole('button', { name: 'ログイン' }).click()

  await expect(page).toHaveURL(/\/tasks$/)
  await expect(page.getByRole('heading', { name: 'タスク一覧' })).toBeVisible()
}

/**
 * ユーザー登録からログインまでをまとめて実行する。
 */
export async function registerAndLogin(page: Page, user: E2eUser) {
  await registerUser(page, user)
  await loginUser(page, user)
}
