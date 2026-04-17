import { expect, test, type Page } from '@playwright/test'
import { createE2eUser, createTaskTitle, registerAndLogin, registerUser } from './helpers'

async function selectFirstAssignableUserIfAvailable(page: Page) {
  const assigneeSelect = page.getByLabel('担当者')
  const assigneeCount = await assigneeSelect.locator('option').count()
  if (assigneeCount > 1) {
    await assigneeSelect.selectOption({ index: 1 })
  }
}

async function createTaskViaUi(
  page: Page,
  {
    title,
    status = 'TODO',
    priority = 'MEDIUM',
  }: {
    title: string
    status?: 'TODO' | 'DOING' | 'DONE'
    priority?: 'LOW' | 'MEDIUM' | 'HIGH'
  },
) {
  await page.locator('.content-header').getByRole('button', { name: 'タスク作成' }).click()
  await expect(page).toHaveURL(/\/tasks\/new$/)

  await page.getByLabel('タイトル').fill(title)
  await page.getByLabel('ステータス').selectOption(status)
  await page.getByLabel('優先度').selectOption(priority)
  await selectFirstAssignableUserIfAvailable(page)
  await page.getByRole('button', { name: 'タスクを作成' }).click()

  await expect(page).toHaveURL(/\/tasks$/)
  await expect(page.getByText('タスクを作成しました。')).toBeVisible()
}

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
  await selectFirstAssignableUserIfAvailable(page)

  await page.getByRole('button', { name: 'タスクを作成' }).click()

  await expect(page).toHaveURL(/\/tasks$/)
  await expect(page.getByText('タスクを作成しました。')).toBeVisible()

  const createdRow = page.locator('tbody tr').filter({ hasText: createdTitle })
  await expect(createdRow).toBeVisible()
  await createdRow.getByRole('button', { name: '詳細' }).click()

  await expect(page).toHaveURL(/\/tasks\/\d+$/)
  await expect(page.getByText(createdTitle)).toBeVisible()
  await expect(page.getByRole('button', { name: 'コメント', exact: true })).toBeVisible()

  await page.getByRole('button', { name: '編集' }).click()
  await expect(page).toHaveURL(/\/tasks\/\d+$/)
  await expect(page.getByRole('button', { name: '保存' })).toBeVisible()

  await page.getByLabel('タイトル').fill(updatedTitle)
  await page.getByLabel('ステータス').selectOption('DOING')
  await page.getByLabel('優先度').selectOption('HIGH')
  await page.getByRole('button', { name: '保存' }).click()

  await expect(page.getByText('タスクを更新しました。')).toBeVisible()
  await expect(page.getByText(updatedTitle)).toBeVisible()
  await expect(page.locator('.task-detail-side-item').filter({ hasText: 'ステータス' })).toContainText('DOING')
  await expect(page.locator('.task-detail-side-item').filter({ hasText: '優先度' })).toContainText('HIGH')

  await page.getByPlaceholder('コメントを入力してください').fill('Playwright comment')
  await page.getByRole('button', { name: '投稿' }).click()
  await expect(page.getByText('Playwright comment')).toBeVisible()

  await page.locator('input[type="file"]').setInputFiles({
    name: 'playwright-note.txt',
    mimeType: 'text/plain',
    buffer: Buffer.from('Playwright attachment'),
  })
  await expect(page.getByText('playwright-note.txt')).toBeVisible()

  await page.getByRole('button', { name: '履歴', exact: true }).click()
  await expect(page.locator('.activity-item')).toHaveCount(4)

  await page.getByRole('button', { name: 'コメント', exact: true }).click()

  page.once('dialog', (dialog) => dialog.accept())
  await page.locator('.content-header').getByRole('button', { name: '削除', exact: true }).click()

  await expect(page).toHaveURL(/\/tasks$/)
  await expect(page.getByText('タスクを削除しました。')).toBeVisible()
  await expect(page.locator('tbody tr').filter({ hasText: updatedTitle })).toHaveCount(0)
})

test('フィルタとログアウトが従来通り動く', async ({ page }) => {
  const user = createE2eUser()
  const todoTitle = createTaskTitle('playwright-filter-todo')
  const doneTitle = createTaskTitle('playwright-filter-done')

  await registerAndLogin(page, user)
  await createTaskViaUi(page, { title: todoTitle, status: 'TODO', priority: 'LOW' })
  await createTaskViaUi(page, { title: doneTitle, status: 'DONE', priority: 'HIGH' })

  await page.getByLabel('ステータス').selectOption('DONE')
  await page.getByLabel('優先度').selectOption('HIGH')

  await expect(page.locator('tbody tr').filter({ hasText: doneTitle })).toHaveCount(1)
  await expect(page.locator('tbody tr').filter({ hasText: todoTitle })).toHaveCount(0)
  await expect(page.locator('.summary-card').filter({ hasText: '表示件数' })).toContainText('1')

  await page.getByRole('button', { name: 'ログアウト' }).click()

  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByRole('button', { name: 'ログイン' })).toBeVisible()
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
