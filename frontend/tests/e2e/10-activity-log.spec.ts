import { expect, test } from '@playwright/test'
import { createE2eUser, createTaskTitle, createTaskViaApi, getCurrentUserId, registerAndLogin } from './helpers'

test('ACT-12: タスク詳細画面の履歴タブでアクティビティログを表示できる', async ({ page }) => {
  const user = createE2eUser()
  const title = createTaskTitle('playwright-activity-history')

  await registerAndLogin(page, user)
  const currentUserId = await getCurrentUserId(page)
  const task = await createTaskViaApi(page, title, currentUserId)

  await page.goto(`/tasks/${task.id}`)
  await page.getByRole('button', { name: '履歴', exact: true }).click()

  await expect(page.locator('.activity-item')).toHaveCount(1)
  await expect(page.getByText('タスク作成')).toBeVisible()
  await expect(page.getByText(`${user.name}さんがタスクを作成しました。`)).toBeVisible()
})
