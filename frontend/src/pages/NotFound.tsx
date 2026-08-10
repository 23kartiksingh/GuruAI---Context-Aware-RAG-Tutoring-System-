import { Link } from 'react-router-dom'

export default function NotFound() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-3 bg-[#0a0a0c] text-center">
      <h1 className="text-4xl font-bold text-white">404</h1>
      <p className="text-slate-500">This page doesn’t exist.</p>
      <Link to="/" className="text-sm font-medium text-purple-400 hover:underline">
        Back home
      </Link>
    </div>
  )
}
