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

async function createTeamFromModal(page: Page, teamName: string, description: string) {
  await openTeams(page)
  await page.getByRole('button', { name: 'チームを作成する' }).first().click()
  await page.getByLabel(/チーム名/).fill(teamName)
  await page.getByLabel('チーム説明').fill(description)
  await page.getByRole('dialog').getByRole('button', { name: '作成する', exact: true }).click()
}

async function prepareTeamWithCandidate(page: Page, description = 'Member operations') {
  const member = createE2eUser()
  const teamName = createTaskTitle('playwright-member-team')

  await registerUser(page, member)
  const owner = createE2eUser()
  await registerAndLogin(page, owner)
  const memberId = await getAssignableUserIdByEmail(page, member.email)
  const team = await createTeamViaApi(page, teamName, description)

  return { member, memberId, team }
}

async function routeAvailableUsersFailure(page: Page) {
  await page.route('**/api/teams/*/available-users', async (route) => {
    await route.fulfill({
      status: 500,
      contentType: 'application/json',
      body: JSON.stringify({ status: 500, errorCode: 'ERR-SYS-999', message: 'Internal Server Error' }),
    })
  })
}

test('TEAM-L-03: 所属チーム0件時は空状態メッセージを表示する', async ({ page }) => {
  const user = createE2eUser()

  await registerAndLogin(page, user)
  await openTeams(page)

  await expect(page.getByText('所属しているチームがありません')).toBeVisible()
  await expect(page.getByRole('button', { name: 'チームを作成する' }).first()).toBeVisible()
})

test('TEAM-C-01: チームを作成できる', async ({ page }) => {
  const user = createE2eUser()
  const teamName = createTaskTitle('playwright-created-team')

  await registerAndLogin(page, user)
  await createTeamFromModal(page, teamName, 'Playwright team description')

  await expect(page.getByText('チームを作成しました。')).toBeVisible()
  await expect(page.getByRole('heading', { name: teamName })).toBeVisible()
  await expect(page.getByText('Playwright team description')).toBeVisible()
  await expect(page.locator('.team-detail-inline-meta').getByText('OWNER', { exact: true })).toBeVisible()
  await expect(page.getByText('1名')).toBeVisible()
})

test('TEAM-C-03: チーム作成後に詳細画面へ遷移する', async ({ page }) => {
  const user = createE2eUser()
  const teamName = createTaskTitle('playwright-created-route-team')

  await registerAndLogin(page, user)
  await createTeamFromModal(page, teamName, 'Route check')

  await expect(page).toHaveURL(/\/teams\/\d+$/)
})

test('TEAM-L-01: 所属チーム一覧に自分のチームが表示される', async ({ page }) => {
  const user = createE2eUser()
  const teamName = createTaskTitle('playwright-list-team')

  await registerAndLogin(page, user)
  await createTeamViaApi(page, teamName, 'Listed team')
  await openTeams(page)
  await page.getByRole('button', { name: '再読込' }).click()

  const card = page.locator('.team-card').filter({ hasText: teamName })
  await expect(card).toBeVisible()
  await expect(card).toContainText('オーナー')
})

test('TEAM-L-06: チーム一覧から詳細へ遷移できる', async ({ page }) => {
  const user = createE2eUser()
  const teamName = createTaskTitle('playwright-list-detail-team')

  await registerAndLogin(page, user)
  await createTeamViaApi(page, teamName, 'Listed team detail')
  await openTeams(page)
  await page.getByRole('button', { name: '再読込' }).click()

  const card = page.locator('.team-card').filter({ hasText: teamName })
  await card.getByRole('button', { name: '詳細' }).click()

  await expect(page).toHaveURL(/\/teams\/\d+$/)
  await expect(page.getByRole('heading', { name: teamName })).toBeVisible()
})

test('TEAM-A-01: メンバーを追加できる', async ({ page }) => {
  const { memberId, team } = await prepareTeamWithCandidate(page)

  await page.goto(`/teams/${team.id}`)
  await page.getByRole('button', { name: 'メンバーを追加' }).click()
  await page.getByLabel('ユーザー').selectOption(String(memberId))
  await page.getByLabel('ロール').selectOption('MEMBER')
  await page.getByRole('button', { name: '追加する' }).click()

  await expect(page.getByText('メンバーを追加しました。')).toBeVisible()
})

