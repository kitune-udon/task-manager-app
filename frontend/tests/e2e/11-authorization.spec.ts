import { expect, test } from '@playwright/test'
import {
  addTeamMemberViaApi,
  createE2eUser,
  createTaskTitle,
  createTaskViaApi,
  createTeamViaApi,
  getAssignableUserIdByEmail,
  loginUser,
  logout,
  registerAndLogin,
  registerUser,
} from './helpers'

test('AUTH-01 AUTH-09: 参照権限のないタスク詳細・履歴は表示されない', async ({ page }) => {
  const assignee = createE2eUser()
  const creator = createE2eUser()
  const outsider = createE2eUser()
  const title = createTaskTitle('playwright-auth-hidden-task')

  await registerUser(page, assignee)
  await registerUser(page, outsider)
  await registerAndLogin(page, creator)

  const assigneeId = await getAssignableUserIdByEmail(page, assignee.email)
  const team = await createTeamViaApi(page, createTaskTitle('playwright-auth-team'))
  await addTeamMemberViaApi(page, Number(team.id), assigneeId)
  const task = await createTaskViaApi(page, title, assigneeId, { teamId: Number(team.id) })

  await logout(page)
  await loginUser(page, outsider)
  await page.goto(`/tasks/${task.id}`)

  await expect(page.getByText('この操作を行う権限がありません。')).toBeVisible()
  await expect(page.getByText(title)).toHaveCount(0)
  await expect(page.getByRole('button', { name: 'コメント', exact: true })).toHaveCount(0)
  await expect(page.getByRole('button', { name: '履歴', exact: true })).toHaveCount(0)
})
