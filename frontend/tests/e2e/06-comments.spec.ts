import { expect, test, type Page } from '@playwright/test'
import { createCommentViaApi, createE2eUser, createTaskTitle, createTaskViaApi, getCurrentUserId, registerAndLogin } from './helpers'

async function createTaskAndOpenDetail(page: Page, title: string) {
  const currentUserId = await getCurrentUserId(page)
  const task = await createTaskViaApi(page, title, currentUserId)
  await page.goto(`/tasks/${task.id}`)
  await expect(page.getByRole('heading', { name: 'タスク詳細' })).toBeVisible()
  return task
}

test('CMT-C-01: コメントを投稿できる', async ({ page }) => {
  const user = createE2eUser()
  const title = createTaskTitle('playwright-comment-create')

  await registerAndLogin(page, user)
  await createTaskAndOpenDetail(page, title)

  await page.getByPlaceholder('コメントを追加する...').fill('Playwright comment')
  await page.getByRole('button', { name: '投稿' }).click()

  await expect(page.getByText('Playwright comment')).toBeVisible()
})

test('CMT-C-02: 投稿成功後にコメント一覧へ反映される', async ({ page }) => {
  const user = createE2eUser()
  const title = createTaskTitle('playwright-comment-reflect')

  await registerAndLogin(page, user)
  await createTaskAndOpenDetail(page, title)

  await page.getByPlaceholder('コメントを追加する...').fill('Reflected comment')
  await page.getByRole('button', { name: '投稿' }).click()

  const comment = page.locator('.comment-item').filter({ hasText: 'Reflected comment' })
  await expect(comment).toBeVisible()
  await expect(comment).toContainText(user.name)
})

test('CMT-U-01: コメント投稿者本人がコメントを更新できる', async ({ page }) => {
  const user = createE2eUser()
  const title = createTaskTitle('playwright-comment-update')

  await registerAndLogin(page, user)
  const task = await createTaskAndOpenDetail(page, title)
  await createCommentViaApi(page, task.id, 'Before edit comment')
  await page.reload()

  const comment = page.locator('.comment-item').filter({ hasText: 'Before edit comment' })
  await expect(comment).toBeVisible()
  await comment.getByRole('button', { name: '編集' }).click()
  await comment.locator('textarea').fill('After edit comment')
  await page.getByRole('button', { name: '保存' }).click()

  await expect(page.getByText('After edit comment')).toBeVisible()
  await expect(page.getByText('Before edit comment')).toHaveCount(0)
})

test('CMT-D-01: コメント投稿者本人がコメントを削除できる', async ({ page }) => {
  const user = createE2eUser()
  const title = createTaskTitle('playwright-comment-delete')

  await registerAndLogin(page, user)
  const task = await createTaskAndOpenDetail(page, title)
  await createCommentViaApi(page, task.id, 'Delete target comment')
  await page.reload()

  const comment = page.locator('.comment-item').filter({ hasText: 'Delete target comment' })
  await expect(comment).toBeVisible()
  page.once('dialog', (dialog) => dialog.accept())
  await comment.getByRole('button', { name: '削除' }).click()

  await expect(comment).toHaveCount(0)
})

test('CMT-D-05: 削除後のコメントはコメント一覧に表示されない', async ({ page }) => {
  const user = createE2eUser()
  const title = createTaskTitle('playwright-comment-delete-hidden')

  await registerAndLogin(page, user)
  const task = await createTaskAndOpenDetail(page, title)
  await createCommentViaApi(page, task.id, 'Hidden after delete comment')
  await page.reload()

  const comment = page.locator('.comment-item').filter({ hasText: 'Hidden after delete comment' })
  page.once('dialog', (dialog) => dialog.accept())
  await comment.getByRole('button', { name: '削除' }).click()

  await expect(page.getByText('Hidden after delete comment')).toHaveCount(0)
})
