import { expect, type Page } from '@playwright/test'

const API_BASE_URL = 'http://localhost:8080'

/**
 * E2Eテストで作成・ログインするユーザー情報。
 */
export type E2eUser = {
  name: string
  email: string
  password: string
}

export type ApiUser = {
  id: number
  name: string
  email: string
}

export type ApiTask = {
  id: number
  title: string
  description?: string | null
  status?: string | null
  priority?: string | null
  version?: number | null
  assignedUser?: ApiUser | null
}

export type ApiComment = {
  id: number
  taskId: number
  content: string
  version?: number | null
}

export type ApiAttachment = {
  id: number
  taskId: number
  originalFileName: string
}

type MultipartValue = string | number | boolean | { name: string; mimeType: string; buffer: Buffer }

type ApiRequestOptions = {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  data?: unknown
  multipart?: Record<string, MultipartValue>
}

/**
 * テストデータの衝突を避けるための一意suffixを生成する。
 */
function buildUniqueSuffix() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

/**
 * 登録テストで利用する一意なユーザー情報を生成する。
 */
export function createE2eUser(): E2eUser {
  const suffix = buildUniqueSuffix()

  return {
    name: `playwright-user-${suffix}`,
    email: `playwright-${suffix}@example.com`,
    password: 'password123',
  }
}

/**
 * E2Eテスト用の一意なタスクタイトルを生成する。
 */
export function createTaskTitle(prefix: string) {
  return `${prefix}-${buildUniqueSuffix()}`
}

/**
 * 認証済みページからAPIを直接呼び出す。
 */
export async function apiRequest<T>(page: Page, path: string, options: ApiRequestOptions = {}) {
  const token = await page.evaluate(() => window.localStorage.getItem('authToken'))

  if (!token) {
    throw new Error('authToken is not stored')
  }

  const response = await page.request.fetch(`${API_BASE_URL}${path}`, {
    method: options.method ?? 'GET',
    headers: {
      Authorization: `Bearer ${token}`,
    },
    data: options.data,
    multipart: options.multipart,
  })

  if (!response.ok()) {
    throw new Error(`${options.method ?? 'GET'} ${path} failed: ${response.status()} ${await response.text()}`)
  }

  if (response.status() === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}

/**
 * localStorageに保持されたログインユーザーIDを数値として取得する。
 */
export async function getCurrentUserId(page: Page) {
  return Number(await page.evaluate(() => window.localStorage.getItem('userId')))
}

/**
 * UIからユーザー登録を実行し、ログイン画面へ戻ることを確認する。
 */
export async function registerUser(page: Page, user: E2eUser) {
  await page.goto('/signup')
  await page.getByLabel(/^ユーザー名/).fill(user.name)
  await page.getByLabel(/^メールアドレス/).fill(user.email)
  await page.getByLabel(/^パスワード(?!確認)/).fill(user.password)
  await page.getByLabel(/^パスワード確認/).fill(user.password)
  await page.getByRole('button', { name: '登録する' }).click()

  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByText('登録に成功しました。ログインしてください。')).toBeVisible()
}

/**
 * UIからログインを実行し、タスク一覧画面へ遷移することを確認する。
 */
export async function loginUser(page: Page, user: E2eUser) {
  await page.goto('/login')
  await page.getByLabel(/^メールアドレス/).fill(user.email)
  await page.getByLabel(/^パスワード(?!確認)/).fill(user.password)
  await page.getByRole('button', { name: 'ログイン' }).click()

  await expect(page).toHaveURL(/\/tasks$/)
  await expect(page.getByRole('heading', { name: 'タスク一覧' })).toBeVisible()
}

/**
 * ユーザー登録からログインまでをまとめて実行する。
 */
export async function registerAndLogin(page: Page, user: E2eUser) {
  await registerUser(page, user)
  await loginUser(page, user)
}

/**
 * UIからログアウトし、ログイン画面へ戻ったことを確認する。
 */
export async function logout(page: Page) {
  await page.getByRole('button', { name: 'ログアウト' }).click()
  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByRole('button', { name: 'ログイン' })).toBeVisible()
}

/**
 * 担当者候補に含まれるメールアドレスからユーザーIDを取得する。
 */
export async function getAssignableUserIdByEmail(page: Page, email: string) {
  const users = await apiRequest<ApiUser[]>(page, '/api/users')
  const user = users.find((candidate) => candidate.email === email)

  if (!user) {
    throw new Error(`Assignable user was not found: ${email}`)
  }

  return user.id
}

/**
 * 担当者候補が存在する環境では、先頭の実ユーザーを選択する。
 */
export async function selectFirstAssignableUserIfAvailable(page: Page) {
  const assigneeSelect = page.getByLabel('担当者')
  const assigneeCount = await assigneeSelect.locator('option').count()
  if (assigneeCount > 1) {
    await assigneeSelect.selectOption({ index: 1 })
  }
}

