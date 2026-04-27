import { expect, test, type Page } from '@playwright/test'
import {
  addTeamMemberViaApi,
  createCommentViaApi,
  createE2eUser,
  createTaskTitle,
  createTaskViaApi,
  createTeamViaApi,
  ensureTeamViaApi,
  getAssignableUserIdByEmail,
  getCurrentUserId,
  loginUser,
  logout,
  openNotifications,
  prepareUnreadCommentNotification,
  registerAndLogin,
  registerUser,
  uploadAttachmentViaApi,
} from './helpers'

async function createCurrentUserTaskAndOpenDetail(page: Page, title: string) {
  const currentUserId = await getCurrentUserId(page)
  const task = await createTaskViaApi(page, title, currentUserId)
  await page.goto(`/tasks/${task.id}`)
  await expect(page.getByRole('heading', { name: 'タスク詳細' })).toBeVisible()
  return task
}

async function createAssignedTask(
  page: Page,
  {
    assigneeEmail,
    title,
  }: {
    assigneeEmail: string
    title: string
  },
) {
  const assigneeId = await getAssignableUserIdByEmail(page, assigneeEmail)
  const team = await ensureTeamViaApi(page, createTaskTitle('playwright-ui-control-team'))
  await addTeamMemberViaApi(page, Number(team.id), assigneeId)
  return createTaskViaApi(page, title, assigneeId, { teamId: Number(team.id) })
}

async function openTaskCreatedByOwnerAsTeamAdmin(
  page: Page,
  title: string,
  setupAsTaskOwner?: (task: Awaited<ReturnType<typeof createTaskViaApi>>) => Promise<void>,
) {
  const owner = createE2eUser()
  const admin = createE2eUser()

  await registerAndLogin(page, admin)
  const adminId = await getCurrentUserId(page)
  await logout(page)

  await registerAndLogin(page, owner)
  const ownerId = await getCurrentUserId(page)
  const team = await createTeamViaApi(page, createTaskTitle('playwright-ui-admin-team'))
  await addTeamMemberViaApi(page, Number(team.id), adminId, 'ADMIN')
  const task = await createTaskViaApi(page, title, ownerId, { teamId: Number(team.id) })
  if (setupAsTaskOwner) {
    await setupAsTaskOwner(task)
  }
  await logout(page)

  await loginUser(page, admin)
  await page.goto(`/tasks/${task.id}`)
  await expect(page.getByRole('heading', { name: 'タスク詳細' })).toBeVisible()

  return task
}

async function openTaskCreatedByOwnerAsTeamMember(page: Page, title: string) {
  const owner = createE2eUser()
  const member = createE2eUser()

  await registerAndLogin(page, member)
  const memberId = await getCurrentUserId(page)
  await logout(page)

  await registerAndLogin(page, owner)
  const team = await createTeamViaApi(page, createTaskTitle('playwright-ui-member-team'))
  await addTeamMemberViaApi(page, Number(team.id), memberId, 'MEMBER')
  const task = await createTaskViaApi(page, title, undefined, { teamId: Number(team.id) })
  await logout(page)

  await loginUser(page, member)
  await page.goto(`/tasks/${task.id}`)
  await expect(page.getByRole('heading', { name: 'タスク詳細' })).toBeVisible()

  return task
}

test('UI-01: タスク更新権限がある場合は編集ボタンが表示される', async ({ page }) => {
  const user = createE2eUser()
  const title = createTaskTitle('playwright-ui-task-edit-button')

  await registerAndLogin(page, user)
  await createCurrentUserTaskAndOpenDetail(page, title)

  await expect(page.locator('.content-header').getByRole('button', { name: '編集' })).toBeVisible()
})

test('UI-TEAM-06 UI-01: チームADMINには他者タスクの編集ボタンが表示される', async ({ page }) => {
  const title = createTaskTitle('playwright-ui-admin-edit-button')

  await openTaskCreatedByOwnerAsTeamAdmin(page, title)

  await expect(page.locator('.content-header').getByRole('button', { name: '編集' })).toBeVisible()
})

test('UI-02: タスク削除ボタンは削除権限がある場合のみ表示される', async ({ page }) => {
  const assignee = createE2eUser()
  const creator = createE2eUser()
  const title = createTaskTitle('playwright-ui-task-delete-button')

  await registerUser(page, assignee)
  await registerAndLogin(page, creator)
  const task = await createAssignedTask(page, { assigneeEmail: assignee.email, title })

  await page.goto(`/tasks/${task.id}`)
  await expect(page.locator('.content-header').getByRole('button', { name: '削除', exact: true })).toBeVisible()

  await logout(page)
  await loginUser(page, assignee)
  await page.goto(`/tasks/${task.id}`)

  await expect(page.locator('.content-header').getByRole('button', { name: '削除', exact: true })).toHaveCount(0)
})

