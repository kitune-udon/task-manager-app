import { expect, test, type Page } from '@playwright/test'
import { createE2eUser, createTaskTitle, createTaskViaApi, getCurrentUserId, registerAndLogin, uploadAttachmentViaApi } from './helpers'

async function createTaskAndOpenDetail(page: Page, title: string) {
  const currentUserId = await getCurrentUserId(page)
  const task = await createTaskViaApi(page, title, currentUserId)
  await page.goto(`/tasks/${task.id}`)
  await expect(page.getByRole('heading', { name: 'タスク詳細' })).toBeVisible()
  return task
}

test('ATT-C-01: 許可ファイルをアップロードできる', async ({ page }) => {
  const user = createE2eUser()
  const title = createTaskTitle('playwright-attachment-upload')
  const fileName = 'playwright-note.txt'

  await registerAndLogin(page, user)
  await createTaskAndOpenDetail(page, title)

  await page.locator('input[type="file"]').setInputFiles({
    name: fileName,
    mimeType: 'text/plain',
    buffer: Buffer.from('Playwright attachment'),
  })

  await expect(page.locator('.attachment-row').filter({ hasText: fileName })).toBeVisible()
})

test('ATT-G-01: ファイル名クリックで添付ファイルをダウンロードできる', async ({ page }) => {
  const user = createE2eUser()
  const title = createTaskTitle('playwright-attachment-download')
  const fileName = 'download-target.txt'

  await registerAndLogin(page, user)
  const task = await createTaskAndOpenDetail(page, title)
  await uploadAttachmentViaApi(page, task.id, fileName)
  await page.reload()

  const downloadPromise = page.waitForEvent('download')
  await page.getByRole('button', { name: fileName }).click()
  const download = await downloadPromise

  expect(download.suggestedFilename()).toBe(fileName)
})

test('ATT-D-01: 添付登録者本人が添付ファイルを削除できる', async ({ page }) => {
  const user = createE2eUser()
  const title = createTaskTitle('playwright-attachment-delete')
  const fileName = 'delete-target.txt'

  await registerAndLogin(page, user)
  const task = await createTaskAndOpenDetail(page, title)
  await uploadAttachmentViaApi(page, task.id, fileName)
  await page.reload()

  const attachmentRow = page.locator('.attachment-row').filter({ hasText: fileName })
  await expect(attachmentRow).toBeVisible()
  page.once('dialog', (dialog) => dialog.accept())
  await attachmentRow.getByRole('button', { name: '削除' }).click()

  await expect(attachmentRow).toHaveCount(0)
})

test('ATT-D-05: 削除後の添付ファイルは一覧へ表示されない', async ({ page }) => {
  const user = createE2eUser()
  const title = createTaskTitle('playwright-attachment-hidden')
  const fileName = 'hidden-after-delete.txt'

  await registerAndLogin(page, user)
  const task = await createTaskAndOpenDetail(page, title)
  await uploadAttachmentViaApi(page, task.id, fileName)
  await page.reload()

  const attachmentRow = page.locator('.attachment-row').filter({ hasText: fileName })
  page.once('dialog', (dialog) => dialog.accept())
  await attachmentRow.getByRole('button', { name: '削除' }).click()

  await expect(page.getByText(fileName)).toHaveCount(0)
})
