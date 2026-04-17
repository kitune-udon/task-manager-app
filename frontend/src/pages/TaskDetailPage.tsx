import { useEffect, useMemo, useRef, useState, type ChangeEvent, type FormEvent } from 'react'
import type { TaskPriority, TaskStatus, TaskItem, TaskComment, TaskAttachment, ActivityItem } from '../lib/taskApi'
import {
  createComment,
  deleteAttachment,
  deleteComment,
  downloadAttachment,
  fetchActivities,
  fetchAttachments,
  fetchComments,
  updateComment,
  uploadAttachment,
} from '../lib/taskApi'
import type { AssigneeOption, TaskFormBindings } from '../hooks/taskStateShared'
import { resolveUserMessage } from '../lib/authApi'
import { TaskShell } from '../components/TaskShell'
import { formatDate, formatDateTime } from '../utils/format'

type DetailTab = 'comments' | 'history'

type Props = {
  activePath: string
  currentUserLabel: string
  currentUserId: number | null
  unreadCount: number
  onRefreshUnreadCount: () => Promise<void>
  onNavigate: (path: string) => void
  onLogout: () => void
  onShowList: () => void
  onStartEdit: () => void
  onCancelEdit: () => void
  onReloadDetail: () => Promise<TaskItem | null>
  onDelete: () => void
  isDeleting: boolean
  isEditing: boolean
  detailErrorMessage: string
  successMessage: string
  isLoadingDetail: boolean
  selectedTask: TaskItem | null
  editForm: TaskFormBindings
  onEditSubmit: (event: FormEvent<HTMLFormElement>) => void
  isSubmitting: boolean
  statusOptions: Array<{ label: string; value: TaskStatus }>
  priorityOptions: Array<{ label: string; value: TaskPriority }>
  assigneeOptions: AssigneeOption[]
  isLoadingAssigneeOptions: boolean
  assigneeOptionsError: string
}

const DETAIL_FORM_ID = 'task-detail-edit-form'

function formatFileSize(value?: number | null) {
  if (!value && value !== 0) {
    return '-'
  }

  if (value < 1024) {
    return `${value} B`
  }

  if (value < 1024 * 1024) {
    return `${(value / 1024).toFixed(1)} KB`
  }

  return `${(value / (1024 * 1024)).toFixed(1)} MB`
}

function canManageOwnResource(currentUserId: number | null, ownerId?: number | string | null) {
  if (currentUserId === null || ownerId === undefined || ownerId === null) {
    return false
  }

  return Number(ownerId) === currentUserId
}

function UserOutlineIcon() {
  return (
    <svg fill="none" height="24" viewBox="0 0 24 24" width="24" xmlns="http://www.w3.org/2000/svg">
      <path
        d="M12 12C14.4853 12 16.5 9.98528 16.5 7.5C16.5 5.01472 14.4853 3 12 3C9.51472 3 7.5 5.01472 7.5 7.5C7.5 9.98528 9.51472 12 12 12Z"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
      <path
        d="M4.5 20.25C4.5 16.9363 7.85786 14.25 12 14.25C16.1421 14.25 19.5 16.9363 19.5 20.25"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
    </svg>
  )
}

