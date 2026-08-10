import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import { useSession } from '@/context/SessionContext'
import * as knowledgeApi from '@/lib/knowledge'
import * as memoryApi from '@/lib/memory'
import { useNotifications } from '@/context/NotificationContext'
import type { MemoryItem, Notification, UserStats } from '@/types/api'

export default function Dashboard() {
  const { user } = useAuth()
  const { notifications, unreadCount, markAllRead } = useNotifications()
  const { setCurrentSessionId } = useSession()
  const navigate = useNavigate()

  // Weak-topic reminders carry a session (and topic) to jump straight back
  // into — other notification types have nothing to link to, so they just
  // stay a plain list item.
  function handleNotificationClick(n: Notification) {
    if (!n.sessionId) return
    setCurrentSessionId(n.sessionId)
    navigate(n.topic ? `/chat?topic=${encodeURIComponent(n.topic)}` : '/chat')
  }

  const [stats, setStats] = useState<UserStats | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!user?.userId) return
    knowledgeApi
      .getStats(user.userId)
      .then(setStats)
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load dashboard'))
      .finally(() => setIsLoading(false))
  }, [user?.userId])

  // ── "About You" preferences — common to every session/chat, so loaded and
  // saved independently of the stats above (a failure here shouldn't block
  // the rest of the dashboard, and vice versa).
  const [memoryItems, setMemoryItems] = useState<MemoryItem[]>([])
  const [memoryDraft, setMemoryDraft] = useState('')
  const [memoryError, setMemoryError] = useState('')
  const [isSavingMemory, setIsSavingMemory] = useState(false)
  const [isClearingMemory, setIsClearingMemory] = useState(false)

  // Inline edit state — which item (if any) is currently being edited, and
  // its in-progress text. Only one at a time keeps this simple.
  const [editingId, setEditingId] = useState<string | null>(null)
  const [editDraft, setEditDraft] = useState('')
  const [isSavingEdit, setIsSavingEdit] = useState(false)
  const [deletingId, setDeletingId] = useState<string | null>(null)

  useEffect(() => {
    if (!user?.userId) return
    memoryApi
      .getMemory(user.userId)
      .then((res) => setMemoryItems(res.items))
      .catch(() => {}) // non-critical — the "About You" card just starts empty
  }, [user?.userId])

  async function handleAddPreference(e: FormEvent) {
    e.preventDefault()
    if (!user?.userId || !memoryDraft.trim()) return
    setMemoryError('')
    setIsSavingMemory(true)
    try {
      const res = await memoryApi.addMemory(user.userId, memoryDraft.trim())
      setMemoryItems(res.items)
      setMemoryDraft('')
    } catch (err) {
      setMemoryError(err instanceof Error ? err.message : 'Failed to save')
    } finally {
      setIsSavingMemory(false)
    }
  }

  function startEditing(item: MemoryItem) {
    setEditingId(item.id)
    setEditDraft(item.text)
    setMemoryError('')
  }

  async function handleSaveEdit(itemId: string) {
    if (!user?.userId || !editDraft.trim()) return
    setMemoryError('')
    setIsSavingEdit(true)
    try {
      const res = await memoryApi.updateMemoryItem(user.userId, itemId, editDraft.trim())
      setMemoryItems(res.items)
      setEditingId(null)
    } catch (err) {
      setMemoryError(err instanceof Error ? err.message : 'Failed to save')
    } finally {
      setIsSavingEdit(false)
    }
  }

  async function handleDeleteItem(itemId: string) {
    if (!user?.userId) return
    setMemoryError('')
    setDeletingId(itemId)
    try {
      const res = await memoryApi.deleteMemoryItem(user.userId, itemId)
      setMemoryItems(res.items)
    } catch (err) {
      setMemoryError(err instanceof Error ? err.message : 'Failed to remove')
    } finally {
      setDeletingId(null)
    }
  }

  async function handleClearMemory() {
    if (!user?.userId) return
    setMemoryError('')
    setIsClearingMemory(true)
    try {
      await memoryApi.clearMemory(user.userId)
      setMemoryItems([])
    } catch (err) {
      setMemoryError(err instanceof Error ? err.message : 'Failed to clear')
    } finally {
      setIsClearingMemory(false)
    }
  }

  if (isLoading) return <p className="text-slate-500">Loading dashboard…</p>

  const accuracy =
    stats && stats.totalQuestions > 0
      ? Math.round((stats.correctAnswers / stats.totalQuestions) * 100)
      : 0

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-semibold text-white">
          Welcome back, {user?.name || user?.username}
        </h1>
        <p className="mt-1 text-sm text-slate-500">Your learning progress across every session.</p>
      </div>

      {error && <p className="text-sm text-red-400">{error}</p>}

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-xl border border-white/5 bg-[#121215] p-5">
          <p className="text-xs font-semibold tracking-wider text-slate-500">QUESTIONS ANSWERED</p>
          <p className="mt-2 text-3xl font-bold text-purple-400">{stats?.totalQuestions ?? 0}</p>
        </div>
        <div className="rounded-xl border border-white/5 bg-[#121215] p-5">
          <p className="text-xs font-semibold tracking-wider text-slate-500">ANSWER ACCURACY</p>
          <p className="mt-2 text-3xl font-bold text-cyan-300">{accuracy}%</p>
          <p className="mt-1 text-xs text-slate-500">
            {stats?.correctAnswers ?? 0} of {stats?.totalQuestions ?? 0} correct
          </p>
        </div>
        <div className="rounded-xl border border-white/5 bg-[#121215] p-5">
          <p className="text-xs font-semibold tracking-wider text-slate-500">AVERAGE MASTERY</p>
          <p className="mt-2 text-3xl font-bold text-emerald-300">
            {stats ? `${stats.avgMasteryPct.toFixed(0)}%` : '0%'}
          </p>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <div className="rounded-xl border border-white/5 bg-[#121215] p-5">
          <div className="flex items-center justify-between">
            <h2 className="font-semibold text-white">Knowledge Map</h2>
            <Link to="/knowledge" className="text-xs font-medium text-purple-400 hover:underline">
              View all
            </Link>
          </div>
          <p className="mt-4 text-sm text-slate-500">
            Your average mastery is {stats ? `${stats.avgMasteryPct.toFixed(0)}%` : '0%'}. See the
            full strong/average/weak breakdown by topic on the Knowledge Map.
          </p>
        </div>

        <div className="rounded-xl border border-white/5 bg-[#121215] p-5">
          <div className="flex items-center justify-between">
            <h2 className="font-semibold text-white">
              Notifications {unreadCount > 0 && <span className="text-purple-400">({unreadCount})</span>}
            </h2>
            {unreadCount > 0 && (
              <button
                onClick={() => void markAllRead()}
                className="text-xs font-medium text-purple-400 hover:underline"
              >
                Mark all read
              </button>
            )}
          </div>
          {notifications.length === 0 ? (
            <p className="mt-4 text-sm text-slate-500">No notifications yet.</p>
          ) : (
            <>
              {/* Capped height with its own scrollbar: the full list can run to
                  dozens of entries, which used to stretch the page far past the
                  panel beside it. ~6 rows fit before it starts scrolling. */}
              <ul className="mt-4 max-h-96 space-y-2 overflow-y-auto pr-1">
                {notifications.map((n) => {
                  const clickable = !!n.sessionId
                  return (
                    <li
                      key={n.id}
                      onClick={clickable ? () => handleNotificationClick(n) : undefined}
                      className={`rounded-lg border p-3 ${
                        n.isRead ? 'border-white/5 bg-white/[0.02]' : 'border-purple-500/20 bg-purple-500/5'
                      } ${clickable ? 'cursor-pointer transition hover:border-purple-500/30' : ''}`}
                    >
                      <p className="text-sm font-medium text-slate-200">{n.title}</p>
                      <p className="mt-0.5 text-xs text-slate-400">{n.message}</p>
                      {clickable && (
                        <p className="mt-1 text-[11px] font-medium text-purple-400">
                          Go to session →
                        </p>
                      )}
                    </li>
                  )
                })}
              </ul>
              {notifications.length > 6 && (
                <p className="mt-3 text-center text-xs text-slate-600">
                  {notifications.length} notifications · scroll for more
                </p>
              )}
            </>
          )}
        </div>
      </div>

      <div className="rounded-xl border border-white/5 bg-[#121215] p-5">
        <h2 className="font-semibold text-white">About You</h2>
        <p className="mt-1 text-sm text-slate-500">
          Tell your tutor a bit about yourself — interests, the languages you code in, how you
          like things explained. It's applied to every session and chat, not just this one, until
          you change it.
        </p>

        <form onSubmit={handleAddPreference} className="mt-4 flex gap-2">
          <input
            type="text"
            value={memoryDraft}
            onChange={(e) => setMemoryDraft(e.target.value)}
            placeholder="e.g. I'm a huge anime fan and I mostly code in Java"
            className="min-w-0 flex-1 rounded-lg border border-white/10 bg-[#17171c] px-3 py-2 text-sm text-slate-200 placeholder:text-slate-600 focus:border-purple-500 focus:outline-none"
          />
          <button
            type="submit"
            disabled={isSavingMemory || !memoryDraft.trim()}
            className="shrink-0 rounded-lg bg-purple-500 px-4 py-2 text-sm font-semibold text-white transition hover:bg-purple-400 disabled:opacity-40"
          >
            {isSavingMemory ? 'Saving…' : 'Add'}
          </button>
        </form>

        {memoryError && <p className="mt-2 text-sm text-rose-400">{memoryError}</p>}

        {memoryItems.length > 0 && (
          <ul className="mt-4 space-y-1.5">
            {memoryItems.map((item) =>
              editingId === item.id ? (
                <li key={item.id} className="flex items-center gap-2">
                  <input
                    type="text"
                    autoFocus
                    value={editDraft}
                    onChange={(e) => setEditDraft(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') void handleSaveEdit(item.id)
                      if (e.key === 'Escape') setEditingId(null)
                    }}
                    className="min-w-0 flex-1 rounded-lg border border-purple-500/40 bg-[#17171c] px-2.5 py-1.5 text-sm text-slate-200 focus:outline-none"
                  />
                  <button
                    onClick={() => void handleSaveEdit(item.id)}
                    disabled={isSavingEdit || !editDraft.trim()}
                    className="shrink-0 text-xs font-medium text-purple-400 hover:underline disabled:opacity-40"
                  >
                    Save
                  </button>
                  <button
                    onClick={() => setEditingId(null)}
                    className="shrink-0 text-xs font-medium text-slate-500 hover:text-slate-300"
                  >
                    Cancel
                  </button>
                </li>
              ) : (
                <li
                  key={item.id}
                  className="group flex items-start justify-between gap-2 text-sm text-slate-300"
                >
                  <span className="flex items-start gap-2">
                    <span className="mt-1.5 h-1 w-1 shrink-0 rounded-full bg-purple-400" />
                    {item.text}
                  </span>
                  <span className="flex shrink-0 items-center gap-2 opacity-0 transition group-hover:opacity-100">
                    <button
                      onClick={() => startEditing(item)}
                      title="Edit"
                      className="text-xs font-medium text-slate-500 hover:text-purple-400"
                    >
                      Edit
                    </button>
                    <button
                      onClick={() => void handleDeleteItem(item.id)}
                      disabled={deletingId === item.id}
                      title="Remove"
                      className="text-xs font-medium text-slate-500 hover:text-rose-400 disabled:opacity-40"
                    >
                      ✕
                    </button>
                  </span>
                </li>
              ),
            )}
          </ul>
        )}

        {memoryItems.length > 0 && (
          <button
            onClick={() => void handleClearMemory()}
            disabled={isClearingMemory}
            className="mt-3 text-xs font-medium text-slate-500 hover:text-rose-400 disabled:opacity-40"
          >
            {isClearingMemory ? 'Clearing…' : 'Clear everything'}
          </button>
        )}
      </div>
    </div>
  )
}
