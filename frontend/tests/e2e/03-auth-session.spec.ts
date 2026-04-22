import { expect, test } from '@playwright/test'
import { createE2eUser, logout, registerAndLogin, registerUser } from './helpers'

test('ログイン画面は検証用の初期値を表示しない', async ({ page }) => {
  await page.goto('/login')

  await expect(page.getByLabel(/^メールアドレス/)).toHaveValue('')
  await expect(page.getByLabel(/^パスワード(?!確認)/)).toHaveValue('')
})

test('LGN-02: ログイン成功後にJWTが保持される', async ({ page }) => {
  const user = createE2eUser()

  await registerAndLogin(page, user)

  await expect.poll(() => page.evaluate(() => window.localStorage.getItem('authToken'))).not.toBeNull()
  const token = await page.evaluate(() => window.localStorage.getItem('authToken'))
  expect(token?.length).toBeGreaterThan(20)
})

test('LGN-03: ログイン成功後に表示名が保持される', async ({ page }) => {
  const user = createE2eUser()

  await registerAndLogin(page, user)

  await expect(page.locator('.user-chip')).toHaveText(user.name)
  await expect.poll(() => page.evaluate(() => window.localStorage.getItem('userDisplayName'))).toBe(user.name)
})

test('LGN-05: ログイン失敗はログイン画面内で扱われる', async ({ page }) => {
  const user = createE2eUser()

  await registerUser(page, user)
  await page.getByLabel(/^メールアドレス/).fill(user.email)
  await page.getByLabel(/^パスワード(?!確認)/).fill('wrong-password123')
  await page.getByRole('button', { name: 'ログイン' }).click()

  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByText('認証に失敗しました')).toBeVisible()
  await expect(page.getByText('認証期限が切れたため、再度ログインしてください。')).toHaveCount(0)
})

test('LGN-06: 保護画面への直接アクセス後はログイン後に戻り先へ復元される', async ({ page }) => {
  const user = createE2eUser()

  await registerUser(page, user)
  await page.evaluate(() => window.localStorage.clear())
  await page.goto('/tasks/new')

  await page.getByLabel(/^メールアドレス/).fill(user.email)
  await page.getByLabel(/^パスワード(?!確認)/).fill(user.password)
  await page.getByRole('button', { name: 'ログイン' }).click()

  await expect(page).toHaveURL(/\/tasks\/new$/)
  await expect(page.getByRole('heading', { name: 'タスク作成' })).toBeVisible()
  await expect.poll(() => page.evaluate(() => window.localStorage.getItem('postLoginRedirectPath'))).toBeNull()
})

test('REG-01 REG-02: 必須項目を満たすと登録できログイン画面へ遷移する', async ({ page }) => {
  const user = createE2eUser()

  await registerUser(page, user)
})

test('REG-04: 登録画面はパスワード確認不一致を画面内で扱う', async ({ page }) => {
  const user = createE2eUser()

  await page.goto('/signup')
  await page.getByLabel(/^ユーザー名/).fill(user.name)
  await page.getByLabel(/^メールアドレス/).fill(user.email)
  await page.getByLabel(/^パスワード(?!確認)/).fill(user.password)
  await page.getByLabel(/^パスワード確認/).fill(`${user.password}-mismatch`)
  await page.getByRole('button', { name: '登録する' }).click()

  await expect(page).toHaveURL(/\/signup$/)
  await expect(page.getByText('確認用パスワードが一致しません。')).toBeVisible()
})

test('REG-06: 登録画面からログイン画面へ戻れる', async ({ page }) => {
  await page.goto('/signup')
  await page.getByRole('button', { name: 'ログインへ戻る' }).click()

  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByRole('button', { name: 'ログイン' })).toBeVisible()
})

test('SES-01: 未認証で保護画面へアクセスするとログイン画面へ遷移する', async ({ page }) => {
  await page.goto('/login')
  await page.evaluate(() => window.localStorage.clear())
  await page.goto('/tasks/new')

  await expect(page).toHaveURL(/\/login$/)
  await expect.poll(() => page.evaluate(() => window.localStorage.getItem('postLoginRedirectPath'))).toBe('/tasks/new')
})

test('SES-02: 保護APIの401で認証情報を破棄してログイン画面へ戻る', async ({ page }) => {
  const user = createE2eUser()

  await registerAndLogin(page, user)
  await page.evaluate(() => {
    window.localStorage.setItem('authToken', 'invalid-token')
  })
  await page.reload()

  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByText('認証期限が切れたため、再度ログインしてください。')).toBeVisible()
  await expect.poll(() => page.evaluate(() => window.localStorage.getItem('authToken'))).toBeNull()
})

test('SES-03: ログイン済みで認証画面へアクセスするとタスク一覧へ戻される', async ({ page }) => {
  const user = createE2eUser()

  await registerAndLogin(page, user)

  await page.goto('/login')
  await expect(page).toHaveURL(/\/tasks$/)

  await page.goto('/signup')
  await expect(page).toHaveURL(/\/tasks$/)
  await expect(page.getByRole('heading', { name: 'タスク一覧' })).toBeVisible()
})

test('SES-04: ログアウト時に認証関連stateと保存情報がクリアされる', async ({ page }) => {
  const user = createE2eUser()

  await registerAndLogin(page, user)
  await page.getByRole('button', { name: 'ログアウト' }).click()

  await expect
    .poll(() =>
      page.evaluate(() => ({
        token: window.localStorage.getItem('authToken'),
        displayName: window.localStorage.getItem('userDisplayName'),
        userId: window.localStorage.getItem('userId'),
        redirectPath: window.localStorage.getItem('postLoginRedirectPath'),
      })),
    )
    .toEqual({
      token: null,
      displayName: null,
      userId: null,
      redirectPath: null,
    })
})

test('SES-05: ログアウト時にログイン画面へ遷移する', async ({ page }) => {
  const user = createE2eUser()

  await registerAndLogin(page, user)
  await logout(page)
})

test('SES-06: ログアウト後に通知ポーリングが停止する', async ({ page }) => {
  const user = createE2eUser()
  let unreadCountRequests = 0

  await page.clock.install()
  await page.route('**/api/notifications/unread-count', async (route) => {
    unreadCountRequests += 1
    await route.fulfill({ json: { unreadCount: 0 } })
  })

  await registerAndLogin(page, user)
  await expect.poll(() => unreadCountRequests).toBeGreaterThan(0)

  await logout(page)
  const afterLogoutRequests = unreadCountRequests
  await page.clock.fastForward(180_000)

  expect(unreadCountRequests).toBe(afterLogoutRequests)
})
