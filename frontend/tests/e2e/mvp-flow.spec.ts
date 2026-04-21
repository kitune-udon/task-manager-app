import { expect, test, type Page } from '@playwright/test'
import { createE2eUser, createTaskTitle, registerAndLogin, registerUser } from './helpers'

/**
 * 担当者候補が存在する環境では、先頭の実ユーザーを選択する。
 */
async function selectFirstAssignableUserIfAvailable(page: Page) {
  const assigneeSelect = page.getByLabel('担当者')
  const assigneeCount = await assigneeSelect.locator('option').count()
  if (assigneeCount > 1) {
    await assigneeSelect.selectOption({ index: 1 })
  }
}

/**
 * 一覧画面からタスク作成画面へ移動し、最小限の入力でタスクを作成する。
 */
async function createTaskViaUi(
  page: Page,
  {
    title,
    status = 'TODO',
    priority = 'MEDIUM',
  }: {
    title: string
    status?: 'TODO' | 'DOING' | 'DONE'
    priority?: 'LOW' | 'MEDIUM' | 'HIGH'
  },
) {
  await page.locator('.content-header').getByRole('button', { name: 'タスク作成' }).click()
  await expect(page).toHaveURL(/\/tasks\/new$/)

  await page.getByLabel('タイトル').fill(title)
  await page.getByLabel('ステータス').selectOption(status)
  await page.getByLabel('優先度').selectOption(priority)
  await selectFirstAssignableUserIfAvailable(page)
  await page.getByRole('button', { name: 'タスクを作成' }).click()

  await expect(page).toHaveURL(/\/tasks$/)
  await expect(page.getByText('タスクを作成しました。')).toBeVisible()
}

