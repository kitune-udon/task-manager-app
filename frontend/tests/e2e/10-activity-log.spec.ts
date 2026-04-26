import { expect, test } from '@playwright/test'
import {
  addTeamMemberViaApi,
  createE2eUser,
  createTaskTitle,
  createTaskViaApi,
  createTeamViaApi,
  getAssignableUserIdByEmail,
  getCurrentUserId,
  registerAndLogin,
  registerUser,
} from './helpers'

test('ACT-12: タスク詳細画面の履歴タブでアクティビティログを表示できる', async ({ page }) => {
  const user = createE2eUser()
  const title = createTaskTitle('playwright-activity-history')

  await registerAndLogin(page, user)
  const currentUserId = await getCurrentUserId(page)
  const task = await createTaskViaApi(page, title, currentUserId)

  await page.goto(`/tasks/${task.id}`)
  await page.getByRole('button', { name: '履歴', exact: true }).click()

  await expect(page.locator('.activity-item')).toHaveCount(1)
  await expect(page.getByText('タスク追加')).toBeVisible()
  await expect(page.getByText(`${user.name}さんがタスクを作成しました。`)).toBeVisible()
})

test('LOG-TEAM-07 LOG-TEAM-08: チーム管理操作はタスク履歴に混在しない', async ({ page }) => {
  const owner = createE2eUser()
  const member = createE2eUser()
  const title = createTaskTitle('playwright-team-log-isolation')

  await registerUser(page, member)
  await registerAndLogin(page, owner)
  const memberId = await getAssignableUserIdByEmail(page, member.email)
  const team = await createTeamViaApi(page, createTaskTitle('playwright-team-log-team'))
  await addTeamMemberViaApi(page, Number(team.id), memberId, 'MEMBER')
  const task = await createTaskViaApi(page, title, memberId, { teamId: Number(team.id) })

  await page.goto(`/tasks/${task.id}?teamId=${team.id}`)
  await page.getByRole('button', { name: '履歴', exact: true }).click()

  await expect(page.getByText('タスク追加')).toBeVisible()
  await expect(page.getByText('チーム作成')).toHaveCount(0)
  await expect(page.getByText('メンバー追加')).toHaveCount(0)
})