test('UI-TEAM-06 UI-02: チームADMINには他者タスクの削除ボタンが表示される', async ({ page }) => {
  const title = createTaskTitle('playwright-ui-admin-delete-button')

  await openTaskCreatedByOwnerAsTeamAdmin(page, title)

  await expect(page.locator('.content-header').getByRole('button', { name: '削除', exact: true })).toBeVisible()
})

test('UI-03: コメント編集ボタンは投稿者本人またはチーム管理者に表示される', async ({ page }) => {
  const assignee = createE2eUser()
  const commenter = createE2eUser()
  const title = createTaskTitle('playwright-ui-comment-edit-button')
  const commentContent = 'editable-comment-by-owner'

  await registerUser(page, assignee)
  await registerAndLogin(page, commenter)
  const task = await createAssignedTask(page, { assigneeEmail: assignee.email, title })
  await createCommentViaApi(page, task.id, commentContent)

  await page.goto(`/tasks/${task.id}`)
  const ownerComment = page.locator('.comment-item').filter({ hasText: commentContent })
  await expect(ownerComment.getByRole('button', { name: '編集' })).toBeVisible()

  await logout(page)
  await loginUser(page, assignee)
  await page.goto(`/tasks/${task.id}`)

  const assigneeComment = page.locator('.comment-item').filter({ hasText: commentContent })
  await expect(assigneeComment).toBeVisible()
  await expect(assigneeComment.getByRole('button', { name: '編集' })).toHaveCount(0)
})

test('UI-03: チームADMINには他者コメントの編集ボタンが表示される', async ({ page }) => {
  const title = createTaskTitle('playwright-ui-admin-comment-edit-button')
  const commentContent = 'editable-comment-by-team-admin'

  await openTaskCreatedByOwnerAsTeamAdmin(page, title, async (task) => {
    await createCommentViaApi(page, task.id, commentContent)
  })

  const comment = page.locator('.comment-item').filter({ hasText: commentContent })
  await expect(comment.getByRole('button', { name: '編集' })).toBeVisible()
})

test('UI-04: コメント削除ボタンは投稿者本人またはチーム管理者に表示される', async ({ page }) => {
  const assignee = createE2eUser()
  const commenter = createE2eUser()
  const title = createTaskTitle('playwright-ui-comment-delete-button')
  const commentContent = 'deletable-comment-by-owner'

  await registerUser(page, assignee)
  await registerAndLogin(page, commenter)
  const task = await createAssignedTask(page, { assigneeEmail: assignee.email, title })
  await createCommentViaApi(page, task.id, commentContent)

  await page.goto(`/tasks/${task.id}`)
  const ownerComment = page.locator('.comment-item').filter({ hasText: commentContent })
  await expect(ownerComment.getByRole('button', { name: '削除' })).toBeVisible()

  await logout(page)
  await loginUser(page, assignee)
  await page.goto(`/tasks/${task.id}`)

  const assigneeComment = page.locator('.comment-item').filter({ hasText: commentContent })
  await expect(assigneeComment).toBeVisible()
  await expect(assigneeComment.getByRole('button', { name: '削除' })).toHaveCount(0)
})

test('UI-04: チームADMINには他者コメントの削除ボタンが表示される', async ({ page }) => {
  const title = createTaskTitle('playwright-ui-admin-comment-delete-button')
  const commentContent = 'deletable-comment-by-team-admin'

  await openTaskCreatedByOwnerAsTeamAdmin(page, title, async (task) => {
    await createCommentViaApi(page, task.id, commentContent)
  })

  const comment = page.locator('.comment-item').filter({ hasText: commentContent })
  await expect(comment.getByRole('button', { name: '削除' })).toBeVisible()
})

test('UI-05: 添付ボタンはタスク更新権限がある場合のみ表示される', async ({ page }) => {
  const assignee = createE2eUser()
  const creator = createE2eUser()
  const title = createTaskTitle('playwright-ui-attachment-button')

  await registerUser(page, assignee)
  await registerAndLogin(page, creator)
  const task = await createAssignedTask(page, { assigneeEmail: assignee.email, title })

  await logout(page)
  await loginUser(page, assignee)
  await page.goto(`/tasks/${task.id}`)
  await expect(page.getByRole('button', { name: '添付' })).toBeVisible()
})

