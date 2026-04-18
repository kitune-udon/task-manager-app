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

type Params = {
  isLoggedIn: boolean
  go: (path: string, replace?: boolean) => void
  routeKey: string
}

type ActiveNotificationAction = 'open' | 'mark-read' | null

function mapNotificationOpenError(error: unknown) {
  const code = extractApiErrorCode(error)

  if (code === 'ERR-TASK-004') {
    return '関連タスクは削除済みか、参照できなくなりました。'
  }

  if (code === 'ERR-AUTH-005') {
    return '関連タスクを参照する権限がありません。'
  }

  return resolveUserMessage(error)
}

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

  const loadNotifications = useCallback(async (nextPage = currentPage, nextUnreadOnly = unreadOnly) => {
    setIsLoadingNotifications(true)
    setNotificationErrorMessage('')

    try {
      const page = await fetchNotifications(nextUnreadOnly, nextPage, PAGE_SIZE)
      const nextNotifications = Array.isArray(page.content) ? page.content : []
      const resolvedTotalPages = page.totalPages ?? 0

      if (nextPage > 0 && nextNotifications.length === 0 && resolvedTotalPages > 0 && nextPage >= resolvedTotalPages) {
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
        setNotificationErrorMessage('関連タスクを特定できませんでした。')
        await loadNotifications(currentPage, unreadOnly)
        return
      }

      await fetchTaskById(nextNotification.relatedTaskId)
      go(`/tasks/${nextNotification.relatedTaskId}`)
    } catch (error) {
      setNotificationErrorMessage(mapNotificationOpenError(error))
      await Promise.all([loadUnreadCount(), loadNotifications(currentPage, unreadOnly)])
    } finally {
      setActiveNotificationId(null)
      setActiveNotificationAction(null)
    }
  }

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

  const handleUnreadOnlyChange = (value: boolean) => {
    setUnreadOnly(value)
    setCurrentPage(0)
  }

  const handlePageChange = (page: number) => {
    if (page < 0 || (totalPages > 0 && page >= totalPages) || page === currentPage) {
      return
    }

    setCurrentPage(page)
  }

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
