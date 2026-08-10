import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { useAuth } from '@/context/AuthContext'
import * as sessionsApi from '@/lib/sessions'
import type { StudySession } from '@/types/api'

// Every study feature (documents, chat, quiz, flashcards) is scoped to a
// "study session". This context holds the list of the user's sessions and
// which one is currently selected, so that choice is shared across pages
// instead of every page re-fetching and re-picking independently.
interface SessionContextValue {
  sessions: StudySession[]
  currentSessionId: string | null
  isLoading: boolean
  setCurrentSessionId: (id: string) => void
  createSession: (title: string, subject?: string) => Promise<void>
  deleteSession: (id: string) => Promise<void>
  refreshSessions: () => Promise<void>
}

const SessionContext = createContext<SessionContextValue | undefined>(undefined)

export function SessionProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const [sessions, setSessions] = useState<StudySession[]>([])
  const [currentSessionId, setCurrentSessionId] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const refreshSessions = useCallback(async () => {
    if (!user?.userId) return
    const list = await sessionsApi.listSessions(user.userId)
    setSessions(list)
    // Keep the current selection if it still exists, otherwise default to
    // the most recently created session.
    setCurrentSessionId((prev) => {
      if (prev && list.some((s) => s.id === prev)) return prev
      return list[0]?.id ?? null
    })
  }, [user?.userId])

  useEffect(() => {
    if (!user?.userId) {
      setIsLoading(false)
      return
    }
    setIsLoading(true)
    refreshSessions().finally(() => setIsLoading(false))
  }, [user?.userId, refreshSessions])

  const createSession = useCallback(
    async (title: string, subject?: string) => {
      if (!user?.userId) return
      const created = await sessionsApi.createSession({ userId: user.userId, title, subject })
      setSessions((prev) => [created, ...prev])
      setCurrentSessionId(created.id)
    },
    [user?.userId],
  )

  const deleteSession = useCallback(
    async (id: string) => {
      await sessionsApi.deleteSession(id)
      setSessions((prev) => {
        const remaining = prev.filter((s) => s.id !== id)
        // If the deleted session was selected, fall back to the next one.
        setCurrentSessionId((current) => (current === id ? remaining[0]?.id ?? null : current))
        return remaining
      })
    },
    [],
  )

  return (
    <SessionContext.Provider
      value={{
        sessions,
        currentSessionId,
        isLoading,
        setCurrentSessionId,
        createSession,
        deleteSession,
        refreshSessions,
      }}
    >
      {children}
    </SessionContext.Provider>
  )
}

export function useSession() {
  const ctx = useContext(SessionContext)
  if (!ctx) throw new Error('useSession must be used within a SessionProvider')
  return ctx
}
