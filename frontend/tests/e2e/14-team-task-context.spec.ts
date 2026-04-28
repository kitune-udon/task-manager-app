import { expect, test } from '@playwright/test'
import {
  addTeamMemberViaApi,
  createE2eUser,
  createTaskTitle,
  createTaskViaApi,
  createTeamViaApi,
  ensureTeamViaApi,
  getAssignableUserIdByEmail,
  registerAndLogin,
  registerUser,
} from './helpers'

test('TTEAM-X-01 TTEAM-X-03 TTEAM-X-06: サイドバーから横断一覧へ遷移し作成ボタンは表示されない', async ({ page }) => {
  const user = createE2eUser()

  await registerAndLogin(page, user)
  await page.locator('.sidebar').getByRole('button', { name: 'タスク一覧' }).click()

  await expect(page).toHaveURL(/\/tasks$/)
  await expect(page.getByText('表示対象:')).toBeVisible()
  await expect(page.getByText('所属全チーム', { exact: true })).toBeVisible()
  await expect(page.locator('.content-header').getByRole('button', { name: 'タスクを作成する' })).toHaveCount(0)
})

test('TTEAM-X-02 TTEAM-X-04 TTEAM-X-05: 横断一覧に所属全チームのタスクとチーム列/絞り込みが表示される', async ({ page }) => {
  const user = createE2eUser()
  const alphaTask = createTaskTitle('playwright-alpha-task')
  const betaTask = createTaskTitle('playwright-beta-task')

  await registerAndLogin(page, user)
  const alpha = await createTeamViaApi(page, createTaskTitle('playwright-alpha-team'))
  const beta = await createTeamViaApi(page, createTaskTitle('playwright-beta-team'))
  await createTaskViaApi(page, alphaTask, undefined, { teamId: Number(alpha.id) })
  await createTaskViaApi(page, betaTask, undefined, { teamId: Number(beta.id) })

  await page.goto('/tasks')

  await expect(page.getByRole('columnheader', { name: 'チーム' })).toBeVisible()
  await expect(page.getByLabel('チーム')).toBeVisible()
  await expect(page.locator('tbody tr').filter({ hasText: alphaTask })).toContainText(alpha.name)
  await expect(page.locator('tbody tr').filter({ hasText: betaTask })).toContainText(beta.name)

  await page.getByLabel('チーム').selectOption(String(alpha.id))
  await expect(page.locator('tbody tr').filter({ hasText: alphaTask })).toHaveCount(1)
  await expect(page.locator('tbody tr').filter({ hasText: betaTask })).toHaveCount(0)
})

test('TEAM-G-04 TTEAM-L-01 TTEAM-L-03: チーム詳細からteam文脈付き一覧へ遷移し詳細へ戻れる', async ({ page }) => {
  const user = createE2eUser()
  const teamName = createTaskTitle('playwright-context-team')

  await registerAndLogin(page, user)
  const team = await createTeamViaApi(page, teamName, 'Context team')

  await page.goto(`/teams/${team.id}`)
  await page.getByRole('button', { name: 'このチームのタスクを見る' }).click()

  await expect(page).toHaveURL(new RegExp(`/tasks\\?teamId=${team.id}$`))
  await expect(page.getByText('チーム:')).toBeVisible()
  await expect(page.getByText(teamName, { exact: true })).toBeVisible()
  await expect(page.locator('.content-header').getByRole('button', { name: 'タスクを作成する' })).toBeVisible()

  await page.getByRole('button', { name: 'チーム詳細へ戻る' }).click()
  await expect(page).toHaveURL(new RegExp(`/teams/${team.id}$`))
})

test('TTEAM-C-02 TTEAM-C-03 TTEAM-C-04: 作成画面はteam固定表示で担当者候補がteamメンバーに限定される', async ({ page }) => {
  const owner = createE2eUser()
  const member = createE2eUser()
  const outsider = createE2eUser()
  const teamName = createTaskTitle('playwright-create-context-team')

  await registerUser(page, member)
  await registerUser(page, outsider)
  await registerAndLogin(page, owner)
  const memberId = await getAssignableUserIdByEmail(page, member.email)
  const team = await createTeamViaApi(page, teamName, 'Create context')
  await addTeamMemberViaApi(page, Number(team.id), memberId, 'MEMBER')

  await page.goto(`/tasks?teamId=${team.id}`)
  await page.locator('.content-header').getByRole('button', { name: 'タスクを作成する' }).click()

  await expect(page).toHaveURL(new RegExp(`/tasks/new\\?teamId=${team.id}$`))
  await expect(page.locator('.inline-context-banner')).toContainText(teamName)
  await expect(page.getByLabel('チーム')).toHaveCount(0)

  const assigneeOptions = await page.getByLabel('担当者').locator('option').allTextContents()
  expect(assigneeOptions.join('\n')).toContain(member.email)
  expect(assigneeOptions.join('\n')).not.toContain(outsider.email)
})

