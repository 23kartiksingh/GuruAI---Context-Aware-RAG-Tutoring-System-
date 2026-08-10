import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import { LogoIcon, GoogleIcon } from '@/components/icons'

// Full-page navigation, not an axios call — same endpoint handles both
// login and signup: first-ever Google login creates the account.
const GOOGLE_LOGIN_URL = `${import.meta.env.VITE_API_BASE_URL}/auth/oauth2/authorization/google`

export default function Register() {
  const { register } = useAuth()
  const navigate = useNavigate()

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [name, setName] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setIsSubmitting(true)
    try {
      // name is optional server-side (defaults to "The Scholar") — only send
      // it if the user actually typed something.
      await register({ username, password, name: name.trim() || undefined })
      navigate('/dashboard', { replace: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Registration failed')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-[#0a0a0c] px-6">
      <div className="w-full max-w-sm rounded-2xl border border-white/5 bg-[#121215] p-8">
        <div className="flex items-center gap-2">
          <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-purple-500/15 text-purple-400">
            <LogoIcon />
          </span>
          <span className="text-lg font-semibold text-purple-400">GuruAI</span>
        </div>
        <h1 className="mt-6 text-xl font-semibold text-white">Create your account</h1>

        <form onSubmit={handleSubmit} className="mt-6 space-y-4">
          <div>
            <label htmlFor="username" className="block text-xs font-semibold tracking-wider text-slate-500">
              USERNAME
            </label>
            <input
              id="username"
              type="text"
              required
              minLength={3}
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="mt-1.5 w-full rounded-lg border border-white/10 bg-[#17171c] px-3 py-2.5 text-sm text-slate-200 placeholder:text-slate-600 focus:border-purple-500 focus:outline-none"
            />
          </div>

          <div>
            <label htmlFor="name" className="block text-xs font-semibold tracking-wider text-slate-500">
              DISPLAY NAME <span className="text-slate-600">(OPTIONAL)</span>
            </label>
            <input
              id="name"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="mt-1.5 w-full rounded-lg border border-white/10 bg-[#17171c] px-3 py-2.5 text-sm text-slate-200 placeholder:text-slate-600 focus:border-purple-500 focus:outline-none"
            />
          </div>

          <div>
            <label htmlFor="password" className="block text-xs font-semibold tracking-wider text-slate-500">
              PASSWORD
            </label>
            <input
              id="password"
              type="password"
              required
              minLength={6}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="mt-1.5 w-full rounded-lg border border-white/10 bg-[#17171c] px-3 py-2.5 text-sm text-slate-200 placeholder:text-slate-600 focus:border-purple-500 focus:outline-none"
            />
            <p className="mt-1 text-xs text-slate-600">At least 6 characters.</p>
          </div>

          {error && <p className="text-sm text-rose-400">{error}</p>}

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full rounded-lg bg-purple-500 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-purple-400 disabled:opacity-40"
          >
            {isSubmitting ? 'Creating account…' : 'Create account'}
          </button>
        </form>

        <div className="mt-5 flex items-center gap-3">
          <div className="h-px flex-1 bg-white/10" />
          <span className="text-xs text-slate-600">OR</span>
          <div className="h-px flex-1 bg-white/10" />
        </div>

        <a
          href={GOOGLE_LOGIN_URL}
          className="mt-5 flex w-full items-center justify-center gap-2.5 rounded-lg border border-white/10 bg-[#17171c] px-4 py-2.5 text-sm font-medium text-slate-200 transition hover:bg-[#1d1d23]"
        >
          <GoogleIcon className="h-4 w-4" />
          Continue with Google
        </a>

        <p className="mt-6 text-center text-sm text-slate-500">
          Already have an account?{' '}
          <Link to="/login" className="font-medium text-purple-400 hover:underline">
            Log in
          </Link>
        </p>
      </div>
    </div>
  )
}
