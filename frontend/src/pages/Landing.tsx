import { Link } from 'react-router-dom'
import { LogoIcon } from '@/components/icons'

const features = [
  { title: 'RAG-grounded tutor', body: 'Answers pulled from your own uploaded notes, not just the model’s memory.' },
  { title: 'Adaptive quizzes', body: 'Generated per subject and difficulty, with explanations on every answer.' },
  { title: 'Spaced repetition', body: 'Flashcards auto-generated from your documents, scheduled with SM-2.' },
  { title: 'Mastery tracking', body: 'Per-topic scores that update as you answer, so weak areas surface early.' },
]

export default function Landing() {
  return (
    <div className="min-h-screen bg-[#0a0a0c]">
      <header className="mx-auto flex max-w-5xl items-center justify-between px-6 py-6">
        <div className="flex items-center gap-2">
          <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-purple-500/15 text-purple-400">
            <LogoIcon />
          </span>
          <span className="text-lg font-semibold text-purple-400">GuruAI</span>
        </div>
        <Link to="/login" className="text-sm font-medium text-slate-400 transition hover:text-slate-200">
          Log in
        </Link>
      </header>

      <main className="mx-auto max-w-5xl px-6 pt-16 text-center">
        <h1 className="text-4xl font-bold tracking-tight text-white sm:text-5xl">
          A tutor that actually read
          <span className="text-purple-400"> your notes</span>
        </h1>
        <p className="mx-auto mt-5 max-w-xl text-slate-400">
          Upload your study material and GuruAI grounds every answer, quiz and flashcard in it — tracking
          which topics you’ve mastered and which need work.
        </p>
        <div className="mt-8 flex justify-center gap-3">
          <Link
            to="/register"
            className="rounded-lg bg-purple-500 px-6 py-2.5 text-sm font-semibold text-white transition hover:bg-purple-400"
          >
            Get started
          </Link>
          <Link
            to="/login"
            className="rounded-lg border border-white/10 px-6 py-2.5 text-sm font-semibold text-slate-300 transition hover:bg-white/5"
          >
            Log in
          </Link>
        </div>

        <div className="mt-20 grid gap-4 pb-20 text-left sm:grid-cols-2">
          {features.map((f) => (
            <div key={f.title} className="rounded-xl border border-white/5 bg-[#121215] p-5">
              <h2 className="font-semibold text-slate-100">{f.title}</h2>
              <p className="mt-1.5 text-sm text-slate-500">{f.body}</p>
            </div>
          ))}
        </div>
      </main>
    </div>
  )
}