test('TTEAM-C-01 TTEAM-C-10: team文脈固定で作成しteamId付き一覧へ戻る', async ({ page }) => {
  const user = createE2eUser()
  const title = createTaskTitle('playwright-create-context-task')

  await registerAndLogin(page, user)
  const team = await ensureTeamViaApi(page, createTaskTitle('playwright-create-return-team'))
  await page.goto(`/tasks?teamId=${team.id}`)
  await page.locator('.content-header').getByRole('button', { name: 'タスクを作成する' }).click()
  await page.getByLabel('タイトル').fill(title)
  await page.getByRole('button', { name: 'タスクを作成' }).click()

  await expect(page).toHaveURL(new RegExp(`/tasks\\?teamId=${team.id}$`))
  await expect(page.getByText('タスクを作成しました。')).toBeVisible()
  await expect(page.locator('tbody tr').filter({ hasText: title })).toBeVisible()
})

test('TTEAM-C-05: teamIdなし直アクセスはteamsへ戻す案内を表示する', async ({ page }) => {
  const user = createE2eUser()

  await registerAndLogin(page, user)
  await page.goto('/tasks/new')

  await expect(page.getByText('タスクを作成するチームを選択してください')).toBeVisible()
  await expect(page).toHaveURL(/\/teams$/)
})

test('TTEAM-U-01 TTEAM-U-02 TTEAM-U-06: 編集画面でteamは固定表示されqueryが維持される', async ({ page }) => {
  const user = createE2eUser()
  const title = createTaskTitle('playwright-edit-context-task')

  await registerAndLogin(page, user)
  const team = await ensureTeamViaApi(page, createTaskTitle('playwright-edit-context-team'))
  const task = await createTaskViaApi(page, title, undefined, { teamId: Number(team.id) })

  await page.goto(`/tasks?teamId=${team.id}`)
  await page.locator('tbody tr').filter({ hasText: title }).getByRole('button', { name: title }).click()
  await expect(page).toHaveURL(new RegExp(`/tasks/${task.id}\\?teamId=${team.id}$`))
  await page.getByRole('button', { name: '編集' }).click()

  await expect(page).toHaveURL(new RegExp(`/tasks/${task.id}/edit\\?teamId=${team.id}$`))
  await expect(page.locator('.task-detail-side-grid').getByText('チーム', { exact: true })).toBeVisible()
  await expect(page.locator('.task-detail-side-grid').getByText(team.name, { exact: true })).toBeVisible()
  await expect(page.getByLabel('チーム')).toHaveCount(0)

  await page.getByRole('button', { name: 'キャンセル' }).click()
  await expect(page).toHaveURL(new RegExp(`/tasks/${task.id}\\?teamId=${team.id}$`))
})

test('TTEAM-X-07 TTEAM-L-06: 一覧から詳細へ進んでも戻り先が維持される', async ({ page }) => {
  const user = createE2eUser()
  const globalTitle = createTaskTitle('playwright-global-return')
  const teamTitle = createTaskTitle('playwright-team-return')

  await registerAndLogin(page, user)
  const team = await ensureTeamViaApi(page, createTaskTitle('playwright-return-team'))
  await createTaskViaApi(page, globalTitle, undefined, { teamId: Number(team.id) })
  await page.goto('/tasks')
  await page.locator('tbody tr').filter({ hasText: globalTitle }).getByRole('button', { name: globalTitle }).click()
  await page.getByRole('button', { name: '一覧へ戻る' }).click()
  await expect(page).toHaveURL(/\/tasks$/)

  await createTaskViaApi(page, teamTitle, undefined, { teamId: Number(team.id) })
  await page.goto(`/tasks?teamId=${team.id}`)
  await page.locator('tbody tr').filter({ hasText: teamTitle }).getByRole('button', { name: teamTitle }).click()
  await page.getByRole('button', { name: '一覧へ戻る' }).click()
  await expect(page).toHaveURL(new RegExp(`/tasks\\?teamId=${team.id}$`))
})
