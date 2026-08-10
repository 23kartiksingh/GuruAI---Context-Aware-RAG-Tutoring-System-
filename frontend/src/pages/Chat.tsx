import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import { useSession } from '@/context/SessionContext'
import * as chatApi from '@/lib/chat'
import * as documentsApi from '@/lib/documents'
import * as knowledgeApi from '@/lib/knowledge'
import type { ChatMessage, MasteryLevel, StudyDocument, TopicMastery } from '@/types/api'
import { DocIcon, QuizIcon, SendIcon, SparkIcon } from '@/components/icons'
import { MarkdownMessage } from '@/components/MarkdownMessage'

// Mastery level → bar colour + the label shown under the topic name.
const masteryMeta: Record<MasteryLevel, { bar: string; text: string; label: string }> = {
  STRONG: { bar: 'bg-emerald-400', text: 'text-emerald-400', label: 'MASTERY HIGH' },
  AVERAGE: { bar: 'bg-purple-400', text: 'text-purple-400', label: 'PROGRESSING' },
  WEAK: { bar: 'bg-rose-400', text: 'text-rose-400', label: 'FOCUS REQUIRED' },
}

export default function Chat() {
  const { user } = useAuth()
  const { sessions, currentSessionId } = useSession()
  const currentSession = sessions.find((s) => s.id === currentSessionId)

  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [draft, setDraft] = useState('')
  const [isSending, setIsSending] = useState(false)
  const [error, setError] = useState('')
  const [lastUsedRag, setLastUsedRag] = useState<boolean | null>(null)

  const [documents, setDocuments] = useState<StudyDocument[]>([])
  const [topics, setTopics] = useState<TopicMastery[]>([])

  // Set when arriving from a "let's revise X" notification click — shows a
  // one-time prompt banner rather than a fake injected chat message, since
  // that would pollute real conversation history.
  const [searchParams, setSearchParams] = useSearchParams()
  const revisitTopic = searchParams.get('topic')

  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!currentSessionId) {
      setMessages([])
      setDocuments([])
      return
    }
    chatApi
      .getHistory(currentSessionId)
      .then(setMessages)
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load chat history'))
    documentsApi.listDocuments(currentSessionId).then(setDocuments).catch(() => setDocuments([]))
  }, [currentSessionId])

  useEffect(() => {
    if (!user?.userId) return
    knowledgeApi
      .getMasteryProfile(user.userId)
      .then((p) => setTopics(p.topics))
      .catch(() => setTopics([]))
  }, [user?.userId])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  async function handleSend(e: FormEvent) {
    e.preventDefault()
    const text = draft.trim()
    if (!text || !currentSessionId || !user) return

    // Show the user's message right away rather than waiting on the round
    // trip; the reply is appended when it arrives.
    setMessages((prev) => [
      ...prev,
      { id: `local-${Date.now()}`, role: 'user', content: text, createdAt: new Date().toISOString() },
    ])
    setDraft('')
    setError('')
    setIsSending(true)

    try {
      const response = await chatApi.sendChat({
        userId: user.userId,
        sessionId: currentSessionId,
        message: text,
      })
      setLastUsedRag(response.usedRag)
      setMessages((prev) => [
        ...prev,
        {
          id: response.messageId,
          role: 'assistant',
          content: response.reply,
          createdAt: response.timestamp,
        },
      ])
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to send message')
    } finally {
      setIsSending(false)
    }
  }

  if (!currentSessionId) {
    return (
      <div className="rounded-xl border border-dashed border-white/10 bg-[#121215] p-12 text-center">
        <p className="text-slate-400">Create a study session from the sidebar to start chatting.</p>
      </div>
    )
  }

  return (
    <div className="flex h-[calc(100vh-10rem)] gap-6">
      {/* ── Chat column ───────────────────────────────────────── */}
      <div className="flex min-w-0 flex-1 flex-col">
        <div className="flex items-center justify-between border-b border-white/5 pb-3">
          <div>
            <h1 className="font-semibold text-white">{currentSession?.title}</h1>
            {lastUsedRag !== null && (
              <p className="text-xs text-slate-500">
                {lastUsedRag ? 'Last reply grounded in your documents' : 'Last reply used general knowledge'}
              </p>
            )}
          </div>
          <Link
            to="/quiz"
            className="flex items-center gap-2 rounded-lg bg-cyan-500/10 px-3 py-1.5 text-xs font-semibold text-cyan-300 ring-1 ring-cyan-400/20 transition hover:bg-cyan-500/20"
          >
            <QuizIcon className="h-4 w-4" />
            Generate Quiz
          </Link>
        </div>

        {revisitTopic && (
          <div className="mt-4 flex items-start justify-between gap-3 rounded-xl border border-purple-500/20 bg-purple-500/5 p-4">
            <div>
              <p className="text-sm font-medium text-purple-200">Let's revise: {revisitTopic}</p>
              <p className="mt-1 text-xs text-slate-400">
                Ask a question below about what's tripping you up, or{' '}
                <Link
                  to={`/quiz?topic=${encodeURIComponent(revisitTopic)}`}
                  className="text-purple-400 hover:underline"
                >
                  take a quick quiz
                </Link>{' '}
                to brush up.
              </p>
            </div>
            <button
              onClick={() => setSearchParams({}, { replace: true })}
              className="shrink-0 text-xs text-slate-500 hover:text-slate-300"
              aria-label="Dismiss"
            >
              ✕
            </button>
          </div>
        )}

        <div className="flex-1 space-y-4 overflow-y-auto py-5 pr-1">
          {messages.length === 0 && (
            <p className="pt-10 text-center text-sm text-slate-500">
              Ask anything about the documents in this session.
            </p>
          )}
          {messages.map((m) =>
            m.role === 'user' ? (
              <div key={m.id} className="flex justify-end">
                <div className="max-w-lg rounded-2xl rounded-tr-sm bg-[#1e1e26] px-4 py-2.5 text-sm text-slate-200">
                  {m.content}
                </div>
              </div>
            ) : (
              <div key={m.id} className="flex gap-3">
                <span className="mt-1 flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-purple-500/15 text-purple-400">
                  <SparkIcon className="h-4 w-4" />
                </span>
                <div className="min-w-0 flex-1 border-l-2 border-purple-500/40 pl-4">
                  <p className="text-xs font-semibold tracking-wide text-purple-400">TUTOR AI</p>
                  <div className="mt-1">
                    <MarkdownMessage content={m.content} />
                  </div>
                </div>
              </div>
            ),
          )}
          {isSending && (
            <p className="pl-10 text-xs text-slate-500">Tutor is thinking…</p>
          )}
          <div ref={bottomRef} />
        </div>

        {error && <p className="pb-2 text-sm text-red-400">{error}</p>}

        <form onSubmit={handleSend} className="flex items-center gap-2 rounded-xl border border-white/10 bg-[#121215] p-2">
          <input
            type="text"
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            placeholder="Ask your tutor anything…"
            className="min-w-0 flex-1 bg-transparent px-3 py-2 text-sm text-slate-200 placeholder:text-slate-500 focus:outline-none"
          />
          <button
            type="submit"
            disabled={isSending || !draft.trim()}
            className="flex h-9 w-9 items-center justify-center rounded-full bg-purple-500 text-white transition hover:bg-purple-400 disabled:opacity-40"
          >
            <SendIcon className="h-4 w-4" />
          </button>
        </form>
      </div>

      {/* ── Knowledge hub ─────────────────────────────────────── */}
      <aside className="hidden w-80 shrink-0 flex-col gap-6 overflow-y-auto xl:flex">
        <div>
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-semibold text-white">Active Documents</h2>
            <span className="rounded-md bg-white/5 px-2 py-0.5 text-[11px] text-slate-400">
              {documents.length}
            </span>
          </div>
          <div className="mt-3 space-y-2">
            {documents.length === 0 ? (
              <p className="text-xs text-slate-500">
                No documents yet —{' '}
                <Link to="/documents" className="text-purple-400 hover:underline">
                  upload some
                </Link>
                .
              </p>
            ) : (
              documents.map((doc) => (
                <div
                  key={doc.documentId}
                  className="flex items-center gap-3 rounded-lg border border-white/5 bg-[#121215] p-3"
                >
                  <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-cyan-400/10 text-cyan-300">
                    <DocIcon className="h-4 w-4" />
                  </span>
                  <div className="min-w-0">
                    <p className="truncate text-xs font-medium text-slate-200">{doc.filename}</p>
                    <p className="text-[11px] text-slate-500">
                      {doc.status} · {doc.chunkCount} chunks
                    </p>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        <div>
          <h2 className="text-sm font-semibold text-white">My Knowledge Profile</h2>
          <div className="mt-3 space-y-4">
            {topics.length === 0 ? (
              <p className="text-xs text-slate-500">
                Take a quiz to start building your mastery profile.
              </p>
            ) : (
              topics.map((t) => {
                const meta = masteryMeta[t.masteryLevel]
                const pct = Math.round(t.emaScore * 100)
                // This panel shows the student's whole profile, not just
                // this session's — the same topic name can legitimately
                // appear once per session it was tracked in, each with its
                // own score, so label which session when that happens.
                const dupeCount = topics.filter((other) => other.topic === t.topic).length
                const sessionLabel =
                  dupeCount > 1 && t.sessionId
                    ? sessions.find((s) => s.id === t.sessionId)?.title ?? 'Other session'
                    : undefined
                return (
                  <div key={t.id}>
                    <div className="flex items-baseline justify-between gap-2">
                      <p className="truncate text-xs font-medium text-slate-200">
                        {t.topic}
                        {sessionLabel && (
                          <span className="ml-1.5 rounded-full bg-white/5 px-1.5 py-0.5 text-[9px] font-medium text-slate-500">
                            {sessionLabel}
                          </span>
                        )}
                      </p>
                      <span className="shrink-0 text-xs font-semibold text-slate-300">{pct}%</span>
                    </div>
                    <p className={`text-[10px] font-semibold tracking-wide ${meta.text}`}>{meta.label}</p>
                    <div className="mt-1.5 h-1.5 w-full rounded-full bg-white/5">
                      <div className={`h-1.5 rounded-full ${meta.bar}`} style={{ width: `${pct}%` }} />
                    </div>
                  </div>
                )
              })
            )}
          </div>
        </div>
      </aside>
    </div>
  )
}
