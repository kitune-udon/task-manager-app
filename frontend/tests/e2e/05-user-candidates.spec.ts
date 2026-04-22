import { expect, test } from '@playwright/test'
import { createE2eUser, createTaskTitle, registerAndLogin, registerUser } from './helpers'

test('USR-02: 担当者候補がタスク作成画面と詳細編集モードに表示される', async ({ page }) => {
  const assignee = createE2eUser()
  const creator = createE2eUser()
  const title = createTaskTitle('playwright-assignee-option')
  const assigneeOptionLabel = `${assignee.name} (${assignee.email})`

  await registerUser(page, assignee)
  await registerAndLogin(page, creator)

  await page.locator('.content-header').getByRole('button', { name: 'タスク作成' }).click()
  const createAssigneeSelect = page.getByLabel('担当者')
  await expect(createAssigneeSelect).toContainText(assignee.name)
  await createAssigneeSelect.selectOption({ label: assigneeOptionLabel })
  await page.getByLabel('タイトル').fill(title)
  await page.getByLabel('ステータス').selectOption('TODO')
  await page.getByLabel('優先度').selectOption('MEDIUM')
  await page.getByRole('button', { name: 'タスクを作成' }).click()

  await expect(page).toHaveURL(/\/tasks$/)
  await page.locator('tbody tr').filter({ hasText: title }).getByRole('button', { name: '詳細' }).click()
  await page.getByRole('button', { name: '編集' }).click()

  await expect(page.getByLabel('担当者')).toContainText(assignee.name)
})

test('USR-03: 担当者候補取得中は読み込み中表示が出る', async ({ page }) => {
  const user = createE2eUser()

  await page.route('http://localhost:8080/api/users', async (route) => {
    await new Promise((resolve) => setTimeout(resolve, 3_000))
    await route.continue()
  })

  await registerAndLogin(page, user)
  await page.locator('.content-header').getByRole('button', { name: 'タスク作成' }).click()

  await expect(page.getByText('担当者候補を読み込み中です...')).toBeVisible()
})