test('TEAM-A-03: 追加成功後にメンバー一覧へ反映される', async ({ page }) => {
  const { member, memberId, team } = await prepareTeamWithCandidate(page, 'Member list reflection')

  await page.goto(`/teams/${team.id}`)
  await page.getByRole('button', { name: 'メンバーを追加' }).click()
  await page.getByLabel('ユーザー').selectOption(String(memberId))
  await page.getByLabel('ロール').selectOption('MEMBER')
  await page.getByRole('button', { name: '追加する' }).click()

  await expect(page.locator('tbody tr').filter({ hasText: member.email })).toContainText('MEMBER')
})

test('TEAM-R-01: OWNERがロール変更できる', async ({ page }) => {
  const { member, memberId, team } = await prepareTeamWithCandidate(page, 'Role change')
  await addTeamMemberViaApi(page, Number(team.id), memberId, 'MEMBER')

  await page.goto(`/teams/${team.id}`)
  const row = page.locator('tbody tr').filter({ hasText: member.email })
  await row.getByRole('button', { name: 'ロール変更' }).click()
  await page.getByLabel('変更後ロール').selectOption('ADMIN')
  await page.getByRole('button', { name: '変更する' }).click()

  await expect(page.getByText('ロールを変更しました。')).toBeVisible()
})

test('TEAM-R-02: ロール変更成功後にメンバー一覧へ反映される', async ({ page }) => {
  const { member, memberId, team } = await prepareTeamWithCandidate(page, 'Role list reflection')
  await addTeamMemberViaApi(page, Number(team.id), memberId, 'MEMBER')

  await page.goto(`/teams/${team.id}`)
  const row = page.locator('tbody tr').filter({ hasText: member.email })
  await row.getByRole('button', { name: 'ロール変更' }).click()
  await page.getByLabel('変更後ロール').selectOption('ADMIN')
  await page.getByRole('button', { name: '変更する' }).click()

  await expect(row).toContainText('ADMIN')
})

test('TEAM-D-01: メンバーを削除できる', async ({ page }) => {
  const { member, memberId, team } = await prepareTeamWithCandidate(page, 'Member remove')
  await addTeamMemberViaApi(page, Number(team.id), memberId, 'MEMBER')

  await page.goto(`/teams/${team.id}`)
  const row = page.locator('tbody tr').filter({ hasText: member.email })
  await row.getByRole('button', { name: '削除' }).click()
  await page.getByRole('button', { name: '削除する' }).click()

  await expect(page.getByText('メンバーを削除しました。')).toBeVisible()
})

test('TEAM-D-02: 削除成功後にメンバー一覧から除外される', async ({ page }) => {
  const { member, memberId, team } = await prepareTeamWithCandidate(page, 'Member remove list')
  await addTeamMemberViaApi(page, Number(team.id), memberId, 'MEMBER')

  await page.goto(`/teams/${team.id}`)
  const row = page.locator('tbody tr').filter({ hasText: member.email })
  await row.getByRole('button', { name: '削除' }).click()
  await page.getByRole('button', { name: '削除する' }).click()

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

test('TEAM-G-07: MEMBERには管理ボタンが表示されない', async ({ page }) => {
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

test('TEAM-G-08: 候補取得失敗時もチーム詳細は表示継続する', async ({ page }) => {
  const user = createE2eUser()
  const teamName = createTaskTitle('playwright-available-failure')

  await registerAndLogin(page, user)
  const team = await createTeamViaApi(page, teamName, 'Available failure')
  await routeAvailableUsersFailure(page)

  await page.goto(`/teams/${team.id}`)
  await page.getByRole('button', { name: 'メンバーを追加' }).click()

  await expect(page.getByRole('heading', { name: teamName })).toBeVisible()
})

test('TEAM-G-09: 候補取得失敗時はメンバー追加エリアにエラーと再読み込み導線を表示する', async ({ page }) => {
  const user = createE2eUser()
  const teamName = createTaskTitle('playwright-available-message')

  await registerAndLogin(page, user)
  const team = await createTeamViaApi(page, teamName, 'Available failure message')
  await routeAvailableUsersFailure(page)

  await page.goto(`/teams/${team.id}`)
  await page.getByRole('button', { name: 'メンバーを追加' }).click()

  await expect(page.getByText('システムエラーが発生しました。しばらくしてから再度お試しください。')).toBeVisible()
  await expect(page.getByRole('dialog').getByRole('button', { name: '再読み込み' })).toBeVisible()
})
