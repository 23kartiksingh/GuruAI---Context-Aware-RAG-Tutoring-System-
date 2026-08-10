import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import { useNotifications } from '@/context/NotificationContext'
import { BellIcon, ChevronDownIcon } from '@/components/icons'

/**
 * Avatar + name in the top bar; click opens account details and actions.
 */
export function UserMenu() {
  const { user, logout } = useAuth()
  const { unreadCount } = useNotifications()
  const [open, setOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  // Close when clicking anywhere outside the menu.
  useEffect(() => {
    if (!open) return
    function onClickOutside(e: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', onClickOutside)
    return () => document.removeEventListener('mousedown', onClickOutside)
  }, [open])

  const displayName = user?.name || user?.username || 'Account'
  const initial = displayName.charAt(0).toUpperCase()

  return (
    <div ref={menuRef} className="relative">
      <button
        onClick={() => setOpen((v) => !v)}
        className="flex items-center gap-2.5 rounded-full py-1 pl-1 pr-3 transition hover:bg-white/5"
      >
        <span className="relative flex h-8 w-8 items-center justify-center rounded-full bg-gradient-to-br from-purple-500 to-cyan-400 text-sm font-semibold text-white">
          {initial}
          {unreadCount > 0 && (
            <span className="absolute -right-0.5 -top-0.5 h-2.5 w-2.5 rounded-full bg-purple-400 ring-2 ring-[#0a0a0c]" />
          )}
        </span>
        <span className="text-sm font-medium text-slate-300">{displayName}</span>
        <ChevronDownIcon className={`h-4 w-4 text-slate-500 transition ${open ? 'rotate-180' : ''}`} />
      </button>

      {open && (
        <div className="absolute right-0 z-20 mt-2 w-60 overflow-hidden rounded-xl border border-white/10 bg-[#17171c] shadow-xl shadow-black/40">
          <div className="flex items-center gap-3 border-b border-white/5 px-4 py-3">
            <span className="flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-br from-purple-500 to-cyan-400 font-semibold text-white">
              {initial}
            </span>
            <div className="min-w-0">
              <p className="truncate text-sm font-medium text-slate-200">{displayName}</p>
              {user?.username && <p className="truncate text-xs text-slate-500">@{user.username}</p>}
            </div>
          </div>

          <div className="p-1.5">
            <Link
              to="/dashboard"
              onClick={() => setOpen(false)}
              className="flex items-center justify-between rounded-lg px-3 py-2 text-sm text-slate-300 transition hover:bg-white/5"
            >
              <span className="flex items-center gap-2.5">
                <BellIcon className="h-4 w-4 text-slate-500" />
                Notifications
              </span>
              {unreadCount > 0 && (
                <span className="rounded-full bg-purple-500 px-1.5 text-[10px] font-semibold text-white">
                  {unreadCount}
                </span>
              )}
            </Link>

            <button
              onClick={() => void logout()}
              className="mt-0.5 flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-left text-sm text-rose-300 transition hover:bg-rose-500/10"
            >
              <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                <path d="m16 17 5-5-5-5M21 12H9" />
              </svg>
              Log out
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
