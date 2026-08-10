import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { useAuth } from '@/context/AuthContext'
import * as notificationsApi from '@/lib/notifications'
import type { Notification } from '@/types/api'

/**
 * Shared notification state.
 *
 * <p>Both the top bar (unread badge) and the dashboard panel read this. They
 * used to fetch independently, so marking everything read on the dashboard
 * updated that page's local copy while the header kept showing the stale
 * count until a full reload. One source of truth fixes that.
 */
interface NotificationContextValue {
  notifications: Notification[]
  unreadCount: number
  markAllRead: () => Promise<void>
}

const NotificationContext = createContext<NotificationContextValue | undefined>(undefined)

export function NotificationProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const [notifications, setNotifications] = useState<Notification[]>([])

  useEffect(() => {
    if (!user?.userId) {
      setNotifications([])
      return
    }
    notificationsApi.getAll(user.userId).then(setNotifications).catch(() => setNotifications([]))
  }, [user?.userId])

  const markAllRead = useCallback(async () => {
    if (!user?.userId) return
    await notificationsApi.markAllRead(user.userId)
    setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })))
  }, [user?.userId])

  const unreadCount = notifications.filter((n) => !n.isRead).length

  return (
    <NotificationContext.Provider value={{ notifications, unreadCount, markAllRead }}>
      {children}
    </NotificationContext.Provider>
  )
}

export function useNotifications() {
  const ctx = useContext(NotificationContext)
  if (!ctx) throw new Error('useNotifications must be used within a NotificationProvider')
  return ctx
}
