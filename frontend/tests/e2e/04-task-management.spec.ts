import { expect, test, type Page } from '@playwright/test'
import {
  createE2eUser,
  createTaskTitle,
  createTaskViaApi,
  createTaskViaUi,
  deleteTaskViaApi,
  registerAndLogin,
} from './helpers'

async function openTaskDetailFromList(page: Page, title: string) {
  const row = page.locator('tbody tr').filter({ hasText: title })
  await expect(row).toBeVisible()
  await row.getByRole('button', { name: '詳細' }).click()
  await expect(page).toHaveURL(/\/tasks\/\d+$/)
}

test('TSK-L-04: ステータス条件でタスク一覧を絞り込める', async ({ page }) => {
  const user = createE2eUser()
  const todoTitle = createTaskTitle('playwright-filter-status-todo')
  const doneTitle = createTaskTitle('playwright-filter-status-done')

  await registerAndLogin(page, user)
  await createTaskViaUi(page, { title: todoTitle, status: 'TODO', priority: 'LOW' })
  await createTaskViaUi(page, { title: doneTitle, status: 'DONE', priority: 'HIGH' })

  await page.getByLabel('ステータス').selectOption('DONE')

  await expect(page.locator('tbody tr').filter({ hasText: doneTitle })).toHaveCount(1)
  await expect(page.locator('tbody tr').filter({ hasText: todoTitle })).toHaveCount(0)
})

test('TSK-L-05: 優先度条件でタスク一覧を絞り込める', async ({ page }) => {
  const user = createE2eUser()
  const lowTitle = createTaskTitle('playwright-filter-priority-low')
  const highTitle = createTaskTitle('playwright-filter-priority-high')

  await registerAndLogin(page, user)
  await createTaskViaUi(page, { title: lowTitle, status: 'TODO', priority: 'LOW' })
  await createTaskViaUi(page, { title: highTitle, status: 'DONE', priority: 'HIGH' })

  await page.getByLabel('優先度').selectOption('HIGH')

  await expect(page.locator('tbody tr').filter({ hasText: highTitle })).toHaveCount(1)
  await expect(page.locator('tbody tr').filter({ hasText: lowTitle })).toHaveCount(0)
})

test('TSK-L-06: 条件一致0件時は空状態メッセージを表示する', async ({ page }) => {
  const user = createE2eUser()

  await registerAndLogin(page, user)

  await expect(page.getByText('条件に一致するタスクはありません。')).toBeVisible()
  await expect(page.locator('.summary-card').filter({ hasText: '表示件数' })).toContainText('0')
})

test('TSK-L-07: 一覧の対象行からタスク詳細へ遷移できる', async ({ page }) => {
  const user = createE2eUser()
  const title = createTaskTitle('playwright-list-detail')

  await registerAndLogin(page, user)
  await createTaskViaUi(page, { title })

  await openTaskDetailFromList(page, title)
  await expect(page.getByRole('heading', { name: 'タスク詳細' })).toBeVisible()
})

test('TSK-L-08: タスク作成操作で作成画面へ遷移できる', async ({ page }) => {
  const user = createE2eUser()

  await registerAndLogin(page, user)
  await page.locator('.content-header').getByRole('button', { name: 'タスク作成' }).click()

  await expect(page).toHaveURL(/\/tasks\/new$/)
  await expect(page.getByRole('heading', { name: 'タスク作成' })).toBeVisible()
})

test('TSK-L-09: 再読込操作でタスク一覧を再取得できる', async ({ page }) => {
  const user = createE2eUser()
  const reloadedTitle = createTaskTitle('playwright-reload-task')

  await registerAndLogin(page, user)
  await expect(page.locator('tbody tr').filter({ hasText: reloadedTitle })).toHaveCount(0)

  await createTaskViaApi(page, reloadedTitle)
  await expect(page.locator('tbody tr').filter({ hasText: reloadedTitle })).toHaveCount(0)

  await page.locator('.content-header').getByRole('button', { name: '再読込' }).click()
  await expect(page.locator('tbody tr').filter({ hasText: reloadedTitle })).toBeVisible()
})

test('TSK-C-01: 必須項目を満たすとタスク作成できる', async ({ page }) => {
  const user = createE2eUser()
  const title = createTaskTitle('playwright-create-task')

  await registerAndLogin(page, user)
  await createTaskViaUi(page, { title })

  await expect(page.locator('tbody tr').filter({ hasText: title })).toBeVisible()
})

test('TSK-C-02: タスク作成成功後にタスク一覧へ戻る', async ({ page }) => {
  const user = createE2eUser()
  const title = createTaskTitle('playwright-create-return')

  await registerAndLogin(page, user)
  await createTaskViaUi(page, { title })

  await expect(page).toHaveURL(/\/tasks$/)
  await expect(page.getByText('タスクを作成しました。')).toBeVisible()
})

test('TSK-G-02: タスク詳細に主要項目が表示される', async ({ page }) => {
  const user = createE2eUser()
  const title = createTaskTitle('playwright-detail-fields')

  await registerAndLogin(page, user)
  await createTaskViaUi(page, {
    title,
    description: 'Playwright detail description',
    status: 'TODO',
    priority: 'MEDIUM',
    dueDate: '2026-12-31',
  })

  await openTaskDetailFromList(page, title)

  await expect(page.getByText(title)).toBeVisible()
  await expect(page.getByText('Playwright detail description')).toBeVisible()
  await expect(page.locator('.task-detail-side-item').filter({ hasText: 'ステータス' })).toContainText('TODO')
  await expect(page.locator('.task-detail-side-item').filter({ hasText: '優先度' })).toContainText('MEDIUM')
  await expect(page.locator('.task-detail-side-item').filter({ hasText: '作成者' })).toContainText(user.name)
  await expect(page.locator('.task-detail-side-item').filter({ hasText: '期限' })).toContainText('2026/12/31')
})

