import { useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import { useSession } from '@/context/SessionContext'
import * as quizApi from '@/lib/quiz'
import type { AnswerResult, DifficultyLevel, Quiz as QuizType } from '@/types/api'

// "AUTO" exists only in the UI: the backend treats a null difficulty as
// "pick one from this student's mastery", so it's mapped to undefined below
// rather than sent as a literal value.
type DifficultyChoice = DifficultyLevel | 'AUTO'
const difficultyChoices: { value: DifficultyChoice; label: string }[] = [
  { value: 'AUTO', label: 'Auto (match my level)' },
  { value: 'BEGINNER', label: 'Easy' },
  { value: 'INTERMEDIATE', label: 'Intermediate' },
  { value: 'ADVANCED', label: 'Hard' },
]
const optionLetters = ['A', 'B', 'C', 'D'] as const

const inputClass =
  'mt-1 rounded-lg border border-white/10 bg-[#17171c] px-3 py-2 text-sm text-slate-200 placeholder:text-slate-600 focus:border-purple-500 focus:outline-none'

export default function Quiz() {
  const { user } = useAuth()
  const { sessions, currentSessionId } = useSession()
  const currentSession = sessions.find((s) => s.id === currentSessionId)

  const [searchParams] = useSearchParams()
  // Prefilled when arriving via a "let's revise X" notification or the
  // Chat page's revisit banner — still just a normal editable field.
  const [topic, setTopic] = useState(searchParams.get('topic') ?? '')
  const [difficulty, setDifficulty] = useState<DifficultyChoice>('AUTO')
  const [questionCount, setQuestionCount] = useState(5)
  const [isGenerating, setIsGenerating] = useState(false)
  const [error, setError] = useState('')

  const [quiz, setQuiz] = useState<QuizType | null>(null)
  const [selected, setSelected] = useState<Record<string, string>>({})
  const [results, setResults] = useState<Record<string, AnswerResult>>({})

  // Subject comes from the session, not a free-text field — typing an
  // unrelated word here (e.g. "Competition" for a sports-science session)
  // used to send that raw string straight to the model with nothing to
  // ground it, which is how a Motor Fitness quiz once came back full of
  // species-competition ecology questions. currentSession.subject is
  // never actually populated by the app today (session creation only
  // collects a title), so the title doubles as the subject until sessions
  // capture one explicitly.
  const effectiveSubject = currentSession?.subject || currentSession?.title || 'General'

  async function handleGenerate(e: FormEvent) {
    e.preventDefault()
    if (!user || !currentSessionId) return

    setError('')
    setIsGenerating(true)
    try {
      const generated = await quizApi.generateQuiz({
        userId: user.userId,
        sessionId: currentSessionId,
        subject: effectiveSubject,
        topic: topic.trim() || undefined,
        // undefined => backend resolves difficulty from mastery
        difficulty: difficulty === 'AUTO' ? undefined : difficulty,
        questionCount,
      })
      setQuiz(generated)
      setSelected({})
      setResults({})
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to generate quiz')
    } finally {
      setIsGenerating(false)
    }
  }

  async function handleAnswer(questionId: string) {
    const answer = selected[questionId]
    if (!answer) return
    try {
      const result = await quizApi.submitAnswer(questionId, {
        questionId,
        answer: answer as 'A' | 'B' | 'C' | 'D',
      })
      setResults((prev) => ({ ...prev, [questionId]: result }))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to submit answer')
    }
  }

  if (!currentSessionId) {
    return (
      <div className="rounded-xl border border-dashed border-white/10 bg-[#121215] p-12 text-center">
        <p className="text-slate-400">Create a study session from the sidebar to generate a quiz.</p>
      </div>
    )
  }

  const answeredCount = quiz ? quiz.questions.filter((q) => results[q.id]).length : 0
  const correctCount = Object.values(results).filter((r) => r.correct).length

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-white">Quiz</h1>
        <p className="mt-1 text-sm text-slate-500">Session: {currentSession?.title}</p>
      </div>

      <form
        onSubmit={handleGenerate}
        className="flex flex-wrap items-end gap-4 rounded-xl border border-white/5 bg-[#121215] p-5"
      >
        <div className="flex flex-col">
          <label className="text-xs font-semibold tracking-wider text-slate-500">
            TOPIC (OPTIONAL)
          </label>
          <input
            type="text"
            value={topic}
            onChange={(e) => setTopic(e.target.value)}
            placeholder={`e.g. a topic from ${effectiveSubject}`}
            className={`${inputClass} w-56`}
          />
          <p className="mt-1 max-w-56 text-[11px] text-slate-500">
            Give a specific topic and questions are grounded in your uploaded
            documents. Leave blank for a general {effectiveSubject} quiz.
          </p>
        </div>
        <div className="flex flex-col">
          <label className="text-xs font-semibold tracking-wider text-slate-500">DIFFICULTY</label>
          <select
            value={difficulty}
            onChange={(e) => setDifficulty(e.target.value as DifficultyChoice)}
            className={inputClass}
          >
            {difficultyChoices.map((d) => (
              <option key={d.value} value={d.value} className="bg-[#17171c]">
                {d.label}
              </option>
            ))}
          </select>
        </div>
        <div className="flex flex-col">
          <label className="text-xs font-semibold tracking-wider text-slate-500">QUESTIONS</label>
          <input
            type="number"
            min={1}
            max={15}
            value={questionCount}
            onChange={(e) => setQuestionCount(Number(e.target.value))}
            className={`${inputClass} w-20`}
          />
        </div>
        <button
          type="submit"
          disabled={isGenerating}
          className="rounded-lg bg-purple-500 px-5 py-2 text-sm font-semibold text-white transition hover:bg-purple-400 disabled:opacity-40"
        >
          {isGenerating ? 'Generating…' : 'Generate quiz'}
        </button>
      </form>

      {error && <p className="text-sm text-red-400">{error}</p>}

      {quiz && (
        <div className="space-y-4">
          <div className="flex flex-wrap items-center gap-3 text-sm text-slate-400">
            <span>
              Score: <span className="font-semibold text-slate-200">{correctCount}</span> / {answeredCount}{' '}
              answered · {quiz.questions.length} total
            </span>
            {difficulty === 'AUTO' && (
              <span className="rounded-full bg-purple-500/10 px-2.5 py-0.5 text-xs font-medium text-purple-300 ring-1 ring-purple-400/20">
                Auto-matched to {quiz.difficulty}
              </span>
            )}
          </div>

          {answeredCount === quiz.questions.length && (
            <div className="rounded-xl border border-purple-500/20 bg-purple-500/5 p-5 text-center">
              <p className="text-sm font-medium text-slate-300">Quiz complete</p>
              <p className="mt-1 text-3xl font-bold text-purple-300">
                {Math.round((correctCount / quiz.questions.length) * 100)}%
              </p>
              <p className="mt-1 text-sm text-slate-500">
                {correctCount} of {quiz.questions.length} correct · your mastery for these topics has been
                updated
              </p>
            </div>
          )}

          {quiz.questions.map((q, i) => {
            const result = results[q.id]
            return (
              <div key={q.id} className="rounded-xl border border-white/5 bg-[#121215] p-5">
                <p className="font-medium text-slate-100">
                  <span className="text-purple-400">{i + 1}.</span> {q.questionText}
                </p>
                <div className="mt-4 space-y-2">
                  {q.options.map((option, idx) => {
                    const letter = optionLetters[idx]
                    const isPicked = selected[q.id] === letter
                    return (
                      <label
                        key={letter}
                        className={`flex cursor-pointer items-center gap-3 rounded-lg border px-3 py-2 text-sm transition ${
                          isPicked
                            ? 'border-purple-500/40 bg-purple-500/10 text-slate-100'
                            : 'border-white/5 text-slate-300 hover:bg-white/5'
                        } ${result ? 'cursor-default' : ''}`}
                      >
                        <input
                          type="radio"
                          name={q.id}
                          value={letter}
                          disabled={!!result}
                          checked={isPicked}
                          onChange={() => setSelected((prev) => ({ ...prev, [q.id]: letter }))}
                          className="accent-purple-500"
                        />
                        {option}
                      </label>
                    )
                  })}
                </div>

                {!result ? (
                  <button
                    onClick={() => handleAnswer(q.id)}
                    disabled={!selected[q.id]}
                    className="mt-4 rounded-lg border border-white/10 px-4 py-1.5 text-sm font-medium text-slate-300 transition hover:bg-white/5 disabled:opacity-40"
                  >
                    Submit answer
                  </button>
                ) : (
                  <div
                    className={`mt-4 rounded-lg border p-3 text-sm ${
                      result.correct
                        ? 'border-emerald-400/20 bg-emerald-400/5 text-emerald-300'
                        : 'border-rose-400/20 bg-rose-400/5 text-rose-300'
                    }`}
                  >
                    <p className="font-semibold">
                      {result.correct ? 'Correct' : `Incorrect — the answer was ${result.correctAnswer}`}
                    </p>
                    {result.explanation && (
                      <p className="mt-1 text-slate-400">{result.explanation}</p>
                    )}
                  </div>
                )}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