test('ログイン画面は検証用の初期値を表示しない', async ({ page }) => {
  await page.goto('/login')

  await expect(page.getByLabel(/^メールアドレス/)).toHaveValue('')
  await expect(page.getByLabel(/^パスワード(?!確認)/)).toHaveValue('')
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

test('LGN-02 LGN-03 SES-04: ログイン成功は認証情報を保存しログアウトで破棄する', async ({ page }) => {
  const user = createE2eUser()

  await registerAndLogin(page, user)

  const savedAuthState = await page.evaluate(() => ({
    token: window.localStorage.getItem('authToken'),
    displayName: window.localStorage.getItem('userDisplayName'),
    userId: window.localStorage.getItem('userId'),
    redirectPath: window.localStorage.getItem('postLoginRedirectPath'),
  }))

  expect(savedAuthState.token?.length).toBeGreaterThan(20)
  expect(savedAuthState.displayName).toBe(user.name)
  expect(Number(savedAuthState.userId)).toBeGreaterThan(0)
  expect(savedAuthState.redirectPath).toBeNull()
  await expect(page.locator('.user-chip')).toHaveText(user.name)

  await page.getByRole('button', { name: 'ログアウト' }).click()

  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByRole('button', { name: 'ログイン' })).toBeVisible()
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

test('SES-01 LGN-06: 保護画面への直接アクセス後はログイン後に戻り先へ復元される', async ({ page }) => {
  const user = createE2eUser()

  await registerUser(page, user)
  await page.evaluate(() => window.localStorage.clear())
  await page.goto('/tasks/new')

  await expect(page).toHaveURL(/\/login$/)
  await expect
    .poll(() => page.evaluate(() => window.localStorage.getItem('postLoginRedirectPath')))
    .toBe('/tasks/new')

  await page.getByLabel(/^メールアドレス/).fill(user.email)
  await page.getByLabel(/^パスワード(?!確認)/).fill(user.password)
  await page.getByRole('button', { name: 'ログイン' }).click()

  await expect(page).toHaveURL(/\/tasks\/new$/)
  await expect(page.getByRole('heading', { name: 'タスク作成' })).toBeVisible()
  await expect
    .poll(() => page.evaluate(() => window.localStorage.getItem('postLoginRedirectPath')))
    .toBeNull()
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

test('REG-04 REG-06: 登録画面はパスワード確認不一致を画面内で扱いログイン画面へ戻れる', async ({ page }) => {
  const user = createE2eUser()

  await page.goto('/signup')
  await page.getByLabel(/^ユーザー名/).fill(user.name)
  await page.getByLabel(/^メールアドレス/).fill(user.email)
  await page.getByLabel(/^パスワード(?!確認)/).fill(user.password)
  await page.getByLabel(/^パスワード確認/).fill(`${user.password}-mismatch`)
  await page.getByRole('button', { name: '登録する' }).click()

  await expect(page).toHaveURL(/\/signup$/)
  await expect(page.getByText('確認用パスワードが一致しません。')).toBeVisible()

  await page.getByRole('button', { name: 'ログインへ戻る' }).click()

  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByRole('button', { name: 'ログイン' })).toBeVisible()
})

test('TSK-C-01 TSK-C-02 TSK-G-02 TSK-G-03 TSK-U-01 CMT-C-01 CMT-C-02 ATT-C-01 ATT-G-01 ACT-12 UI-01 UI-02: タスクの作成から更新と削除まで画面操作で完了できる', async ({ page }) => {
  const user = createE2eUser()
  const createdTitle = createTaskTitle('playwright-created-task')
  const updatedTitle = `${createdTitle}-updated`

  await registerAndLogin(page, user)

  // 作成
  await page.locator('.content-header').getByRole('button', { name: 'タスク作成' }).click()
  await expect(page).toHaveURL(/\/tasks\/new$/)

  await page.getByLabel('タイトル').fill(createdTitle)
  await page.getByLabel('説明').fill('Playwright から作成したタスクです。')
  await page.getByLabel('期限').fill('2026-12-31')
  await selectFirstAssignableUserIfAvailable(page)

  await page.getByRole('button', { name: 'タスクを作成' }).click()

  await expect(page).toHaveURL(/\/tasks$/)
  await expect(page.getByText('タスクを作成しました。')).toBeVisible()

  const createdRow = page.locator('tbody tr').filter({ hasText: createdTitle })
  await expect(createdRow).toBeVisible()
  await createdRow.getByRole('button', { name: '詳細' }).click()

  await expect(page).toHaveURL(/\/tasks\/\d+$/)
  await expect(page.getByText(createdTitle)).toBeVisible()
  await expect(page.getByRole('button', { name: 'コメント', exact: true })).toBeVisible()

  // 更新
  await page.getByRole('button', { name: '編集' }).click()
  await expect(page).toHaveURL(/\/tasks\/\d+$/)
  await expect(page.getByRole('button', { name: '保存' })).toBeVisible()

  await page.getByLabel('タイトル').fill(updatedTitle)
  await page.getByLabel('ステータス').selectOption('DOING')
  await page.getByLabel('優先度').selectOption('HIGH')
  await page.getByRole('button', { name: '保存' }).click()

  await expect(page.getByText('タスクを更新しました。')).toBeVisible()
  await expect(page.getByText(updatedTitle)).toBeVisible()
  await expect(page.locator('.task-detail-side-item').filter({ hasText: 'ステータス' })).toContainText('DOING')
  await expect(page.locator('.task-detail-side-item').filter({ hasText: '優先度' })).toContainText('HIGH')

  // コメント・添付・履歴
  await page.getByPlaceholder('コメントを追加する...').fill('Playwright comment')
  await page.getByRole('button', { name: '投稿' }).click()
  await expect(page.getByText('Playwright comment')).toBeVisible()

  await page.locator('input[type="file"]').setInputFiles({
    name: 'playwright-note.txt',
    mimeType: 'text/plain',
    buffer: Buffer.from('Playwright attachment'),
  })
  await expect(page.getByText('playwright-note.txt')).toBeVisible()

  await page.getByRole('button', { name: '履歴', exact: true }).click()
  await expect(page.locator('.activity-item')).toHaveCount(4)

  await page.getByRole('button', { name: 'コメント', exact: true }).click()

  // 削除
  page.once('dialog', (dialog) => dialog.accept())
  await page.locator('.content-header').getByRole('button', { name: '削除', exact: true }).click()

  await expect(page).toHaveURL(/\/tasks$/)
  await expect(page.getByText('タスクを削除しました。')).toBeVisible()
  await expect(page.locator('tbody tr').filter({ hasText: updatedTitle })).toHaveCount(0)
})

test('TSK-L-04 TSK-L-05 TSK-L-08 SES-05: フィルタとログアウトが従来通り動く', async ({ page }) => {
  const user = createE2eUser()
  const todoTitle = createTaskTitle('playwright-filter-todo')
  const doneTitle = createTaskTitle('playwright-filter-done')

  await registerAndLogin(page, user)
  await createTaskViaUi(page, { title: todoTitle, status: 'TODO', priority: 'LOW' })
  await createTaskViaUi(page, { title: doneTitle, status: 'DONE', priority: 'HIGH' })

  await page.getByLabel('ステータス').selectOption('DONE')
  await page.getByLabel('優先度').selectOption('HIGH')

  await expect(page.locator('tbody tr').filter({ hasText: doneTitle })).toHaveCount(1)
  await expect(page.locator('tbody tr').filter({ hasText: todoTitle })).toHaveCount(0)
  await expect(page.locator('.summary-card').filter({ hasText: '表示件数' })).toContainText('1')

  await page.getByRole('button', { name: 'ログアウト' }).click()

  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByRole('button', { name: 'ログイン' })).toBeVisible()
})

test('SES-02 NTF-P-11: セッション切れ時はログイン画面へ戻される', async ({ page }) => {
  const user = createE2eUser()

  await registerAndLogin(page, user)

  await page.evaluate(() => {
    window.localStorage.setItem('authToken', 'invalid-token')
  })
  await page.reload()

  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByText('認証期限が切れたため、再度ログインしてください。')).toBeVisible()
})