test('UI-05: 通常メンバーには添付ボタンが表示されない', async ({ page }) => {
  const title = createTaskTitle('playwright-ui-member-attachment-button')

  await openTaskCreatedByOwnerAsTeamMember(page, title)

  await expect(page.getByRole('button', { name: '添付' })).toHaveCount(0)
})

test('UI-06: 添付削除ボタンは添付登録者本人またはチーム管理者に表示される', async ({ page }) => {
  const assignee = createE2eUser()
  const uploader = createE2eUser()
  const title = createTaskTitle('playwright-ui-attachment-delete-button')
  const fileName = 'owner-deletable-attachment.txt'

  await registerUser(page, assignee)
  await registerAndLogin(page, uploader)
  const task = await createAssignedTask(page, { assigneeEmail: assignee.email, title })
  await uploadAttachmentViaApi(page, task.id, fileName)

  await page.goto(`/tasks/${task.id}`)
  const ownerAttachment = page.locator('.attachment-row').filter({ hasText: fileName })
  await expect(ownerAttachment.getByRole('button', { name: '削除' })).toBeVisible()

  await logout(page)
  await loginUser(page, assignee)
  await page.goto(`/tasks/${task.id}`)

  const assigneeAttachment = page.locator('.attachment-row').filter({ hasText: fileName })
  await expect(assigneeAttachment).toBeVisible()
  await expect(assigneeAttachment.getByRole('button', { name: '削除' })).toHaveCount(0)
})

test('UI-06: チームADMINには他者添付の削除ボタンが表示される', async ({ page }) => {
  const title = createTaskTitle('playwright-ui-admin-attachment-delete-button')
  const fileName = 'admin-deletable-attachment.txt'

  await openTaskCreatedByOwnerAsTeamAdmin(page, title, async (task) => {
    await uploadAttachmentViaApi(page, task.id, fileName)
  })

  const attachment = page.locator('.attachment-row').filter({ hasText: fileName })
  await expect(attachment.getByRole('button', { name: '削除' })).toBeVisible()
})

test('UI-07: 通知一括既読ボタンは未読通知がある場合のみ表示される', async ({ page }) => {
  const { recipient } = await prepareUnreadCommentNotification(page, {
    titlePrefix: 'playwright-ui-read-all-visible',
  })

  await logout(page)
  await loginUser(page, recipient)
  await openNotifications(page)

  const readAllButton = page.locator('.content-header').getByRole('button', { name: '一括既読' })
  await expect(readAllButton).toBeVisible()
  await readAllButton.click()

  await expect(page.locator('.content-header').getByRole('button', { name: '一括既読' })).toHaveCount(0)
})

test('UI-08: 未読通知バッジは未読件数が1件以上の場合のみ表示される', async ({ page }) => {
  const user = createE2eUser()
  let unreadCount = 0

  await page.route('**/api/notifications/unread-count', async (route) => {
    await route.fulfill({ json: { unreadCount } })
  })

  await registerAndLogin(page, user)
  await expect(page.locator('.nav-badge')).toHaveCount(0)

  unreadCount = 1
  await page.locator('.sidebar').getByRole('button', { name: '通知' }).click()

  await expect(page.locator('.nav-badge')).toHaveText('1')
})

test('UI-09: コメントと履歴タブはタスク参照権限がある場合のみ表示される', async ({ page }) => {
  const assignee = createE2eUser()
  const outsider = createE2eUser()
  const creator = createE2eUser()
  const title = createTaskTitle('playwright-ui-activity-tabs')

  await registerUser(page, assignee)
  await registerUser(page, outsider)
  await registerAndLogin(page, creator)
  const task = await createAssignedTask(page, { assigneeEmail: assignee.email, title })

  await logout(page)
  await loginUser(page, assignee)
  await page.goto(`/tasks/${task.id}`)
  await expect(page.getByRole('button', { name: 'コメント', exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: '履歴', exact: true })).toBeVisible()

  await logout(page)
  await loginUser(page, outsider)
  await page.goto(`/tasks/${task.id}`)

  await expect(page.getByRole('button', { name: 'コメント', exact: true })).toHaveCount(0)
  await expect(page.getByRole('button', { name: '履歴', exact: true })).toHaveCount(0)
})

