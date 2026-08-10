import { useEffect, useState } from 'react'
import { useAuth } from '@/context/AuthContext'
import * as flashcardsApi from '@/lib/flashcards'
import type { Flashcard } from '@/types/api'

// SM-2 grades recall 0-5. Four buttons (Anki-style) instead of six raw
// numbers — easier to pick between, same underlying scale.
const qualityOptions = [
  { label: 'Again', quality: 0, style: 'border-rose-400/30 text-rose-300 hover:bg-rose-400/10' },
  { label: 'Hard', quality: 2, style: 'border-amber-400/30 text-amber-300 hover:bg-amber-400/10' },
  { label: 'Good', quality: 4, style: 'border-emerald-400/30 text-emerald-300 hover:bg-emerald-400/10' },
  { label: 'Easy', quality: 5, style: 'border-cyan-400/30 text-cyan-300 hover:bg-cyan-400/10' },
]

export default function Flashcards() {
  const { user } = useAuth()

  const [dueCards, setDueCards] = useState<Flashcard[]>([])
  const [allCards, setAllCards] = useState<Flashcard[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [showBack, setShowBack] = useState(false)
  const [error, setError] = useState('')
  const [lastReviewNote, setLastReviewNote] = useState('')

  useEffect(() => {
    if (!user?.userId) return
    Promise.all([flashcardsApi.getDueToday(user.userId), flashcardsApi.getAll(user.userId)])
      .then(([due, all]) => {
        setDueCards(due)
        setAllCards(all)
      })
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load flashcards'))
      .finally(() => setIsLoading(false))
  }, [user?.userId])

  async function handleReview(quality: number) {
    const card = dueCards[0]
    if (!card) return
    try {
      const result = await flashcardsApi.reviewCard(card.id, quality)
      setLastReviewNote(`Next review in ${result.newIntervalDays} day(s) — ${result.nextReviewDate}`)
      setDueCards((prev) => prev.slice(1))
      setShowBack(false)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to submit review')
    }
  }

  if (isLoading) return <p className="text-slate-500">Loading flashcards…</p>

  const currentCard = dueCards[0]

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-semibold text-white">Flashcards</h1>
        <p className="mt-1 text-sm text-slate-500">
          Auto-generated from your uploaded documents, scheduled with SM-2 spaced repetition.
        </p>
      </div>

      {error && <p className="text-sm text-red-400">{error}</p>}

      <div className="mx-auto max-w-2xl rounded-xl border border-white/5 bg-[#121215] p-8">
        {!currentCard ? (
          <p className="text-center text-slate-500">
            {allCards.length === 0
              ? 'No flashcards yet — upload a document and they generate automatically.'
              : 'All caught up. Nothing due for review right now.'}
          </p>
        ) : (
          <div className="space-y-6 text-center">
            <p className="text-xs font-semibold tracking-wider text-slate-500">
              {currentCard.subject} · {currentCard.topic} · {dueCards.length} DUE
            </p>
            <div className="flex min-h-32 items-center justify-center rounded-lg bg-white/[0.03] p-8 text-lg text-slate-100">
              {showBack ? currentCard.back : currentCard.front}
            </div>

            {!showBack ? (
              <button
                onClick={() => setShowBack(true)}
                className="rounded-lg bg-purple-500 px-6 py-2.5 text-sm font-semibold text-white transition hover:bg-purple-400"
              >
                Show answer
              </button>
            ) : (
              <div className="flex justify-center gap-2">
                {qualityOptions.map((opt) => (
                  <button
                    key={opt.label}
                    onClick={() => handleReview(opt.quality)}
                    className={`rounded-lg border px-5 py-2 text-sm font-medium transition ${opt.style}`}
                  >
                    {opt.label}
                  </button>
                ))}
              </div>
            )}
          </div>
        )}
        {lastReviewNote && <p className="mt-6 text-center text-xs text-slate-500">{lastReviewNote}</p>}
      </div>

      {allCards.length > 0 && (
        <div className="overflow-hidden rounded-xl border border-white/5 bg-[#121215]">
          <table className="w-full text-sm">
            <thead className="bg-white/[0.02] text-left text-xs tracking-wider text-slate-500">
              <tr>
                <th className="px-5 py-3 font-semibold">FRONT</th>
                <th className="px-5 py-3 font-semibold">SUBJECT</th>
                <th className="px-5 py-3 font-semibold">NEXT REVIEW</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {allCards.map((card) => (
                <tr key={card.id}>
                  <td className="px-5 py-3 text-slate-300">{card.front}</td>
                  <td className="px-5 py-3 text-slate-500">{card.subject}</td>
                  <td className="px-5 py-3 text-slate-500">{card.nextReviewDate}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
