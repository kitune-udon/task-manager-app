import { expect, test } from '@playwright/test'
import {
  createCommentViaApi,
  createE2eUser,
  createTaskTitle,
  createTaskViaApi,
  getAssignableUserIdByEmail,
  loginUser,
  logout,
  openNotifications,
  prepareUnreadCommentNotification,
  registerAndLogin,
  registerUser,
} from './helpers'

test('NTF-P-01: 認証後レイアウト表示時に未読件数を取得する', async ({ page }) => {
  const user = createE2eUser()
  let unreadCountRequests = 0

  await page.route('**/api/notifications/unread-count', async (route) => {
    unreadCountRequests += 1
    await route.fulfill({ json: { unreadCount: 0 } })
  })

  await registerAndLogin(page, user)

  await expect.poll(() => unreadCountRequests).toBeGreaterThan(0)
})

test('NTF-P-02: ログイン直後に未読件数を取得する', async ({ page }) => {
  const user = createE2eUser()
  let unreadCountRequests = 0

  await page.route('**/api/notifications/unread-count', async (route) => {
    unreadCountRequests += 1
    await route.fulfill({ json: { unreadCount: 0 } })
  })

  await registerUser(page, user)
  await page.getByLabel(/^メールアドレス/).fill(user.email)
  await page.getByLabel(/^パスワード(?!確認)/).fill(user.password)
  await page.getByRole('button', { name: 'ログイン' }).click()

  await expect(page).toHaveURL(/\/tasks$/)
  await expect.poll(() => unreadCountRequests).toBeGreaterThan(0)
})

test('NTF-P-03: 画面遷移時に未読件数を再取得する', async ({ page }) => {
  const user = createE2eUser()
  let unreadCountRequests = 0

  await page.route('**/api/notifications/unread-count', async (route) => {
    unreadCountRequests += 1
    await route.fulfill({ json: { unreadCount: 0 } })
  })

  await registerAndLogin(page, user)
  await expect.poll(() => unreadCountRequests).toBeGreaterThan(0)

  const afterLoginRequests = unreadCountRequests
  await page.locator('.sidebar').getByRole('button', { name: /通知/ }).click()

  await expect(page).toHaveURL(/\/notifications$/)
  await expect.poll(() => unreadCountRequests).toBeGreaterThan(afterLoginRequests)
})

test('NTF-P-04: 表示中タブでは60秒間隔で未読件数を再取得する', async ({ page }) => {
  const user = createE2eUser()
  let unreadCountRequests = 0

  await page.clock.install()
  await page.route('**/api/notifications/unread-count', async (route) => {
    unreadCountRequests += 1
    await route.fulfill({ json: { unreadCount: 0 } })
  })

  await registerAndLogin(page, user)
  await expect.poll(() => unreadCountRequests).toBeGreaterThan(0)

  const afterInitialRequests = unreadCountRequests
  await page.clock.fastForward(59_000)
  expect(unreadCountRequests).toBe(afterInitialRequests)

  await page.clock.fastForward(1_000)
  await expect.poll(() => unreadCountRequests).toBeGreaterThan(afterInitialRequests)
})

test('NTF-P-05: 個別既読化後に未読件数が再取得される', async ({ page }) => {
  const { recipient, taskTitle } = await prepareUnreadCommentNotification(page, {
    titlePrefix: 'playwright-notification-mark-read',
  })
  let unreadCountRequests = 0
  let releaseReadRequest!: () => void

  await page.route('**/api/notifications/unread-count', async (route) => {
    unreadCountRequests += 1
    await route.continue()
  })
  await page.route('**/api/notifications/*/read', async (route) => {
    await new Promise<void>((release) => {
      releaseReadRequest = release
    })
    await route.continue()
  })

  await logout(page)
  await loginUser(page, recipient)
  await openNotifications(page)

  await expect(page.locator('.nav-badge')).toHaveText('1')
  const row = page.locator('tbody tr').filter({ hasText: taskTitle })
  const requestsBeforeRead = unreadCountRequests

  await row.getByRole('button', { name: '既読にする' }).click()
  releaseReadRequest()

  await expect(page.locator('.nav-badge')).toHaveCount(0)
  await expect.poll(() => unreadCountRequests).toBeGreaterThan(requestsBeforeRead)
})

test('NTF-P-06: 一括既読化後に未読件数が再取得される', async ({ page }) => {
  const { recipient } = await prepareUnreadCommentNotification(page, {
    titlePrefix: 'playwright-notification-mark-all-read',
    commentContent: 'first unread notification',
  })
  const recipientId = await getAssignableUserIdByEmail(page, recipient.email)
  const secondTask = await createTaskViaApi(page, createTaskTitle('playwright-notification-mark-all-read-second'), recipientId)
  await createCommentViaApi(page, secondTask.id, 'second unread notification')

  let unreadCountRequests = 0
  let releaseReadAllRequest!: () => void

  await page.route('**/api/notifications/unread-count', async (route) => {
    unreadCountRequests += 1
    await route.continue()
  })
  await page.route('**/api/notifications/read-all', async (route) => {
    await new Promise<void>((release) => {
      releaseReadAllRequest = release
    })
    await route.continue()
  })

  await logout(page)
  await loginUser(page, recipient)
  await openNotifications(page)

  await expect(page.locator('.nav-badge')).toHaveText('2')
  const requestsBeforeReadAll = unreadCountRequests

  await page.locator('.content-header').getByRole('button', { name: '一括既読' }).click()
  releaseReadAllRequest()

  await expect(page.locator('.nav-badge')).toHaveCount(0)
  await expect.poll(() => unreadCountRequests).toBeGreaterThan(requestsBeforeReadAll)
})