test('UI-10: タスク保存中は保存ボタンを再押下できない', async ({ page }) => {
  const user = createE2eUser()
  const taskTitle = createTaskTitle('playwright-task-double-submit')
  let resolveUpdateRequestStarted!: () => void
  let releaseUpdateRequest!: () => void
  const updateRequestStarted = new Promise<void>((resolve) => {
    resolveUpdateRequestStarted = resolve
  })

  await page.route('**/api/tasks/*', async (route) => {
    if (route.request().method() !== 'PUT') {
      await route.continue()
      return
    }

    resolveUpdateRequestStarted()
    await new Promise<void>((release) => {
      releaseUpdateRequest = release
    })
    await route.continue()
  })

  await registerAndLogin(page, user)
  const task = await createCurrentUserTaskAndOpenDetail(page, taskTitle)

  await page.getByRole('button', { name: '編集' }).click()
  await page.getByLabel('タイトル').fill(`${task.title}-updated`)
  await page.getByRole('button', { name: '保存' }).click()

  await updateRequestStarted
  await expect(page.getByRole('button', { name: '保存中...' })).toBeDisabled()

  releaseUpdateRequest()

  await expect(page.getByText('タスクを更新しました。')).toBeVisible()
})

test('UI-11: コメント投稿中は投稿操作を再実行できない', async ({ page }) => {
  const user = createE2eUser()
  const taskTitle = createTaskTitle('playwright-comment-create-double-submit')
  let taskId = 0
  let releaseCreateRequest!: () => void

  await page.route('**/api/tasks/*/comments', async (route) => {
    if (route.request().method() !== 'POST') {
      await route.continue()
      return
    }

    await new Promise<void>((release) => {
      releaseCreateRequest = release
    })
    await route.fulfill({
      status: 201,
      json: {
        id: 9_000_001,
        taskId,
        content: 'double-submit-comment',
        createdBy: { id: await getCurrentUserId(page), name: user.name, email: user.email },
        createdAt: new Date().toISOString(),
        updatedAt: null,
        version: 0,
      },
    })
  })

  await registerAndLogin(page, user)
  const task = await createCurrentUserTaskAndOpenDetail(page, taskTitle)
  taskId = task.id

  await page.getByPlaceholder('コメントを追加する...').fill('double-submit-comment')
  await page.getByRole('button', { name: '投稿' }).click()

  await expect(page.getByRole('button', { name: '投稿中...' })).toBeDisabled()

  releaseCreateRequest()

  await expect(page.getByRole('button', { name: '投稿' })).toBeVisible()
})

test('UI-11: コメント更新中は更新操作を再実行できない', async ({ page }) => {
  const user = createE2eUser()
  const taskTitle = createTaskTitle('playwright-comment-update-double-submit')
  const commentContent = 'double-submit-update-comment'
  const updatedContent = `${commentContent}-updated`
  let releaseUpdateRequest!: () => void

  await page.route('**/api/comments/*', async (route) => {
    if (route.request().method() !== 'PUT') {
      await route.continue()
      return
    }

    await new Promise<void>((release) => {
      releaseUpdateRequest = release
    })
    await route.fulfill({
      status: 200,
      json: {
        id: 9_000_002,
        taskId: 0,
        content: updatedContent,
        createdBy: { id: await getCurrentUserId(page), name: user.name, email: user.email },
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        version: 1,
      },
    })
  })

  await registerAndLogin(page, user)
  const task = await createCurrentUserTaskAndOpenDetail(page, taskTitle)
  await createCommentViaApi(page, task.id, commentContent)
  await page.reload()

  const comment = page.locator('.comment-item').filter({ hasText: commentContent })
  await expect(comment).toBeVisible()
  await comment.getByRole('button', { name: '編集' }).click()
  await comment.locator('textarea').fill(updatedContent)
  await comment.getByRole('button', { name: '保存' }).click()

  await expect(comment.getByRole('button', { name: '更新中...' })).toBeDisabled()

  releaseUpdateRequest()

  await expect(page.getByRole('button', { name: '投稿' })).toBeVisible()
})

test('UI-11: コメント削除中は削除操作を再実行できない', async ({ page }) => {
  const user = createE2eUser()
  const taskTitle = createTaskTitle('playwright-comment-delete-double-submit')
  const commentContent = 'double-submit-delete-comment'
  let releaseDeleteRequest!: () => void

  await page.route('**/api/comments/*', async (route) => {
    if (route.request().method() !== 'DELETE') {
      await route.continue()
      return
    }

    await new Promise<void>((release) => {
      releaseDeleteRequest = release
    })
    await route.fulfill({ status: 204 })
  })

  await registerAndLogin(page, user)
  const task = await createCurrentUserTaskAndOpenDetail(page, taskTitle)
  await createCommentViaApi(page, task.id, commentContent)
  await page.reload()

  const comment = page.locator('.comment-item').filter({ hasText: commentContent })
  await expect(comment).toBeVisible()
  page.once('dialog', (dialog) => dialog.accept())
  await comment.getByRole('button', { name: '削除' }).click()

  await expect(comment.getByRole('button', { name: '処理中...' })).toBeDisabled()

  releaseDeleteRequest()

  await expect(comment.getByRole('button', { name: '削除' })).toBeVisible()
})

