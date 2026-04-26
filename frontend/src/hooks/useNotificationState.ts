import { useCallback, useEffect, useRef, useState } from 'react'
import { extractApiErrorCode, resolveUserMessage } from '../lib/apiError'
import {
  fetchTaskById,
} from '../lib/taskApi'
import {
  fetchNotifications,
  fetchUnreadCount,
  markAllNotificationsAsRead,
  markNotificationAsRead,
  type NotificationItem,
} from '../lib/notificationApi'

const PAGE_SIZE = 20
const ACTIVE_POLLING_INTERVAL_MS = 60000
const INACTIVE_POLLING_INTERVAL_MS = 180000

/**
 * 通知状態hookが必要とする認証状態、画面遷移、現在ルートの識別子。
 */
type Params = {
  isLoggedIn: boolean
  go: (path: string, replace?: boolean) => void
  routeKey: string
}

type ActiveNotificationAction = 'open' | 'mark-read' | null

/**
 * 通知クリック時に関連タスクを開けなかった理由を、ユーザー向けメッセージへ変換する。
 */
function mapNotificationOpenError(error: unknown) {
  const code = extractApiErrorCode(error)

  if (code === 'ERR-TASK-004') {
    return 'アクセスできないか、削除されています'
  }

  if (code === 'ERR-AUTH-005' || code === 'ERR-TASK-009') {
    return 'アクセスできないか、削除されています'
  }

  return resolveUserMessage(error)
}

/**
 * 通知一覧、未読件数、既読化、通知クリック時の関連タスク遷移をまとめて管理する。
 */
