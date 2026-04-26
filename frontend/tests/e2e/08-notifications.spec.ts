import { expect, test } from '@playwright/test'
import {
  createCommentViaApi,
  createE2eUser,
  createTaskTitle,
  createTaskViaApi,
  deleteTaskViaApi,
  addTeamMemberViaApi,
  ensureTeamViaApi,
  getAssignableUserIdByEmail,
  loginUser,
  logout,
  openNotificationRow,
  openNotifications,
  prepareUnreadCommentNotification,
  registerAndLogin,
  registerUser,
  removeTeamMemberByUserIdViaApi,
  updateTaskAssigneeViaApi,
} from './helpers'

test('NTF-L-06: 通知0件時は空状態メッセージを表示する', async ({ page }) => {
  const user = createE2eUser()

  await registerAndLogin(page, user)
  await openNotifications(page)

  await expect(page.getByText('通知はありません。')).toBeVisible()
})

test('NTF-N-01 NTF-N-02: 未読通知クリックは既読化後に関連タスク詳細へ遷移する', async ({ page }) => {
  const { recipient, task, taskTitle } = await prepareUnreadCommentNotification(page, {
    titlePrefix: 'playwright-notification-open',
  })
  const calls: string[] = []
  let resolveReadRequestStarted!: () => void
  let releaseReadRequest!: () => void
  const readRequestStarted = new Promise<void>((resolve) => {
    resolveReadRequestStarted = resolve
  })
  await page.route('**/api/notifications/*/read', async (route) => {
    calls.push('read')
    resolveReadRequestStarted()
    await new Promise<void>((release) => {
      releaseReadRequest = release
    })
    await route.continue()
  })

  await page.route(`**/api/tasks/${task.id}`, async (route) => {
    calls.push('task')
    await route.continue()
  })

  await logout(page)
  await loginUser(page, recipient)
  await openNotifications(page)

  await openNotificationRow(page, taskTitle)
  await readRequestStarted
  expect(calls).toEqual(['read'])

  releaseReadRequest()

  await expect(page).toHaveURL(new RegExp(`/tasks/${task.id}\\?from=notifications&teamId=${task.teamId}$`))
  expect(calls.slice(0, 2)).toEqual(['read', 'task'])
  expect(calls.filter((call) => call === 'task').length).toBeGreaterThan(0)

  await page.getByRole('button', { name: '一覧へ戻る' }).click()
  await expect(page).toHaveURL(/\/notifications$/)
})

test('NTF-N-03 NTF-N-05: 関連タスク削除済み通知は一覧に留まり既読状態を維持する', async ({ page }) => {
  const { recipient, task, taskTitle } = await prepareUnreadCommentNotification(page, {
    titlePrefix: 'playwright-notification-deleted-task',
  })

  await deleteTaskViaApi(page, task.id)
  await logout(page)
  await loginUser(page, recipient)
  await openNotifications(page)

  const row = await openNotificationRow(page, taskTitle)

  await expect(page).toHaveURL(/\/notifications$/)
  await expect(page.getByText('アクセスできないか、削除されています')).toBeVisible()
  await expect(row.locator('.badge-muted')).toHaveText('既読')
  await expect(row.getByRole('button', { name: '既読にする' })).toBeDisabled()
  await expect(page.locator('.nav-badge')).toHaveCount(0)
})

test('NTF-N-07 TTEAM-N-05: 関連タスクの参照権限を失った通知は一覧に留まりメッセージを表示する', async ({ page }) => {
  const recipient = createE2eUser()
  const replacementAssignee = createE2eUser()
  const actor = createE2eUser()
  const taskTitle = createTaskTitle('playwright-notification-forbidden-task')

  await registerUser(page, recipient)
  await registerUser(page, replacementAssignee)
  await registerAndLogin(page, actor)

  const recipientId = await getAssignableUserIdByEmail(page, recipient.email)
  const replacementAssigneeId = await getAssignableUserIdByEmail(page, replacementAssignee.email)
  const team = await ensureTeamViaApi(page, createTaskTitle('playwright-notification-team'))
  await addTeamMemberViaApi(page, Number(team.id), recipientId)
  await addTeamMemberViaApi(page, Number(team.id), replacementAssigneeId)
  const task = await createTaskViaApi(page, taskTitle, recipientId, { teamId: Number(team.id) })
  await createCommentViaApi(page, task.id, 'permission-lost-notification')
  await updateTaskAssigneeViaApi(page, task, replacementAssigneeId)
  await removeTeamMemberByUserIdViaApi(page, Number(team.id), recipientId)

  await logout(page)
  await loginUser(page, recipient)
  await openNotifications(page)

  await openNotificationRow(page, taskTitle)

  await expect(page).toHaveURL(/\/notifications$/)
  await expect(page.getByText('アクセスできないか、削除されています')).toBeVisible()
})
