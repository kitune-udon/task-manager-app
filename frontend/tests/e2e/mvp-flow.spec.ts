import { expect, test } from '@playwright/test'
import { createE2eUser, createTaskTitle, registerAndLogin, registerUser } from './helpers'

test('ログイン画面は検証用の初期値を表示しない', async ({ page }) => {
  await page.goto('/login')

  await expect(page.getByLabel(/^メールアドレス/)).toHaveValue('')
  await expect(page.getByLabel(/^パスワード(?!確認)/)).toHaveValue('')
})

test('ログイン失敗はログイン画面内で扱われる', async ({ page }) => {
  const user = createE2eUser()

  await registerUser(page, user)
  await page.getByLabel(/^メールアドレス/).fill(user.email)
  await page.getByLabel(/^パスワード(?!確認)/).fill('wrong-password123')
  await page.getByRole('button', { name: 'ログイン' }).click()

  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByText('認証に失敗しました')).toBeVisible()
  await expect(page.getByText('認証期限が切れたため、再度ログインしてください。')).toHaveCount(0)
})

test('タスクの作成から更新と削除まで画面操作で完了できる', async ({ page }) => {
  const user = createE2eUser()
  const createdTitle = createTaskTitle('playwright-created-task')
  const updatedTitle = `${createdTitle}-updated`

  await registerAndLogin(page, user)

  await page.locator('.content-header').getByRole('button', { name: 'タスク作成' }).click()
  await expect(page).toHaveURL(/\/tasks\/new$/)

  await page.getByLabel('タイトル').fill(createdTitle)
  await page.getByLabel('説明').fill('Playwright から作成したタスクです。')
  await page.getByLabel('期限').fill('2026-12-31')

  const assigneeSelect = page.getByLabel('担当者')
  const assigneeCount = await assigneeSelect.locator('option').count()
  if (assigneeCount > 1) {
    await assigneeSelect.selectOption({ index: 1 })
  }

  await page.getByRole('button', { name: 'タスクを作成' }).click()

  await expect(page).toHaveURL(/\/tasks$/)
  await expect(page.getByText('タスクを作成しました。')).toBeVisible()

  const createdRow = page.locator('tbody tr').filter({ hasText: createdTitle })
  await expect(createdRow).toBeVisible()
  await createdRow.getByRole('button', { name: '詳細' }).click()

  await expect(page).toHaveURL(/\/tasks\/\d+$/)
  await expect(page.getByText(createdTitle)).toBeVisible()

  await page.getByRole('button', { name: '編集' }).click()
  await expect(page).toHaveURL(/\/tasks\/\d+\/edit$/)

  await page.getByLabel('タイトル').fill(updatedTitle)
  await page.getByLabel('ステータス').selectOption('DOING')
  await page.getByLabel('優先度').selectOption('HIGH')
  await page.getByRole('button', { name: '更新する' }).click()

  await expect(page).toHaveURL(/\/tasks\/\d+$/)
  await expect(page.getByText('タスクを更新しました。')).toBeVisible()
  await expect(page.getByText(updatedTitle)).toBeVisible()
  await expect(page.locator('.task-detail-meta-item').filter({ hasText: 'ステータス' })).toContainText('DOING')
  await expect(page.locator('.task-detail-meta-item').filter({ hasText: '優先度' })).toContainText('HIGH')

  page.once('dialog', (dialog) => dialog.accept())
  await page.getByRole('button', { name: '削除' }).click()

  await expect(page).toHaveURL(/\/tasks$/)
  await expect(page.getByText('タスクを削除しました。')).toBeVisible()
  await expect(page.locator('tbody tr').filter({ hasText: updatedTitle })).toHaveCount(0)
})

test('セッション切れ時はログイン画面へ戻される', async ({ page }) => {
  const user = createE2eUser()

  await registerAndLogin(page, user)

  await page.evaluate(() => {
    window.localStorage.setItem('authToken', 'invalid-token')
  })
  await page.reload()

  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByText('認証期限が切れたため、再度ログインしてください。')).toBeVisible()
})
