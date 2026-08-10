import { useState, type FormEvent } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import { useSession } from '@/context/SessionContext'
import {
  CardsIcon,
  ChatIcon,
  CloseIcon,
  DashboardIcon,
  LibraryIcon,
  LogoIcon,
  MapIcon,
  PlusIcon,
  QuizIcon,
} from '@/components/icons'
import { UserMenu } from '@/components/UserMenu'

const navItems = [
  { to: '/dashboard', label: 'Dashboard', Icon: DashboardIcon },
  { to: '/documents', label: 'Library', Icon: LibraryIcon },
  { to: '/knowledge', label: 'Knowledge Map', Icon: MapIcon },
  { to: '/chat', label: 'Chat', Icon: ChatIcon },
  { to: '/quiz', label: 'Quiz', Icon: QuizIcon },
  { to: '/flashcards', label: 'Flashcards', Icon: CardsIcon },
]

export function Layout() {
  const { sessions, currentSessionId, setCurrentSessionId, createSession, deleteSession } = useSession()

  const [showNewSession, setShowNewSession] = useState(false)
  const [newTitle, setNewTitle] = useState('')
  const [deletingId, setDeletingId] = useState<string | null>(null)

  async function handleCreateSession(e: FormEvent) {
    e.preventDefault()
    if (!newTitle.trim()) return
    await createSession(newTitle.trim())
    setNewTitle('')
    setShowNewSession(false)
  }

  async function handleDeleteSession(id: string, title: string) {
    // Deleting a session also removes its documents, flashcards and the
    // mastery earned in it — worth spelling out before it happens.
    const confirmed = window.confirm(
      `Delete "${title}"?\n\nThis also deletes its uploaded documents, generated flashcards, ` +
        `and the topic mastery recorded in this session. This cannot be undone.`,
    )
    if (!confirmed) return

    setDeletingId(id)
    try {
      await deleteSession(id)
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <div className="flex min-h-screen bg-[#0a0a0c]">
      {/* ── Sidebar ─────────────────────────────────────────────── */}
      <aside className="fixed inset-y-0 left-0 flex w-64 flex-col border-r border-white/5 bg-[#0f0f12] px-4 py-5">
        <div className="flex items-center gap-2 px-2">
          <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-purple-500/15 text-purple-400">
            <LogoIcon />
          </span>
          <span className="text-lg font-semibold text-purple-400">GuruAI</span>
        </div>

        {!showNewSession ? (
          <button
            onClick={() => setShowNewSession(true)}
            className="mt-6 flex items-center justify-center gap-2 rounded-full bg-purple-500 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-purple-400"
          >
            <PlusIcon className="h-4 w-4" />
            New Study Session
          </button>
        ) : (
          <form onSubmit={handleCreateSession} className="mt-6 space-y-2">
            <input
              autoFocus
              type="text"
              value={newTitle}
              onChange={(e) => setNewTitle(e.target.value)}
              placeholder="Session name…"
              className="w-full rounded-lg border border-white/10 bg-[#17171c] px-3 py-2 text-sm text-slate-200 placeholder:text-slate-500 focus:border-purple-500 focus:outline-none"
            />
            <div className="flex gap-2">
              <button
                type="submit"
                disabled={!newTitle.trim()}
                className="flex-1 rounded-lg bg-purple-500 px-3 py-1.5 text-xs font-semibold text-white hover:bg-purple-400 disabled:opacity-40"
              >
                Create
              </button>
              <button
                type="button"
                onClick={() => setShowNewSession(false)}
                className="rounded-lg border border-white/10 px-3 py-1.5 text-xs text-slate-400 hover:bg-white/5"
              >
                Cancel
              </button>
            </div>
          </form>
        )}

        <nav className="mt-6 space-y-1">
          {navItems.map(({ to, label, Icon }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition ${
                  isActive
                    ? 'bg-purple-500/10 text-purple-300'
                    : 'text-slate-400 hover:bg-white/5 hover:text-slate-200'
                }`
              }
            >
              <Icon />
              {label}
            </NavLink>
          ))}
        </nav>

        {sessions.length > 0 && (
          <div className="mt-8 min-h-0 flex-1 overflow-y-auto">
            <p className="px-3 text-xs font-semibold tracking-wider text-slate-500">SESSIONS</p>
            <ul className="mt-2 space-y-0.5">
              {sessions.map((s) => (
                <li key={s.id} className="group/session flex items-center">
                  <button
                    onClick={() => setCurrentSessionId(s.id)}
                    className={`min-w-0 flex-1 truncate rounded-md px-3 py-1.5 text-left text-sm transition ${
                      s.id === currentSessionId
                        ? 'text-purple-300'
                        : 'text-slate-500 hover:bg-white/5 hover:text-slate-300'
                    }`}
                  >
                    {s.title}
                  </button>
                  <button
                    onClick={() => void handleDeleteSession(s.id, s.title)}
                    disabled={deletingId === s.id}
                    title="Delete session"
                    aria-label={`Delete session ${s.title}`}
                    className="mr-1 shrink-0 rounded p-1 text-slate-600 opacity-0 transition hover:bg-rose-500/15 hover:text-rose-300 focus:opacity-100 group-hover/session:opacity-100 disabled:opacity-40"
                  >
                    <CloseIcon className="h-3.5 w-3.5" />
                  </button>
                </li>
              ))}
            </ul>
          </div>
        )}
      </aside>

      {/* ── Main column ─────────────────────────────────────────── */}
      <div className="ml-64 flex min-h-screen flex-1 flex-col">
        <header className="flex items-center justify-end border-b border-white/5 px-8 py-3">
          <UserMenu />
        </header>
        <main className="flex-1 px-8 py-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