/**
 * 一覧画面からタスク作成画面へ移動し、最小限の入力でタスクを作成する。
 */
export async function createTaskViaUi(
  page: Page,
  {
    title,
    description,
    status = 'TODO',
    priority = 'MEDIUM',
    dueDate,
  }: {
    title: string
    description?: string
    status?: 'TODO' | 'DOING' | 'DONE'
    priority?: 'LOW' | 'MEDIUM' | 'HIGH'
    dueDate?: string
  },
) {
  await page.locator('.content-header').getByRole('button', { name: 'タスク作成' }).click()
  await expect(page).toHaveURL(/\/tasks\/new$/)

  await page.getByLabel('タイトル').fill(title)
  if (description !== undefined) {
    await page.getByLabel('説明').fill(description)
  }
  if (dueDate !== undefined) {
    await page.getByLabel('期限').fill(dueDate)
  }
  await page.getByLabel('ステータス').selectOption(status)
  await page.getByLabel('優先度').selectOption(priority)
  await selectFirstAssignableUserIfAvailable(page)
  await page.getByRole('button', { name: 'タスクを作成' }).click()

  await expect(page).toHaveURL(/\/tasks$/)
  await expect(page.getByText('タスクを作成しました。')).toBeVisible()
}

/**
 * UI状態を経由せずAPIでタスクを追加する。
 */
export async function createTaskViaApi(
  page: Page,
  title: string,
  assignedUserId?: number,
  options: {
    description?: string
    status?: 'TODO' | 'DOING' | 'DONE'
    priority?: 'LOW' | 'MEDIUM' | 'HIGH'
  } = {},
) {
  return apiRequest<ApiTask>(page, '/api/tasks', {
    method: 'POST',
    data: {
      title,
      description: options.description,
      status: options.status ?? 'TODO',
      priority: options.priority ?? 'MEDIUM',
      assignedUserId,
    },
  })
}

/**
 * APIでタスクの担当者だけを変更する。
 */
export async function updateTaskAssigneeViaApi(page: Page, task: ApiTask, assignedUserId: number) {
  return apiRequest<ApiTask>(page, `/api/tasks/${task.id}`, {
    method: 'PUT',
    data: {
      title: task.title,
      description: task.description ?? undefined,
      status: task.status ?? 'TODO',
      priority: task.priority ?? 'MEDIUM',
      assignedUserId,
      version: Number(task.version ?? 0),
    },
  })
}

/**
 * APIでコメントを追加する。
 */
export async function createCommentViaApi(page: Page, taskId: number, content: string) {
  return apiRequest<ApiComment>(page, `/api/tasks/${taskId}/comments`, {
    method: 'POST',
    data: { content },
  })
}

/**
 * APIで添付ファイルをアップロードする。
 */
export async function uploadAttachmentViaApi(page: Page, taskId: number, fileName: string) {
  return apiRequest<ApiAttachment>(page, `/api/tasks/${taskId}/attachments`, {
    method: 'POST',
    multipart: {
      file: {
        name: fileName,
        mimeType: 'text/plain',
        buffer: Buffer.from(`attachment body for ${fileName}`),
      },
    },
  })
}

/**
 * APIでタスクを削除する。
 */
export async function deleteTaskViaApi(page: Page, taskId: number) {
  await apiRequest<void>(page, `/api/tasks/${taskId}`, { method: 'DELETE' })
}

/**
 * 通知一覧へ移動する。
 */
export async function openNotifications(page: Page) {
  await page.locator('.sidebar').getByRole('button', { name: /通知/ }).click()
  await expect(page).toHaveURL(/\/notifications$/)
}

/**
 * 通知一覧で指定タスクの通知行を開く。
 */
export async function openNotificationRow(page: Page, taskTitle: string) {
  const row = page.locator('tbody tr').filter({ hasText: taskTitle })
  await expect(row).toBeVisible()
  await row.getByRole('button', { name: `${taskTitle}の詳細を開く` }).first().click()
  return row
}

/**
 * 未読コメント通知を受け取るユーザー、通知を発生させるユーザー、対象タスクを用意する。
 */
export async function prepareUnreadCommentNotification(
  page: Page,
  {
    titlePrefix,
    commentContent = 'notification comment',
  }: {
    titlePrefix: string
    commentContent?: string
  },
) {
  const recipient = createE2eUser()
  const actor = createE2eUser()
  const taskTitle = createTaskTitle(titlePrefix)

  await registerUser(page, recipient)
  await registerAndLogin(page, actor)

  const recipientId = await getAssignableUserIdByEmail(page, recipient.email)
  const task = await createTaskViaApi(page, taskTitle, recipientId)
  const comment = await createCommentViaApi(page, task.id, commentContent)

  return { actor, recipient, task, taskTitle, comment }
}