test('UI-12: 添付アップロード中はアップロード操作を再実行できない', async ({ page }) => {
  const user = createE2eUser()
  const taskTitle = createTaskTitle('playwright-attachment-upload-double-submit')
  const fileName = 'double-submit-attachment.txt'
  let taskId = 0
  let releaseUploadRequest!: () => void

  await page.route('**/api/tasks/*/attachments', async (route) => {
    if (route.request().method() !== 'POST') {
      await route.continue()
      return
    }

    await new Promise<void>((release) => {
      releaseUploadRequest = release
    })
    await route.fulfill({
      status: 201,
      json: {
        id: 9_100_001,
        taskId,
        originalFileName: fileName,
        contentType: 'text/plain',
        fileSize: 24,
        storageType: 'LOCAL',
        uploadedBy: { id: await getCurrentUserId(page), name: user.name, email: user.email },
        createdAt: new Date().toISOString(),
      },
    })
  })

  await registerAndLogin(page, user)
  const task = await createCurrentUserTaskAndOpenDetail(page, taskTitle)
  taskId = task.id

  await page.locator('input[type="file"]').setInputFiles({
    name: fileName,
    mimeType: 'text/plain',
    buffer: Buffer.from('double submit attachment'),
  })

  await expect(page.getByRole('button', { name: 'アップロード中...' })).toBeDisabled()

  releaseUploadRequest()

  await expect(page.getByRole('button', { name: '添付' })).toBeVisible()
})

test('UI-12: 添付削除中は削除操作を再実行できない', async ({ page }) => {
  const user = createE2eUser()
  const taskTitle = createTaskTitle('playwright-attachment-delete-double-submit')
  const fileName = 'double-submit-delete-attachment.txt'
  let releaseDeleteRequest!: () => void

  await page.route('**/api/attachments/*', async (route) => {
    if (route.request().method() !== 'DELETE') {
      await route.continue()
      return
    }

    await new Promise<void>((release) => {
      releaseDeleteRequest = release
    })
    await route.fulfill({ status: 204 })
  })

  await registerAndLogin(page, user)
  const task = await createCurrentUserTaskAndOpenDetail(page, taskTitle)
  await uploadAttachmentViaApi(page, task.id, fileName)
  await page.reload()

  const attachmentRow = page.locator('.attachment-row').filter({ hasText: fileName })
  await expect(attachmentRow).toBeVisible()
  page.once('dialog', (dialog) => dialog.accept())
  await attachmentRow.getByRole('button', { name: '削除' }).click()

  await expect(attachmentRow.getByRole('button', { name: '処理中...' })).toBeDisabled()

  releaseDeleteRequest()

  await expect(attachmentRow.getByRole('button', { name: '削除' })).toBeVisible()
})

test('UI-13: 個別既読化中は既読操作を再実行できない', async ({ page }) => {
  const { recipient, taskTitle } = await prepareUnreadCommentNotification(page, {
    titlePrefix: 'playwright-ui-individual-read-disabled',
  })
  let releaseReadRequest!: () => void

  await page.route('**/api/notifications/*/read', async (route) => {
    await new Promise<void>((release) => {
      releaseReadRequest = release
    })
    await route.continue()
  })

  await logout(page)
  await loginUser(page, recipient)
  await openNotifications(page)

  const row = page.locator('tbody tr').filter({ hasText: taskTitle })
  await row.getByRole('button', { name: '既読にする' }).click()

  await expect(row.getByRole('button', { name: '処理中...' })).toBeDisabled()

  releaseReadRequest()

  await expect(row.locator('.badge-muted')).toHaveText('既読')
})

test('UI-13: 一括既読化中は既読操作を再実行できない', async ({ page }) => {
  const { recipient } = await prepareUnreadCommentNotification(page, {
    titlePrefix: 'playwright-ui-read-all-disabled',
  })
  let releaseReadAllRequest!: () => void

  await page.route('**/api/notifications/read-all', async (route) => {
    await new Promise<void>((release) => {
      releaseReadAllRequest = release
    })
    await route.continue()
  })

  await logout(page)
  await loginUser(page, recipient)
  await openNotifications(page)

  await page.locator('.content-header').getByRole('button', { name: '一括既読' }).click()

  await expect(page.locator('.content-header').getByRole('button', { name: '既読化中...' })).toBeDisabled()

  releaseReadAllRequest()

  await expect(page.locator('.content-header').getByRole('button', { name: '一括既読' })).toHaveCount(0)
})