test('NTF-P-07: 前回リクエスト中は次のポーリングを開始しない', async ({ page }) => {
  const user = createE2eUser()
  let unreadCountRequests = 0
  let releaseUnreadCount!: () => void

  await page.clock.install()
  await page.route('**/api/notifications/unread-count', async (route) => {
    unreadCountRequests += 1
    await new Promise<void>((release) => {
      releaseUnreadCount = release
    })
    await route.fulfill({ json: { unreadCount: 0 } })
  })

  await registerAndLogin(page, user)
  await expect.poll(() => unreadCountRequests).toBe(1)

  await page.clock.fastForward(120_000)
  expect(unreadCountRequests).toBe(1)

  releaseUnreadCount()
  await expect.poll(() => page.locator('.nav-badge').count()).toBe(0)
})

test('NTF-P-08: 未読1件以上でバッジ表示される', async ({ page }) => {
  const user = createE2eUser()

  await page.route('**/api/notifications/unread-count', async (route) => {
    await route.fulfill({ json: { unreadCount: 1 } })
  })

  await registerAndLogin(page, user)

  await expect(page.locator('.nav-badge')).toHaveText('1')
})

test('NTF-P-09: 未読0件でバッジ非表示となる', async ({ page }) => {
  const user = createE2eUser()

  await page.route('**/api/notifications/unread-count', async (route) => {
    await route.fulfill({ json: { unreadCount: 0 } })
  })

  await registerAndLogin(page, user)

  await expect(page.locator('.nav-badge')).toHaveCount(0)
})

test('NTF-P-10: 未読100件以上で99+表示となる', async ({ page }) => {
  const user = createE2eUser()

  await page.route('**/api/notifications/unread-count', async (route) => {
    await route.fulfill({ json: { unreadCount: 100 } })
  })

  await registerAndLogin(page, user)

  await expect(page.locator('.nav-badge')).toHaveText('99+')
})

test('NTF-P-11: 未読件数取得が401の場合はログイン画面へ遷移する', async ({ page }) => {
  const user = createE2eUser()

  await registerUser(page, user)
  await page.route('**/api/notifications/unread-count', async (route) => {
    await route.fulfill({
      status: 401,
      json: { errorCode: 'ERR-AUTH-003', message: 'Unauthorized' },
    })
  })

  await page.getByLabel(/^メールアドレス/).fill(user.email)
  await page.getByLabel(/^パスワード(?!確認)/).fill(user.password)
  await page.getByRole('button', { name: 'ログイン' }).click()

  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByText('認証期限が切れたため、再度ログインしてください。')).toBeVisible()
})

test('NTF-P-12: 未読件数取得の5xxは画面全体エラーにせず次回ポーリングで再試行する', async ({ page }) => {
  const user = createE2eUser()
  let requestCount = 0

  await page.clock.install()
  await page.route('**/api/notifications/unread-count', async (route) => {
    requestCount += 1
    if (requestCount === 1) {
      await route.fulfill({ status: 500, json: { errorCode: 'ERR-SYS-999', message: 'system error' } })
      return
    }

    await route.fulfill({ json: { unreadCount: 1 } })
  })

  await registerAndLogin(page, user)
  await expect.poll(() => requestCount).toBe(1)
  await expect(page.locator('.status-box.error-box')).toHaveCount(0)
  await expect(page.locator('.nav-badge')).toHaveCount(0)

  await page.clock.fastForward(60_000)

  await expect(page.locator('.nav-badge')).toHaveText('1')
})

test('NTF-P-13: タブ非アクティブ時はポーリング間隔を180秒に切り替える', async ({ page }) => {
  const user = createE2eUser()
  let unreadCountRequests = 0

  await page.clock.install()
  await page.route('**/api/notifications/unread-count', async (route) => {
    unreadCountRequests += 1
    await route.fulfill({ json: { unreadCount: 0 } })
  })

  await registerAndLogin(page, user)
  await expect.poll(() => unreadCountRequests).toBeGreaterThan(0)

  const afterInitialRequests = unreadCountRequests
  await page.evaluate(() => {
    Object.defineProperty(document, 'visibilityState', {
      configurable: true,
      get: () => 'hidden',
    })
    document.dispatchEvent(new Event('visibilitychange'))
  })
  await expect.poll(() => page.evaluate(() => document.visibilityState)).toBe('hidden')

  await page.clock.fastForward(60_000)
  expect(unreadCountRequests).toBe(afterInitialRequests)

  await page.clock.fastForward(120_000)
  await expect.poll(() => unreadCountRequests).toBeGreaterThan(afterInitialRequests)
})

test('NTF-P-14: ログアウト時にポーリングが停止する', async ({ page }) => {
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
