import { expect, test, type Page } from '@playwright/test'
import {
  addTeamMemberViaApi,
  createE2eUser,
  createTaskTitle,
  createTeamViaApi,
  getAssignableUserIdByEmail,
  loginUser,
  logout,
  registerAndLogin,
  registerUser,
} from './helpers'

async function openTeams(page: Page) {
  await page.locator('.sidebar').getByRole('button', { name: 'チーム' }).click()
  await expect(page).toHaveURL(/\/teams$/)
}

test('TEAM-L-03: 所属チーム0件時は空状態メッセージを表示する', async ({ page }) => {
  const user = createE2eUser()

  await registerAndLogin(page, user)
  await openTeams(page)

  await expect(page.getByText('所属しているチームがありません')).toBeVisible()
  await expect(page.getByRole('button', { name: 'チームを作成する' }).first()).toBeVisible()
})

test('TEAM-C-01 TEAM-C-03: チームを作成でき詳細画面へ遷移する', async ({ page }) => {
  const user = createE2eUser()
  const teamName = createTaskTitle('playwright-created-team')

  await registerAndLogin(page, user)
  await openTeams(page)
  await page.getByRole('button', { name: 'チームを作成する' }).first().click()
  await page.getByLabel(/チーム名/).fill(teamName)
  await page.getByLabel('チーム説明').fill('Playwright team description')
  await page.getByRole('dialog').getByRole('button', { name: '作成する', exact: true }).click()

  await expect(page).toHaveURL(/\/teams\/\d+$/)
  await expect(page.getByText('チームを作成しました。')).toBeVisible()
  await expect(page.getByRole('heading', { name: teamName })).toBeVisible()
  await expect(page.getByText('Playwright team description')).toBeVisible()
  await expect(page.locator('.team-detail-inline-meta').getByText('OWNER', { exact: true })).toBeVisible()
  await expect(page.getByText('1名')).toBeVisible()
})

test('TEAM-L-01 TEAM-L-02 TEAM-L-06: 所属チーム一覧から詳細へ遷移できる', async ({ page }) => {
  const user = createE2eUser()
  const teamName = createTaskTitle('playwright-list-team')

  await registerAndLogin(page, user)
  await createTeamViaApi(page, teamName, 'Listed team')
  await openTeams(page)
  await page.getByRole('button', { name: '再読込' }).click()

  const card = page.locator('.team-card').filter({ hasText: teamName })
  await expect(card).toBeVisible()
  await expect(card).toContainText('オーナー')
  await card.getByRole('button', { name: '詳細' }).click()

  await expect(page).toHaveURL(/\/teams\/\d+$/)
  await expect(page.getByRole('heading', { name: teamName })).toBeVisible()
})

test('TEAM-M-01 TEAM-M-07 TEAM-M-12: メンバー追加、ロール変更、削除ができる', async ({ page }) => {
  const owner = createE2eUser()
  const member = createE2eUser()
  const teamName = createTaskTitle('playwright-member-team')

  await registerUser(page, member)
  await registerAndLogin(page, owner)
  const memberId = await getAssignableUserIdByEmail(page, member.email)
  const team = await createTeamViaApi(page, teamName, 'Member operations')

  await page.goto(`/teams/${team.id}`)
  await page.getByRole('button', { name: 'メンバーを追加' }).click()
  await page.getByLabel('ユーザー').selectOption(String(memberId))
  await page.getByLabel('ロール').selectOption('MEMBER')
  await page.getByRole('button', { name: '追加する' }).click()
  await expect(page.getByText('メンバーを追加しました。')).toBeVisible()
  await expect(page.locator('tbody tr').filter({ hasText: member.email })).toContainText('MEMBER')

  const row = page.locator('tbody tr').filter({ hasText: member.email })
  await row.getByRole('button', { name: 'ロール変更' }).click()
  await page.getByLabel('変更後ロール').selectOption('ADMIN')
  await page.getByRole('button', { name: '変更する' }).click()
  await expect(page.getByText('ロールを変更しました。')).toBeVisible()
  await expect(row).toContainText('ADMIN')

  await row.getByRole('button', { name: '削除' }).click()
  await page.getByRole('button', { name: '削除する' }).click()
  await expect(page.getByText('メンバーを削除しました。')).toBeVisible()
  await expect(page.locator('tbody tr').filter({ hasText: member.email })).toHaveCount(0)
})

test('AUTH-TEAM-12: ADMINは自分自身をチームから外せる', async ({ page }) => {
  const owner = createE2eUser()
  const admin = createE2eUser()
  const teamName = createTaskTitle('playwright-admin-self-remove')

  await registerUser(page, admin)
  await registerAndLogin(page, owner)
  const adminId = await getAssignableUserIdByEmail(page, admin.email)
  const team = await createTeamViaApi(page, teamName, 'Admin self remove')
  await addTeamMemberViaApi(page, Number(team.id), adminId, 'ADMIN')

  await logout(page)
  await loginUser(page, admin)
  await page.goto(`/teams/${team.id}`)
  await page.getByRole('button', { name: 'チームから外れる' }).click()
  await page.getByRole('dialog').getByRole('button', { name: 'チームから外れる' }).click()

  await expect(page).toHaveURL(/\/teams$/)
  await expect(page.getByText('チームから外れました')).toBeVisible()
})

test('TEAM-G-07 UI-TEAM-01 UI-TEAM-02: MEMBERには管理ボタンが表示されない', async ({ page }) => {
  const owner = createE2eUser()
  const member = createE2eUser()
  const teamName = createTaskTitle('playwright-member-view')

  await registerUser(page, member)
  await registerAndLogin(page, owner)
  const memberId = await getAssignableUserIdByEmail(page, member.email)
  const team = await createTeamViaApi(page, teamName, 'Member view')
  await addTeamMemberViaApi(page, Number(team.id), memberId, 'MEMBER')

  await logout(page)
  await loginUser(page, member)
  await page.goto(`/teams/${team.id}`)

  await expect(page.getByRole('heading', { name: teamName })).toBeVisible()
  await expect(page.getByRole('button', { name: 'メンバーを追加' })).toHaveCount(0)
  await expect(page.getByRole('button', { name: 'ロール変更' })).toHaveCount(0)
  await expect(page.getByRole('button', { name: '削除' })).toHaveCount(0)
  await expect(page.getByRole('button', { name: 'このチームのタスクを見る' })).toBeVisible()
})

test('TEAM-G-08 TEAM-G-09: 候補取得失敗時もチーム詳細は表示継続する', async ({ page }) => {
  const user = createE2eUser()
  const teamName = createTaskTitle('playwright-available-failure')

  await registerAndLogin(page, user)
  const team = await createTeamViaApi(page, teamName, 'Available failure')
  await page.route('**/api/teams/*/available-users', async (route) => {
    await route.fulfill({
      status: 500,
      contentType: 'application/json',
      body: JSON.stringify({ status: 500, errorCode: 'ERR-SYS-999', message: 'Internal Server Error' }),
    })
  })

  await page.goto(`/teams/${team.id}`)
  await page.getByRole('button', { name: 'メンバーを追加' }).click()

  await expect(page.getByRole('heading', { name: teamName })).toBeVisible()
  await expect(page.getByText('システムエラーが発生しました。しばらくしてから再度お試しください。')).toBeVisible()
})
