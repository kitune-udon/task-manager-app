import type { MouseEvent } from 'react'
import type { NotificationItem } from '../lib/notificationApi'
import { TaskShell } from '../components/TaskShell'
import { formatDateTime } from '../utils/format'

type Props = {
  activePath: string
  currentUserLabel: string
  onNavigate: (path: string) => void
  onLogout: () => void
  unreadCount: number
  notifications: NotificationItem[]
  unreadOnly: boolean
  currentPage: number
  totalPages: number
  totalElements: number
  isLoadingNotifications: boolean
  isMarkingAllRead: boolean
  activeNotificationId: number | null
  activeNotificationAction: 'open' | 'mark-read' | null
  notificationErrorMessage: string
  onUnreadOnlyChange: (value: boolean) => void
  onReload: () => void
  onPageChange: (page: number) => void
  onOpenNotification: (notification: NotificationItem) => void
  onMarkAsRead: (notification: NotificationItem) => void
  onMarkAllRead: () => void
}

function formatEventTypeLabel(eventType: string) {
  const labels: Record<string, string> = {
    TASK_CREATED: 'タスク追加',
    TASK_UPDATED: 'タスク更新',
    TASK_DELETED: 'タスク削除',
    COMMENT_CREATED: 'コメント追加',
    COMMENT_UPDATED: 'コメント更新',
    COMMENT_DELETED: 'コメント削除',
    ATTACHMENT_UPLOADED: '添付追加',
    ATTACHMENT_DELETED: '添付削除',
  }

  return labels[eventType] ?? eventType
}

export function NotificationPage({
  activePath,
  currentUserLabel,
  onNavigate,
  onLogout,
  unreadCount,
  notifications,
  unreadOnly,
  currentPage,
  totalPages,
  totalElements,
  isLoadingNotifications,
  isMarkingAllRead,
  activeNotificationId,
  activeNotificationAction,
  notificationErrorMessage,
  onUnreadOnlyChange,
  onReload,
  onPageChange,
  onOpenNotification,
  onMarkAsRead,
  onMarkAllRead,
}: Props) {
  const isEmpty = notifications.length === 0

  return (
    <TaskShell
      title="通知一覧"
      description="未読通知の確認や既読化、関連タスクへの遷移ができます。"
      activePath={activePath}
      onNavigate={onNavigate}
      onLogout={onLogout}
      currentUserLabel={currentUserLabel}
      unreadCount={unreadCount}
      actions={
        <>
          <button className="secondary-button" onClick={onReload} type="button">
            再読込
          </button>
          <button className="primary-button" disabled={isMarkingAllRead || unreadCount === 0} onClick={onMarkAllRead} type="button">
            {isMarkingAllRead ? '既読化中...' : '一括既読'}
          </button>
        </>
      }
    >
      {notificationErrorMessage ? <div className="status-box error-box">{notificationErrorMessage}</div> : null}

      <section className="panel section-panel">
        <div className="notification-toolbar">
          <label className="notification-toggle">
            <input checked={unreadOnly} onChange={(event) => onUnreadOnlyChange(event.target.checked)} type="checkbox" />
            <span>未読のみ表示</span>
          </label>
          <p className="notification-count-label">全 {totalElements} 件</p>
        </div>

        <div className="table-card">
          {isLoadingNotifications ? (
            <p className="empty-message padded-message">通知を読み込み中です...</p>
          ) : isEmpty ? (
            <p className="empty-message padded-message">{unreadOnly ? '未読通知はありません。' : '通知はありません。'}</p>
          ) : (
            <>
              <div className="table-scroll">
                <table>
                  <thead>
                    <tr>
                      <th>通知種別</th>
                      <th>既読状態</th>
                      <th>メッセージ</th>
                      <th>関連タスク</th>
                      <th>通知日時</th>
                      <th>操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    {notifications.map((notification) => {
                      const isActive = activeNotificationId === notification.id
                      const isOpening = isActive && activeNotificationAction === 'open'
                      const isMarkingRead = isActive && activeNotificationAction === 'mark-read'
                      const openLabel = `${notification.relatedTaskTitle ?? '関連タスク'}の詳細を開く`

                      return (
                        <tr
                          className={notification.isRead ? 'notification-row' : 'notification-row notification-row-unread'}
                          key={notification.id}
                        >
                          <td>
                            <button
                              aria-label={openLabel}
                              className="notification-row-button"
                              disabled={isActive}
                              onClick={() => void onOpenNotification(notification)}
                              type="button"
                            >
                              {formatEventTypeLabel(notification.eventType)}
                            </button>
                          </td>
                          <td>
                            <button
                              aria-label={openLabel}
                              className="notification-row-button"
                              disabled={isActive}
                              onClick={() => void onOpenNotification(notification)}
                              type="button"
                            >
                              <span className={notification.isRead ? 'badge badge-muted' : 'badge badge-attention'}>
                                {notification.isRead ? '既読' : '未読'}
                              </span>
                            </button>
                          </td>
                          <td>
                            <button
                              aria-label={openLabel}
                              className="notification-row-button"
                              disabled={isActive}
                              onClick={() => void onOpenNotification(notification)}
                              type="button"
                            >
                              {notification.message}
                            </button>
                          </td>
                          <td>
                            <button
                              aria-label={openLabel}
                              className="notification-row-button"
                              disabled={isActive}
                              onClick={() => void onOpenNotification(notification)}
                              type="button"
                            >
                              {notification.relatedTaskTitle ?? '-'}
                            </button>
                          </td>
                          <td>
                            <button
                              aria-label={openLabel}
                              className="notification-row-button"
                              disabled={isActive}
                              onClick={() => void onOpenNotification(notification)}
                              type="button"
                            >
                              {formatDateTime(notification.createdAt)}
                            </button>
                          </td>
                          <td>
                            <div className="notification-row-actions">
                              <button
                                className="table-action-button"
                                disabled={notification.isRead || isActive}
                                onClick={(event: MouseEvent<HTMLButtonElement>) => {
                                  event.stopPropagation()
                                  void onMarkAsRead(notification)
                                }}
                                type="button"
                              >
                                {isOpening || isMarkingRead ? '処理中...' : '既読にする'}
                              </button>
                            </div>
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>

              <div className="pagination-bar">
                <button
                  className="secondary-button"
                  disabled={currentPage <= 0}
                  onClick={() => onPageChange(currentPage - 1)}
                  type="button"
                >
                  前へ
                </button>
                <p className="pagination-label">
                  {totalPages === 0 ? '0 / 0' : `${currentPage + 1} / ${totalPages}`}
                </p>
                <button
                  className="secondary-button"
                  disabled={totalPages === 0 || currentPage >= totalPages - 1}
                  onClick={() => onPageChange(currentPage + 1)}
                  type="button"
                >
                  次へ
                </button>
              </div>
            </>
          )}
        </div>
      </section>
    </TaskShell>
  )
}