export function useNotificationState({ isLoggedIn, go, routeKey }: Params) {
  const [notifications, setNotifications] = useState<NotificationItem[]>([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [unreadOnly, setUnreadOnly] = useState(false)
  const [currentPage, setCurrentPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [isLoadingNotifications, setIsLoadingNotifications] = useState(false)
  const [isMarkingAllRead, setIsMarkingAllRead] = useState(false)
  const [activeNotificationId, setActiveNotificationId] = useState<number | null>(null)
  const [activeNotificationAction, setActiveNotificationAction] = useState<ActiveNotificationAction>(null)
  const [notificationErrorMessage, setNotificationErrorMessage] = useState('')
  const [isFetchingUnreadCount, setIsFetchingUnreadCount] = useState(false)
  const [isDocumentVisible, setIsDocumentVisible] = useState(() =>
    typeof document === 'undefined' ? true : document.visibilityState !== 'hidden',
  )
  const unreadCountRequestRef = useRef<Promise<void> | null>(null)

  /**
   * 既読化した通知を現在の一覧状態へ反映する。
   */
  const applyReadState = (updatedNotification: NotificationItem, previousNotification: NotificationItem) => {
    setNotifications((current) => {
      if (unreadOnly && !previousNotification.isRead) {
        return current.filter((notification) => notification.id !== updatedNotification.id)
      }

      return current.map((notification) =>
        notification.id === updatedNotification.id ? updatedNotification : notification,
      )
    })

    if (unreadOnly && !previousNotification.isRead) {
      setTotalElements((current) => Math.max(0, current - 1))
    }
  }

  /**
   * 未読件数を取得する。ポーリングでは進行中リクエストがあれば新規取得を省略できる。
   */
  const loadUnreadCount = useCallback(async ({ skipIfFetching = false }: { skipIfFetching?: boolean } = {}) => {
    if (unreadCountRequestRef.current) {
      if (skipIfFetching) {
        return
      }

      await unreadCountRequestRef.current
      return
    }

    const request = (async () => {
      setIsFetchingUnreadCount(true)

      try {
        const count = await fetchUnreadCount()
        setUnreadCount(count)
      } catch {
        // Keep the previous unread count and avoid surfacing a page-level error for badge polling.
      } finally {
        unreadCountRequestRef.current = null
        setIsFetchingUnreadCount(false)
      }
    })()

    unreadCountRequestRef.current = request
    await request
  }, [])

  /**
   * 通知一覧を取得し、ページング情報を更新する。
   */
  const loadNotifications = useCallback(async (nextPage = currentPage, nextUnreadOnly = unreadOnly) => {
    setIsLoadingNotifications(true)
    setNotificationErrorMessage('')

    try {
      const page = await fetchNotifications(nextUnreadOnly, nextPage, PAGE_SIZE)
      const nextNotifications = Array.isArray(page.content) ? page.content : []
      const resolvedTotalPages = page.totalPages ?? 0

      if (nextPage > 0 && nextNotifications.length === 0 && resolvedTotalPages > 0 && nextPage >= resolvedTotalPages) {
        // 一括既読などで現在ページが空になった場合は、存在する最後のページへ戻す。
        setCurrentPage(resolvedTotalPages - 1)
        return
      }

      setNotifications(nextNotifications)
      setCurrentPage(page.page ?? nextPage)
      setTotalPages(resolvedTotalPages)
      setTotalElements(page.totalElements ?? nextNotifications.length)
    } catch (error) {
      setNotifications([])
      setTotalPages(0)
      setTotalElements(0)
      setNotificationErrorMessage(resolveUserMessage(error))
    } finally {
      setIsLoadingNotifications(false)
    }
  }, [currentPage, unreadOnly])

  const loadUnreadCountRef = useRef(loadUnreadCount)

  useEffect(() => {
    loadUnreadCountRef.current = loadUnreadCount
  }, [loadUnreadCount])

  useEffect(() => {
    if (!isLoggedIn) {
      // ログアウト後に前ユーザーの通知状態や進行中扱いを残さない。
      setNotifications([])
      setUnreadCount(0)
      setUnreadOnly(false)
      setCurrentPage(0)
      setTotalPages(0)
      setTotalElements(0)
      setNotificationErrorMessage('')
      setIsLoadingNotifications(false)
      setIsMarkingAllRead(false)
      setIsFetchingUnreadCount(false)
      setIsDocumentVisible(true)
      setActiveNotificationId(null)
      setActiveNotificationAction(null)
      unreadCountRequestRef.current = null
      return
    }

    const intervalMs = isDocumentVisible ? ACTIVE_POLLING_INTERVAL_MS : INACTIVE_POLLING_INTERVAL_MS
    // 表示中は短め、非表示中は長めの間隔で未読バッジだけ更新する。
    const timerId = window.setInterval(() => {
      void loadUnreadCount({ skipIfFetching: true })
    }, intervalMs)

    return () => {
      window.clearInterval(timerId)
    }
  }, [isDocumentVisible, isLoggedIn, loadUnreadCount])

  useEffect(() => {
    if (!isLoggedIn || typeof document === 'undefined') {
      return
    }

    const handleVisibilityChange = () => {
      const nextIsVisible = document.visibilityState !== 'hidden'
      setIsDocumentVisible(nextIsVisible)

      if (nextIsVisible) {
        // タブ復帰時は次回ポーリングを待たずにバッジを更新する。
        void loadUnreadCountRef.current()
      }
    }

    document.addEventListener('visibilitychange', handleVisibilityChange)

    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange)
    }
  }, [isLoggedIn])

  useEffect(() => {
    if (!isLoggedIn) {
      return
    }

    void loadUnreadCount()
  }, [isLoggedIn, loadUnreadCount, routeKey])

  useEffect(() => {
    if (!isLoggedIn) {
      return
    }

    void loadNotifications()
  }, [isLoggedIn, loadNotifications])

  /**
   * 通知を開き、必要なら既読化してから関連タスクへ遷移する。
   */
  const handleOpenNotification = async (notification: NotificationItem) => {
    setActiveNotificationId(notification.id)
    setActiveNotificationAction('open')
    setNotificationErrorMessage('')

    try {
      let nextNotification = notification
      if (!notification.isRead) {
        nextNotification = await markNotificationAsRead(notification.id)
        applyReadState(nextNotification, notification)
      }
      await loadUnreadCount()

      if (!nextNotification.relatedTaskId) {
        await loadNotifications(currentPage, unreadOnly)
        setNotificationErrorMessage('関連タスクを特定できませんでした。')
        return
      }

      const relatedTask = await fetchTaskById(nextNotification.relatedTaskId)
      // 遷移前に関連タスクを取得し、削除済みや権限不足をこの画面でメッセージ化する。
      const params = new URLSearchParams({ from: 'notifications' })
      if (relatedTask?.teamId) {
        params.set('teamId', String(relatedTask.teamId))
      }
      go(`/tasks/${nextNotification.relatedTaskId}?${params.toString()}`)
    } catch (error) {
      const message = mapNotificationOpenError(error)
      await Promise.all([loadUnreadCount(), loadNotifications(currentPage, unreadOnly)])
      setNotificationErrorMessage(message)
    } finally {
      setActiveNotificationId(null)
      setActiveNotificationAction(null)
    }
  }

  /**
   * 指定通知を既読にする。
   */
  const handleMarkAsRead = async (notification: NotificationItem) => {
    if (notification.isRead) {
      return
    }

    setActiveNotificationId(notification.id)
    setActiveNotificationAction('mark-read')
    setNotificationErrorMessage('')

    try {
      const updatedNotification = await markNotificationAsRead(notification.id)
      applyReadState(updatedNotification, notification)
      await loadUnreadCount()
    } catch (error) {
      setNotificationErrorMessage(resolveUserMessage(error))
    } finally {
      setActiveNotificationId(null)
      setActiveNotificationAction(null)
    }
  }

  /**
   * すべての通知を既読にし、未読件数と一覧を再取得する。
   */
  const handleMarkAllRead = async () => {
    setIsMarkingAllRead(true)
    setNotificationErrorMessage('')

    try {
      await markAllNotificationsAsRead()
      await Promise.all([loadUnreadCount(), loadNotifications(currentPage, unreadOnly)])
    } catch (error) {
      setNotificationErrorMessage(resolveUserMessage(error))
    } finally {
      setIsMarkingAllRead(false)
    }
  }

  /**
   * 未読のみ表示の切り替え時に先頭ページへ戻す。
   */
  const handleUnreadOnlyChange = (value: boolean) => {
    setUnreadOnly(value)
    setCurrentPage(0)
  }

  /**
   * 有効範囲内のページ番号へ移動する。
   */
  const handlePageChange = (page: number) => {
    if (page < 0 || (totalPages > 0 && page >= totalPages) || page === currentPage) {
      return
    }

    setCurrentPage(page)
  }

  /**
   * 通知関連の状態を初期化する。
   */
  const clearNotificationState = () => {
    setNotifications([])
    setUnreadCount(0)
    setUnreadOnly(false)
    setCurrentPage(0)
    setTotalPages(0)
    setTotalElements(0)
    setIsLoadingNotifications(false)
    setIsMarkingAllRead(false)
    setIsFetchingUnreadCount(false)
    setIsDocumentVisible(true)
    setActiveNotificationId(null)
    setActiveNotificationAction(null)
    setNotificationErrorMessage('')
    unreadCountRequestRef.current = null
  }

  return {
    notifications,
    unreadCount,
    unreadOnly,
    currentPage,
    totalPages,
    totalElements,
    isLoadingNotifications,
    isMarkingAllRead,
    isFetchingUnreadCount,
    activeNotificationId,
    activeNotificationAction,
    notificationErrorMessage,
    actions: {
      loadNotifications,
      loadUnreadCount,
      handleUnreadOnlyChange,
      handlePageChange,
      handleOpenNotification,
      handleMarkAsRead,
      handleMarkAllRead,
      clearNotificationState,
    },
  }
}