test('TSK-G-03: 更新権限がある場合は表示モードと編集モードを切り替えられる', async ({ page }) => {
  const user = createE2eUser()
  const title = createTaskTitle('playwright-edit-mode')

  await registerAndLogin(page, user)
  await createTaskViaUi(page, { title })
  await openTaskDetailFromList(page, title)

  await page.getByRole('button', { name: '編集' }).click()
  await expect(page.getByRole('button', { name: '保存' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'キャンセル' })).toBeVisible()

  await page.getByRole('button', { name: 'キャンセル' }).click()
  await expect(page.getByRole('button', { name: '編集' })).toBeVisible()
})

test('TSK-G-04: 編集キャンセル時に変更前の内容へ戻る', async ({ page }) => {
  const user = createE2eUser()
  const originalTitle = createTaskTitle('playwright-cancel-task')
  const editedTitle = `${originalTitle}-edited`

  await registerAndLogin(page, user)
  await createTaskViaUi(page, { title: originalTitle, status: 'TODO', priority: 'LOW' })
  await openTaskDetailFromList(page, originalTitle)

  await page.getByRole('button', { name: '編集' }).click()
  await page.getByLabel('タイトル').fill(editedTitle)
  await page.getByLabel('ステータス').selectOption('DONE')
  await page.getByLabel('優先度').selectOption('HIGH')
  await page.getByRole('button', { name: 'キャンセル' }).click()

  await expect(page.getByText(originalTitle)).toBeVisible()
  await expect(page.getByText(editedTitle)).toHaveCount(0)
  await expect(page.locator('.task-detail-side-item').filter({ hasText: 'ステータス' })).toContainText('TODO')
  await expect(page.locator('.task-detail-side-item').filter({ hasText: '優先度' })).toContainText('LOW')
})

test('TSK-G-07: コメントタブと履歴タブを切り替えられる', async ({ page }) => {
  const user = createE2eUser()
  const title = createTaskTitle('playwright-detail-tab-switch')

  await registerAndLogin(page, user)
  await createTaskViaUi(page, { title })
  await openTaskDetailFromList(page, title)

  await expect(page.getByPlaceholder('コメントを追加する...')).toBeVisible()

  await page.getByRole('button', { name: '履歴', exact: true }).click()
  await expect(page.locator('.activity-item')).toBeVisible()

  await page.getByRole('button', { name: 'コメント', exact: true }).click()
  await expect(page.getByPlaceholder('コメントを追加する...')).toBeVisible()
})

test('TSK-U-01: 最新versionを指定したタスク更新が画面に反映される', async ({ page }) => {
  const user = createE2eUser()
  const originalTitle = createTaskTitle('playwright-update-task')
  const updatedTitle = `${originalTitle}-updated`

  await registerAndLogin(page, user)
  await createTaskViaUi(page, { title: originalTitle, status: 'TODO', priority: 'LOW' })
  await openTaskDetailFromList(page, originalTitle)

  await page.getByRole('button', { name: '編集' }).click()
  await page.getByLabel('タイトル').fill(updatedTitle)
  await page.getByLabel('ステータス').selectOption('DOING')
  await page.getByLabel('優先度').selectOption('HIGH')
  await page.getByRole('button', { name: '保存' }).click()

  await expect(page.getByText('タスクを更新しました。')).toBeVisible()
  await expect(page.getByText(updatedTitle)).toBeVisible()
  await expect(page.locator('.task-detail-side-item').filter({ hasText: 'ステータス' })).toContainText('DOING')
  await expect(page.locator('.task-detail-side-item').filter({ hasText: '優先度' })).toContainText('HIGH')
})

test('TSK-D-01: 削除権限を持つユーザーがタスクを削除できる', async ({ page }) => {
  const user = createE2eUser()
  const title = createTaskTitle('playwright-delete-task')

  await registerAndLogin(page, user)
  await createTaskViaUi(page, { title })
  await openTaskDetailFromList(page, title)

  page.once('dialog', (dialog) => dialog.accept())
  await page.locator('.content-header').getByRole('button', { name: '削除', exact: true }).click()

  await expect(page).toHaveURL(/\/tasks$/)
  await expect(page.getByText('タスクを削除しました。')).toBeVisible()
})

test('TSK-D-03: 削除済みタスクは一覧に表示されない', async ({ page }) => {
  const user = createE2eUser()
  const title = createTaskTitle('playwright-delete-hidden')

  await registerAndLogin(page, user)
  const task = await createTaskViaApi(page, title)
  await page.locator('.content-header').getByRole('button', { name: '再読込' }).click()
  await expect(page.locator('tbody tr').filter({ hasText: title })).toBeVisible()

  await deleteTaskViaApi(page, task.id)
  await page.locator('.content-header').getByRole('button', { name: '再読込' }).click()

  await expect(page.locator('tbody tr').filter({ hasText: title })).toHaveCount(0)
})
