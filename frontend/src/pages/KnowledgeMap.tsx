import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import { useSession } from '@/context/SessionContext'
import * as knowledgeApi from '@/lib/knowledge'
import type { MasteryLevel, MasteryProfile, TopicMastery } from '@/types/api'

// Bands come from com.guruai.common.enums.MasteryLevel — WEAK below 50%,
// AVERAGE 50-75%, STRONG above 75%. Kept in sync with the backend enum, which
// is the single source of truth for where a topic falls.
const masteryMeta: Record<MasteryLevel, { bar: string; text: string; label: string; range: string }> = {
  STRONG: { bar: 'bg-emerald-400', text: 'text-emerald-400', label: 'MASTERY HIGH', range: 'Above 75%' },
  AVERAGE: { bar: 'bg-cyan-400', text: 'text-cyan-400', label: 'PROGRESSING', range: '50% – 75%' },
  WEAK: { bar: 'bg-rose-400', text: 'text-rose-400', label: 'FOCUS REQUIRED', range: 'Below 50%' },
}

function TopicRow({ topic, sessionLabel }: { topic: TopicMastery; sessionLabel?: string }) {
  const meta = masteryMeta[topic.masteryLevel]
  const pct = Math.round(topic.emaScore * 100)
  return (
    <div className="rounded-xl border border-white/5 bg-[#121215] p-4">
      <div className="flex items-baseline justify-between gap-4">
        <div className="min-w-0">
          <p className="truncate font-medium text-slate-100">
            {topic.topic}
            {/* Same topic name can legitimately exist in more than one
                session, each with its own separate score — shown only when
                that's actually the case, so the common single-session
                topic isn't cluttered with a label nobody needs. */}
            {sessionLabel && (
              <span className="ml-2 rounded-full bg-white/5 px-2 py-0.5 text-[10px] font-medium text-slate-400">
                {sessionLabel}
              </span>
            )}
          </p>
          <p className="text-xs text-slate-500">
            {topic.subject} · {topic.correctCount}/{topic.totalCount} correct
          </p>
        </div>
        <span className={`shrink-0 text-sm font-semibold ${meta.text}`}>{pct}% Mastery</span>
      </div>
      <div className="mt-3 h-1.5 w-full rounded-full bg-white/5">
        <div className={`h-1.5 rounded-full ${meta.bar}`} style={{ width: `${pct}%` }} />
      </div>
    </div>
  )
}

export default function KnowledgeMap() {
  const { user } = useAuth()
  const { sessions } = useSession()

  const [profile, setProfile] = useState<MasteryProfile | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  // Which band the user is currently looking at.
  const [filter, setFilter] = useState<MasteryLevel | 'ALL'>('WEAK')

  useEffect(() => {
    if (!user?.userId) return
    knowledgeApi
      .getMasteryProfile(user.userId)
      .then(setProfile)
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load knowledge map'))
      .finally(() => setIsLoading(false))
  }, [user?.userId])

  if (isLoading) return <p className="text-slate-500">Loading knowledge map…</p>

  const topics = profile?.topics ?? []
  const shown = filter === 'ALL' ? topics : topics.filter((t) => t.masteryLevel === filter)

  // How many times each topic name shows up across the WHOLE profile (not
  // just the current filter) — the same name can appear once per session it
  // was tracked in, each with its own separate score.
  const nameCounts = topics.reduce<Record<string, number>>((acc, t) => {
    acc[t.topic] = (acc[t.topic] ?? 0) + 1
    return acc
  }, {})
  function sessionLabelFor(t: TopicMastery): string | undefined {
    if (nameCounts[t.topic] <= 1 || !t.sessionId) return undefined
    return sessions.find((s) => s.id === t.sessionId)?.title ?? 'Other session'
  }

  const cards = [
    {
      level: 'STRONG' as const,
      count: profile?.strongCount ?? 0,
      title: 'Strong Topics',
      ring: 'from-emerald-400/60',
    },
    {
      level: 'AVERAGE' as const,
      count: profile?.averageCount ?? 0,
      title: 'Average Topics',
      ring: 'from-cyan-400/60',
    },
    {
      level: 'WEAK' as const,
      count: profile?.weakCount ?? 0,
      title: 'Weak Topics',
      ring: 'from-rose-400/60',
    },
  ]

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-3xl font-bold text-white">Knowledge Map</h1>
        <p className="mt-2 max-w-xl text-sm text-slate-500">
          Every topic you've been quizzed on, scored by an exponential moving average and grouped by how
          well you know it. Weak topics are where reviewing pays off most.
        </p>
      </div>

      {error && <p className="text-sm text-red-400">{error}</p>}

      {topics.length === 0 ? (
        <div className="rounded-xl border border-dashed border-white/10 bg-[#121215] p-12 text-center">
          <p className="text-slate-400">No topics tracked yet.</p>
          <p className="mt-1 text-sm text-slate-500">
            Take a{' '}
            <Link to="/quiz" className="text-purple-400 hover:underline">
              quiz
            </Link>{' '}
            and your mastery starts building here.
          </p>
        </div>
      ) : (
        <>
          {/* ── Three category cards ─────────────────────────────── */}
          <div className="grid gap-4 sm:grid-cols-3">
            {cards.map((card) => {
              const meta = masteryMeta[card.level]
              const active = filter === card.level
              return (
                <button
                  key={card.level}
                  onClick={() => setFilter(active ? 'ALL' : card.level)}
                  className={`overflow-hidden rounded-xl border bg-[#121215] p-5 text-left transition ${
                    active ? 'border-white/20 bg-[#17171c]' : 'border-white/5 hover:border-white/10'
                  }`}
                >
                  <div className="flex items-start justify-between">
                    <span className={`text-3xl font-bold ${meta.text}`}>{card.count}</span>
                  </div>
                  <p className="mt-3 font-semibold text-slate-100">{card.title}</p>
                  <p className="text-xs text-slate-500">{meta.range} retention</p>
                  <div className={`mt-4 h-0.5 w-full rounded-full bg-gradient-to-r ${card.ring} to-transparent`} />
                </button>
              )
            })}
          </div>

          {/* ── Filtered topic list ──────────────────────────────── */}
          <div>
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold text-white">
                {filter === 'ALL' ? 'All Topics' : `Focus: ${masteryMeta[filter].label.toLowerCase()}`}
              </h2>
              <div className="flex items-center gap-2 text-xs">
                <span className="text-slate-500">
                  Overall {profile ? Math.round(profile.overallMasteryPct) : 0}%
                </span>
                {filter !== 'ALL' && (
                  <button
                    onClick={() => setFilter('ALL')}
                    className="rounded-md bg-white/5 px-2 py-1 font-medium text-slate-400 hover:bg-white/10"
                  >
                    Show all
                  </button>
                )}
              </div>
            </div>

            <div className="mt-4 space-y-3">
              {shown.length === 0 ? (
                <p className="rounded-xl border border-dashed border-white/10 bg-[#121215] p-8 text-center text-sm text-slate-500">
                  Nothing in this category — good news if it's the weak one.
                </p>
              ) : (
                [...shown]
                  .sort((a, b) => a.emaScore - b.emaScore)
                  .map((t) => <TopicRow key={t.id} topic={t} sessionLabel={sessionLabelFor(t)} />)
              )}
            </div>
          </div>
        </>
      )}
    </div>
  )
}