export function TaskDetailPage({
  activePath,
  currentUserLabel,
  currentUserId,
  unreadCount,
  onRefreshUnreadCount,
  onNavigate,
  onLogout,
  onShowList,
  onStartEdit,
  onCancelEdit,
  onReloadDetail,
  onDelete,
  isDeleting,
  isEditing,
  detailErrorMessage,
  successMessage,
  isLoadingDetail,
  selectedTask,
  editForm,
  onEditSubmit,
  isSubmitting,
  statusOptions,
  priorityOptions,
  assigneeOptions,
  isLoadingAssigneeOptions,
  assigneeOptionsError,
}: Props) {
  const [activeTab, setActiveTab] = useState<DetailTab>('comments')
  const [comments, setComments] = useState<TaskComment[]>([])
  const [attachments, setAttachments] = useState<TaskAttachment[]>([])
  const [activities, setActivities] = useState<ActivityItem[]>([])
  const [isLoadingComments, setIsLoadingComments] = useState(false)
  const [isLoadingAttachments, setIsLoadingAttachments] = useState(false)
  const [isLoadingActivities, setIsLoadingActivities] = useState(false)
  const [commentErrorMessage, setCommentErrorMessage] = useState('')
  const [attachmentErrorMessage, setAttachmentErrorMessage] = useState('')
  const [activityErrorMessage, setActivityErrorMessage] = useState('')
  const [commentDraft, setCommentDraft] = useState('')
  const [editingCommentId, setEditingCommentId] = useState<number | null>(null)
  const [editingCommentContent, setEditingCommentContent] = useState('')
  const [isSubmittingComment, setIsSubmittingComment] = useState(false)
  const [activeCommentId, setActiveCommentId] = useState<number | null>(null)
  const [isUploadingAttachment, setIsUploadingAttachment] = useState(false)
  const [activeAttachmentId, setActiveAttachmentId] = useState<number | null>(null)
  const fileInputRef = useRef<HTMLInputElement | null>(null)

  const selectedTaskId = selectedTask?.id ?? null

  const loadComments = async (taskId: number | string) => {
    setIsLoadingComments(true)
    setCommentErrorMessage('')

    try {
      const nextComments = await fetchComments(taskId)
      setComments(nextComments)
    } catch (error) {
      setComments([])
      setCommentErrorMessage(resolveUserMessage(error))
    } finally {
      setIsLoadingComments(false)
    }
  }

  const loadAttachments = async (taskId: number | string) => {
    setIsLoadingAttachments(true)
    setAttachmentErrorMessage('')

    try {
      const nextAttachments = await fetchAttachments(taskId)
      setAttachments(nextAttachments)
    } catch (error) {
      setAttachments([])
      setAttachmentErrorMessage(resolveUserMessage(error))
    } finally {
      setIsLoadingAttachments(false)
    }
  }

  const loadActivities = async (taskId: number | string) => {
    setIsLoadingActivities(true)
    setActivityErrorMessage('')

    try {
      const nextActivities = await fetchActivities(taskId)
      setActivities(nextActivities)
    } catch (error) {
      setActivities([])
      setActivityErrorMessage(resolveUserMessage(error))
    } finally {
      setIsLoadingActivities(false)
    }
  }

  const reloadDetailResources = async () => {
    if (!selectedTaskId) {
      setComments([])
      setAttachments([])
      setActivities([])
      return
    }

    await Promise.all([loadComments(selectedTaskId), loadAttachments(selectedTaskId), loadActivities(selectedTaskId)])
  }

  useEffect(() => {
    setActiveTab('comments')
    setCommentDraft('')
    setEditingCommentId(null)
    setEditingCommentContent('')
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }

    if (!selectedTaskId) {
      setComments([])
      setAttachments([])
      setActivities([])
      setCommentErrorMessage('')
      setAttachmentErrorMessage('')
      setActivityErrorMessage('')
      return
    }

    void reloadDetailResources()
  }, [selectedTaskId])

  const handleCreateComment = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (!selectedTaskId) {
      return
    }

    const content = commentDraft.trim()
    if (!content) {
      setCommentErrorMessage('コメント内容を入力してください。')
      return
    }

    setIsSubmittingComment(true)
    setCommentErrorMessage('')

    try {
      await createComment(selectedTaskId, { content })
      setCommentDraft('')
      await Promise.all([loadComments(selectedTaskId), loadActivities(selectedTaskId), onReloadDetail(), onRefreshUnreadCount()])
    } catch (error) {
      setCommentErrorMessage(resolveUserMessage(error))
    } finally {
      setIsSubmittingComment(false)
    }
  }

  const handleSaveEditedComment = async (comment: TaskComment) => {
    const content = editingCommentContent.trim()
    if (!content) {
      setCommentErrorMessage('コメント内容を入力してください。')
      return
    }

    setActiveCommentId(comment.id)
    setCommentErrorMessage('')

    try {
      await updateComment(comment.id, {
        content,
        version: Number(comment.version ?? 0),
      })
      setEditingCommentId(null)
      setEditingCommentContent('')
      await Promise.all([loadComments(comment.taskId), loadActivities(comment.taskId), onReloadDetail()])
    } catch (error) {
      setCommentErrorMessage(resolveUserMessage(error))
    } finally {
      setActiveCommentId(null)
    }
  }

  const handleDeleteComment = async (comment: TaskComment) => {
    if (!window.confirm('このコメントを削除しますか？')) {
      return
    }

    setActiveCommentId(comment.id)
    setCommentErrorMessage('')

    try {
      await deleteComment(comment.id)
      if (editingCommentId === comment.id) {
        setEditingCommentId(null)
        setEditingCommentContent('')
      }
      await Promise.all([loadComments(comment.taskId), loadActivities(comment.taskId), onReloadDetail()])
    } catch (error) {
      setCommentErrorMessage(resolveUserMessage(error))
    } finally {
      setActiveCommentId(null)
    }
  }

  const handleAttachmentSelectionChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0] ?? null

    if (!selectedTaskId || !file) {
      return
    }

    setIsUploadingAttachment(true)
    setAttachmentErrorMessage('')

    try {
      await uploadAttachment(selectedTaskId, file)
      await Promise.all([loadAttachments(selectedTaskId), loadActivities(selectedTaskId), onReloadDetail(), onRefreshUnreadCount()])
    } catch (error) {
      setAttachmentErrorMessage(resolveUserMessage(error))
    } finally {
      event.target.value = ''
      setIsUploadingAttachment(false)
    }
  }

  const handleAttachmentButtonClick = () => {
    fileInputRef.current?.click()
  }

  const handleDeleteAttachment = async (attachment: TaskAttachment) => {
    if (!window.confirm('この添付ファイルを削除しますか？')) {
      return
    }

    setActiveAttachmentId(attachment.id)
    setAttachmentErrorMessage('')

    try {
      await deleteAttachment(attachment.id)
      await Promise.all([loadAttachments(attachment.taskId), loadActivities(attachment.taskId), onReloadDetail()])
    } catch (error) {
      setAttachmentErrorMessage(resolveUserMessage(error))
    } finally {
      setActiveAttachmentId(null)
    }
  }

  const handleDownloadAttachment = async (attachment: TaskAttachment) => {
    setActiveAttachmentId(attachment.id)
    setAttachmentErrorMessage('')

    try {
      await downloadAttachment(attachment.id, attachment.originalFileName)
    } catch (error) {
      setAttachmentErrorMessage(resolveUserMessage(error))
    } finally {
      setActiveAttachmentId(null)
    }
  }

  const detailActions = useMemo(
    () => (
      <>
        <button className="secondary-button" onClick={onShowList} type="button">
          一覧へ戻る
        </button>
        {!selectedTask ? null : !isEditing ? (
          <>
            <button className="secondary-button" onClick={onStartEdit} type="button">
              編集
            </button>
            <button className="primary-button danger-button" disabled={isDeleting} onClick={onDelete} type="button">
              {isDeleting ? '削除中...' : '削除'}
            </button>
          </>
        ) : (
          <>
            <button className="secondary-button" disabled={isSubmitting} onClick={onCancelEdit} type="button">
              キャンセル
            </button>
            <button className="primary-button" disabled={isSubmitting} form={DETAIL_FORM_ID} type="submit">
              {isSubmitting ? '保存中...' : '保存'}
            </button>
          </>
        )}
      </>
    ),
    [isDeleting, isEditing, isSubmitting, onCancelEdit, onDelete, onShowList, onStartEdit, selectedTask],
  )

  return (
    <TaskShell
      title="タスク詳細"
      description="タスク本体の確認と更新、コメント・添付・履歴の確認ができます。"
      activePath={activePath}
      onNavigate={onNavigate}
      onLogout={onLogout}
      currentUserLabel={currentUserLabel}
      unreadCount={unreadCount}
      contentAreaClassName="task-detail-content-area"
      contentBodyClassName="task-detail-content-body"
      actions={detailActions}
    >
      {isEditing ? <form id={DETAIL_FORM_ID} onSubmit={onEditSubmit} /> : null}
      {detailErrorMessage ? <div className="status-box error-box">{detailErrorMessage}</div> : null}
      {successMessage ? <div className="status-box success-box">{successMessage}</div> : null}

      <section className="panel section-panel detail-layout-panel">
        {isLoadingDetail ? (
          <p className="empty-message">タスクを読み込み中です...</p>
        ) : !selectedTask ? (
          <p className="empty-message">タスクが見つかりません。</p>
        ) : (
          <div className="task-detail-layout">
            <section className="task-detail-main-card">
              <section className="task-detail-section">
                <div className="task-detail-section-header">
                  <div>
                    <p className="task-detail-id">タスクID: {selectedTask.id}</p>
                    <h2 className="task-detail-section-title">本文</h2>
                  </div>
                </div>

                <div className="task-detail-body-grid">
                  <label className="detail-field-stack">
                    <span className="summary-label">タイトル</span>
                    {isEditing ? (
                      <>
                        <input
                          className={editForm.fieldErrors.title ? 'input-error' : ''}
                          form={DETAIL_FORM_ID}
                          onChange={(event) => editForm.onTitleChange(event.target.value)}
                          type="text"
                          value={editForm.title}
                        />
                        {editForm.fieldErrors.title ? <span className="field-error">{editForm.fieldErrors.title}</span> : null}
                      </>
                    ) : (
                      <div className="detail-value-box detail-title-value">{selectedTask.title}</div>
                    )}
                  </label>

                  <label className="detail-field-stack">
                    <span className="summary-label">説明</span>
                    {isEditing ? (
                      <>
                        <textarea
                          className={editForm.fieldErrors.description ? 'input-error' : ''}
                          form={DETAIL_FORM_ID}
                          onChange={(event) => editForm.onDescriptionChange(event.target.value)}
                          rows={6}
                          value={editForm.description}
                        />
                        {editForm.fieldErrors.description ? (
                          <span className="field-error">{editForm.fieldErrors.description}</span>
                        ) : null}
                      </>
                    ) : (
                      <div className="detail-value-box detail-description-value">{selectedTask.description ?? '-'}</div>
                    )}
                  </label>
                </div>
              </section>

              <section className="task-detail-section">
                <div className="task-detail-section-header">
                  <h3 className="task-detail-section-title">添付ファイル</h3>
                  <div className="attachment-toolbar">
                    <input
                      hidden
                      onChange={handleAttachmentSelectionChange}
                      ref={fileInputRef}
                      type="file"
                    />
                    <button
                      className="secondary-button"
                      disabled={isUploadingAttachment}
                      onClick={handleAttachmentButtonClick}
                      type="button"
                    >
                      {isUploadingAttachment ? 'アップロード中...' : '添付'}
                    </button>
                  </div>
                </div>
                {attachmentErrorMessage ? <div className="status-box error-box">{attachmentErrorMessage}</div> : null}

                {isLoadingAttachments ? (
                  <p className="empty-message">添付一覧を読み込み中です...</p>
                ) : attachments.length === 0 ? (
                  <p className="empty-message">添付ファイルはまだありません。</p>
                ) : (
                  <div className="attachment-list">
                    {attachments.map((attachment) => {
                      const canDelete = canManageOwnResource(currentUserId, attachment.uploadedBy?.id)
                      const isActiveAttachment = activeAttachmentId === attachment.id

                      return (
                        <article className="file-item attachment-row" key={attachment.id}>
                          <div className="attachment-main">
                            <span aria-hidden="true" className="attachment-icon">
                              <svg fill="none" height="18" viewBox="0 0 18 18" width="18" xmlns="http://www.w3.org/2000/svg">
                                <path
                                  d="M5.25 1.5H9.75L13.5 5.25V14.25C13.5 15.0784 12.8284 15.75 12 15.75H5.25C4.42157 15.75 3.75 15.0784 3.75 14.25V3C3.75 2.17157 4.42157 1.5 5.25 1.5Z"
                                  stroke="currentColor"
                                  strokeLinecap="round"
                                  strokeLinejoin="round"
                                  strokeWidth="1.4"
                                />
                                <path
                                  d="M9.75 1.5V5.25H13.5"
                                  stroke="currentColor"
                                  strokeLinecap="round"
                                  strokeLinejoin="round"
                                  strokeWidth="1.4"
                                />
                                <path
                                  d="M6.75 8.25H10.5"
                                  stroke="currentColor"
                                  strokeLinecap="round"
                                  strokeWidth="1.4"
                                />
                                <path
                                  d="M6.75 11.25H10.5"
                                  stroke="currentColor"
                                  strokeLinecap="round"
                                  strokeWidth="1.4"
                                />
                              </svg>
                            </span>
                            <div className="attachment-summary">
                              <button
                                className="attachment-link"
                                disabled={isActiveAttachment}
                                onClick={() => void handleDownloadAttachment(attachment)}
                                type="button"
                              >
                                {attachment.originalFileName}
                              </button>
                              <span className="attachment-meta">{formatFileSize(attachment.fileSize)}</span>
                              <span className="attachment-meta">{attachment.uploadedBy?.name ?? '不明なユーザー'}</span>
                            </div>
                          </div>
                          {canDelete ? (
                            <button
                              className="attachment-delete-button"
                              disabled={isActiveAttachment}
                              onClick={() => void handleDeleteAttachment(attachment)}
                              type="button"
                            >
                              {isActiveAttachment ? '処理中...' : '削除'}
                            </button>
                          ) : null}
                        </article>
                      )
                    })}
                  </div>
                )}
              </section>

              <section className="task-detail-section task-activity-section">
                <div className="activity-toolbar">
                  <h3 className="task-detail-section-title">アクティビティ</h3>
                  <button
                    className="text-link-button activity-refresh-button"
                    onClick={() => void (activeTab === 'comments' ? loadComments(selectedTask.id) : loadActivities(selectedTask.id))}
                    type="button"
                  >
                    再読込
                  </button>
                </div>
                <div className="activity-display-row">
                  <span className="activity-display-label">表示:</span>
                  <div className="tab-list activity-tab-list" role="tablist">
                    <button
                      className={activeTab === 'comments' ? 'tab-button active' : 'tab-button'}
                      onClick={() => setActiveTab('comments')}
                      type="button"
                    >
                      コメント
                    </button>
                    <button
                      className={activeTab === 'history' ? 'tab-button active' : 'tab-button'}
                      onClick={() => setActiveTab('history')}
                      type="button"
                    >
                      履歴
                    </button>
                  </div>
                </div>

                {activeTab === 'comments' ? (
                  <div className="tab-panel activity-panel">
                    {commentErrorMessage ? <div className="status-box error-box">{commentErrorMessage}</div> : null}

                    <form className="comment-compose-form activity-compose-form" onSubmit={handleCreateComment}>
                      <div aria-hidden="true" className="activity-compose-avatar">
                        <UserOutlineIcon />
                      </div>
                      <div className="activity-compose-panel">
                        <textarea
                          className="comment-input activity-comment-input"
                          onChange={(event) => setCommentDraft(event.target.value)}
                          placeholder="コメントを追加する..."
                          rows={4}
                          value={commentDraft}
                        />
                        <div className="comment-compose-actions activity-compose-actions">
                          <button className="primary-button activity-submit-button" disabled={isSubmittingComment} type="submit">
                            {isSubmittingComment ? '投稿中...' : '投稿'}
                          </button>
                        </div>
                      </div>
                    </form>

                    {isLoadingComments ? (
                      <p className="empty-message">コメントを読み込み中です...</p>
                    ) : comments.length === 0 ? (
                      <p className="empty-message">コメントはまだありません。</p>
                    ) : (
                      <div className="stack-list activity-stack-list">
                        {comments.map((comment) => {
                          const canManage = canManageOwnResource(currentUserId, comment.createdBy?.id)
                          const isEditingComment = editingCommentId === comment.id
                          const isActiveComment = activeCommentId === comment.id
                          const isOwnComment = canManageOwnResource(currentUserId, comment.createdBy?.id)
                          const updatedAtLabel = comment.updatedAt ? formatDateTime(comment.updatedAt) : formatDateTime(comment.createdAt)

                          return (
                            <article className="comment-item activity-comment-card" key={comment.id}>
                              <div className="comment-header activity-comment-header">
                                <div className="activity-comment-identity">
                                  <span aria-hidden="true" className="activity-comment-user-icon">
                                    <UserOutlineIcon />
                                  </span>
                                  <div className="activity-comment-meta-block">
                                    <div className="activity-comment-name-row">
                                      <p className="comment-author">{comment.createdBy?.name ?? '不明なユーザー'}</p>
                                      {isOwnComment ? <span className="activity-self-badge">自分</span> : null}
                                    </div>
                                    <p className="comment-meta activity-comment-timestamp">
                                      投稿日: {formatDateTime(comment.createdAt)}
                                      <span className="activity-timestamp-divider" />
                                      更新日: {updatedAtLabel}
                                    </p>
                                  </div>
                                </div>
                                {canManage ? (
                                  <div className="comment-item-actions activity-comment-actions">
                                    {isEditingComment ? null : (
                                      <button
                                        className="activity-comment-button"
                                        onClick={() => {
                                          setEditingCommentId(comment.id)
                                          setEditingCommentContent(comment.content)
                                          setCommentErrorMessage('')
                                        }}
                                        type="button"
                                      >
                                        編集
                                      </button>
                                    )}
                                    <button
                                      className="activity-comment-button activity-comment-delete-button"
                                      disabled={isActiveComment}
                                      onClick={() => void handleDeleteComment(comment)}
                                      type="button"
                                    >
                                      {isActiveComment ? '処理中...' : '削除'}
                                    </button>
                                  </div>
                                ) : null}
                              </div>

                              {isEditingComment ? (
                                <div className="comment-edit-area activity-comment-edit-area">
                                  <textarea
                                    className="comment-input activity-comment-input"
                                    onChange={(event) => setEditingCommentContent(event.target.value)}
                                    rows={4}
                                    value={editingCommentContent}
                                  />
                                  <div className="comment-compose-actions activity-compose-actions">
                                    <button
                                      className="activity-comment-button"
                                      onClick={() => {
                                        setEditingCommentId(null)
                                        setEditingCommentContent('')
                                      }}
                                      type="button"
                                    >
                                      キャンセル
                                    </button>
                                    <button
                                      className="primary-button activity-submit-button"
                                      disabled={isActiveComment}
                                      onClick={() => void handleSaveEditedComment(comment)}
                                      type="button"
                                    >
                                      {isActiveComment ? '更新中...' : '保存'}
                                    </button>
                                  </div>
                                </div>
                              ) : (
                                <p className="comment-text">{comment.content}</p>
                              )}
                            </article>
                          )
                        })}
                      </div>
                    )}
                  </div>
                ) : null}

                {activeTab === 'history' ? (
                  <div className="tab-panel activity-panel">
                    {activityErrorMessage ? <div className="status-box error-box">{activityErrorMessage}</div> : null}

                    {isLoadingActivities ? (
                      <p className="empty-message">履歴を読み込み中です...</p>
                    ) : activities.length === 0 ? (
                      <p className="empty-message">履歴はまだありません。</p>
                    ) : (
                      <div className="stack-list activity-stack-list">
                        {activities.map((activity) => (
                          <article className="activity-item activity-history-card" key={activity.id}>
                            <div className="activity-item-header activity-history-header">
                              <span className="badge activity-type-badge">{activity.eventType}</span>
                              <span className="activity-history-date">{formatDateTime(activity.createdAt)}</span>
                            </div>
                            <p className="activity-summary">{activity.summary ?? '-'}</p>
                          </article>
                        ))}
                      </div>
                    )}
                  </div>
                ) : null}
              </section>
            </section>

            <aside className="task-side-card task-detail-side-card">
              <h3 className="task-detail-section-title">属性</h3>
              <div className="task-detail-side-grid">
                <label className="task-detail-side-item">
                  <span className="summary-label">ステータス</span>
                  {isEditing ? (
                    <>
                      <select
                        className={editForm.fieldErrors.status ? 'input-error' : ''}
                        form={DETAIL_FORM_ID}
                        onChange={(event) => editForm.onStatusChange(event.target.value)}
                        value={editForm.status}
                      >
                        {statusOptions.map((option) => (
                          <option key={option.value} value={option.value}>
                            {option.label}
                          </option>
                        ))}
                      </select>
                      {editForm.fieldErrors.status ? <span className="field-error">{editForm.fieldErrors.status}</span> : null}
                    </>
                  ) : (
                    <strong>{selectedTask.status ?? '-'}</strong>
                  )}
                </label>

                <label className="task-detail-side-item">
                  <span className="summary-label">優先度</span>
                  {isEditing ? (
                    <>
                      <select
                        className={editForm.fieldErrors.priority ? 'input-error' : ''}
                        form={DETAIL_FORM_ID}
                        onChange={(event) => editForm.onPriorityChange(event.target.value)}
                        value={editForm.priority}
                      >
                        {priorityOptions.map((option) => (
                          <option key={option.value} value={option.value}>
                            {option.label}
                          </option>
                        ))}
                      </select>
                      {editForm.fieldErrors.priority ? (
                        <span className="field-error">{editForm.fieldErrors.priority}</span>
                      ) : null}
                    </>
                  ) : (
                    <strong>{selectedTask.priority ?? '-'}</strong>
                  )}
                </label>

                <label className="task-detail-side-item">
                  <span className="summary-label">担当者</span>
                  {isEditing ? (
                    <>
                      <select
                        className={editForm.fieldErrors.assignedUserId ? 'input-error' : ''}
                        disabled={isLoadingAssigneeOptions}
                        form={DETAIL_FORM_ID}
                        onChange={(event) => editForm.onAssignedUserIdChange(event.target.value)}
                        value={editForm.assignedUserId}
                      >
                        {assigneeOptions.map((option) => (
                          <option key={option.value || 'unassigned'} value={option.value}>
                            {option.label}
                          </option>
                        ))}
                      </select>
                      {isLoadingAssigneeOptions ? <p className="empty-message">担当者候補を読み込み中です...</p> : null}
                      {assigneeOptionsError ? <span className="field-error">{assigneeOptionsError}</span> : null}
                      {editForm.fieldErrors.assignedUserId ? (
                        <span className="field-error">{editForm.fieldErrors.assignedUserId}</span>
                      ) : null}
                    </>
                  ) : (
                    <strong>{selectedTask.assignedUserName ?? '-'}</strong>
                  )}
                </label>

                <div className="task-detail-side-item">
                  <span className="summary-label">作成者</span>
                  <strong>{selectedTask.createdByName ?? '-'}</strong>
                </div>

                <label className="task-detail-side-item">
                  <span className="summary-label">期限</span>
                  {isEditing ? (
                    <>
                      <input
                        className={editForm.fieldErrors.dueDate ? 'input-error' : ''}
                        form={DETAIL_FORM_ID}
                        onChange={(event) => editForm.onDueDateChange(event.target.value)}
                        type="date"
                        value={editForm.dueDate}
                      />
                      {editForm.fieldErrors.dueDate ? <span className="field-error">{editForm.fieldErrors.dueDate}</span> : null}
                    </>
                  ) : (
                    <strong>{formatDate(selectedTask.dueDate)}</strong>
                  )}
                </label>

                <div className="task-detail-side-item">
                  <span className="summary-label">作成日</span>
                  <strong>{formatDateTime(selectedTask.createdAt)}</strong>
                </div>

                <div className="task-detail-side-item">
                  <span className="summary-label">更新日</span>
                  <strong>{formatDateTime(selectedTask.updatedAt)}</strong>
                </div>
              </div>
            </aside>
          </div>
        )}
      </section>
    </TaskShell>
  )
}
