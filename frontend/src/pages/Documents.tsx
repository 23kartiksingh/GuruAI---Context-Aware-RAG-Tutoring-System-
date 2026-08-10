import { useEffect, useRef, useState, type DragEvent } from 'react'
import { useSession } from '@/context/SessionContext'
import * as documentsApi from '@/lib/documents'
import type { UploadOutcome } from '@/lib/documents'
import type { StudyDocument } from '@/types/api'
import { CloseIcon, DocIcon, UploadIcon } from '@/components/icons'

const statusStyles: Record<StudyDocument['status'], string> = {
  PROCESSING: 'bg-amber-400/10 text-amber-300 ring-amber-400/20',
  INDEXED: 'bg-emerald-400/10 text-emerald-300 ring-emerald-400/20',
  FAILED: 'bg-red-400/10 text-red-300 ring-red-400/20',
}

export default function Documents() {
  const { sessions, currentSessionId, isLoading: sessionsLoading } = useSession()
  const currentSession = sessions.find((s) => s.id === currentSessionId)

  const [documents, setDocuments] = useState<StudyDocument[]>([])
  const [isUploading, setIsUploading] = useState(false)
  const [uploadingNames, setUploadingNames] = useState<string[]>([])
  const [outcomes, setOutcomes] = useState<UploadOutcome[]>([])
  const [isDragging, setIsDragging] = useState(false)
  const [deletingId, setDeletingId] = useState<string | null>(null)
  const [error, setError] = useState('')

  const fileInputRef = useRef<HTMLInputElement>(null)

  async function loadDocuments(sessionId: string) {
    try {
      setDocuments(await documentsApi.listDocuments(sessionId))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load documents')
    }
  }

  useEffect(() => {
    if (!currentSessionId) {
      setDocuments([])
      return
    }
    loadDocuments(currentSessionId)

    // Indexing is async (backend returns 202 immediately) — poll while
    // anything is still PROCESSING so badges update on their own.
    const interval = setInterval(() => {
      setDocuments((prev) => {
        if (prev.some((d) => d.status === 'PROCESSING')) {
          loadDocuments(currentSessionId)
        }
        return prev
      })
    }, 3000)
    return () => clearInterval(interval)
  }, [currentSessionId])

  async function handleFiles(fileList: FileList | null) {
    if (!fileList || fileList.length === 0 || !currentSessionId) return
    const files = Array.from(fileList)

    setError('')
    setOutcomes([])
    setUploadingNames(files.map((f) => f.name))
    setIsUploading(true)
    try {
      const results = await documentsApi.uploadDocuments(currentSessionId, files)
      setOutcomes(results)
      await loadDocuments(currentSessionId)
    } finally {
      setIsUploading(false)
      setUploadingNames([])
      if (fileInputRef.current) fileInputRef.current.value = ''
    }
  }

  async function handleDelete(doc: StudyDocument) {
    if (!currentSessionId) return
    if (!window.confirm(`Delete "${doc.filename}"? Its indexed chunks are removed too.`)) return

    setError('')
    setDeletingId(doc.documentId)
    try {
      await documentsApi.deleteDocument(currentSessionId, doc.documentId)
      setDocuments((prev) => prev.filter((d) => d.documentId !== doc.documentId))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete document')
    } finally {
      setDeletingId(null)
    }
  }

  function handleDrop(e: DragEvent<HTMLDivElement>) {
    e.preventDefault()
    setIsDragging(false)
    void handleFiles(e.dataTransfer.files)
  }

  if (sessionsLoading) {
    return <p className="text-slate-500">Loading sessions…</p>
  }

  if (!currentSessionId) {
    return (
      <div className="rounded-xl border border-dashed border-white/10 bg-[#121215] p-12 text-center">
        <p className="text-slate-400">Create a study session from the sidebar to start uploading documents.</p>
      </div>
    )
  }

  const failedCount = outcomes.filter((o) => !o.ok).length

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-white">Library</h1>
        <p className="mt-1 text-sm text-slate-500">
          Session: <span className="text-slate-400">{currentSession?.title}</span> · documents are parsed,
          chunked and embedded for RAG chat
        </p>
      </div>

      {/* ── Drop zone ─────────────────────────────────────────── */}
      <div
        onDragOver={(e) => {
          e.preventDefault()
          setIsDragging(true)
        }}
        onDragLeave={() => setIsDragging(false)}
        onDrop={handleDrop}
        onClick={() => fileInputRef.current?.click()}
        className={`cursor-pointer rounded-xl border-2 border-dashed p-10 text-center transition ${
          isDragging
            ? 'border-purple-400 bg-purple-500/5'
            : 'border-white/10 bg-[#121215] hover:border-white/20'
        }`}
      >
        <input
          ref={fileInputRef}
          type="file"
          multiple
          accept=".pdf,.docx,.txt,.md"
          className="hidden"
          onChange={(e) => void handleFiles(e.target.files)}
        />
        <span className="mx-auto flex h-12 w-12 items-center justify-center rounded-xl bg-white/5 text-slate-400">
          <UploadIcon className="h-6 w-6" />
        </span>
        <p className="mt-3 text-sm font-medium text-slate-300">
          {isUploading ? 'Uploading…' : 'Drop documents here, or click to browse'}
        </p>
        <p className="mt-1 text-xs text-slate-500">PDF, DOCX, TXT, MD · multiple files supported</p>
      </div>

      {/* Files are uploaded one at a time — show which are queued vs done. */}
      {isUploading && uploadingNames.length > 0 && (
        <ul className="space-y-1 text-sm text-slate-400">
          {uploadingNames.map((name) => (
            <li key={name} className="flex items-center gap-2">
              <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-purple-400" />
              {name}
            </li>
          ))}
        </ul>
      )}

      {outcomes.length > 0 && (
        <div className="rounded-lg border border-white/10 bg-[#121215] p-4 text-sm">
          <p className="font-medium text-slate-300">
            Uploaded {outcomes.length - failedCount} of {outcomes.length} file(s)
          </p>
          {failedCount > 0 && (
            <ul className="mt-2 space-y-1">
              {outcomes
                .filter((o) => !o.ok)
                .map((o) => (
                  <li key={o.filename} className="text-red-300">
                    {o.filename}: {'error' in o ? o.error : ''}
                  </li>
                ))}
            </ul>
          )}
        </div>
      )}

      {error && <p className="text-sm text-red-400">{error}</p>}

      {/* ── Document grid ─────────────────────────────────────── */}
      {documents.length === 0 ? (
        <p className="text-sm text-slate-500">No documents in this session yet.</p>
      ) : (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {documents.map((doc) => (
            <div
              key={doc.documentId}
              className="group relative rounded-xl border border-white/5 bg-[#121215] p-4"
            >
              <button
                onClick={() => void handleDelete(doc)}
                disabled={deletingId === doc.documentId}
                title="Delete document"
                aria-label={`Delete ${doc.filename}`}
                className="absolute -right-2 -top-2 flex h-7 w-7 items-center justify-center rounded-full border border-white/10 bg-[#1e1e26] text-slate-400 opacity-0 transition hover:border-rose-400/30 hover:bg-rose-500/15 hover:text-rose-300 focus:opacity-100 group-hover:opacity-100 disabled:opacity-40"
              >
                <CloseIcon className="h-3.5 w-3.5" />
              </button>
              <div className="flex items-start justify-between gap-3">
                <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-cyan-400/10 text-cyan-300">
                  <DocIcon className="h-4 w-4" />
                </span>
                <span
                  className={`rounded-full px-2 py-0.5 text-[10px] font-semibold tracking-wide ring-1 ${statusStyles[doc.status]}`}
                >
                  {deletingId === doc.documentId ? 'DELETING' : doc.status}
                </span>
              </div>
              <p className="mt-3 truncate text-sm font-medium text-slate-200" title={doc.filename}>
                {doc.filename}
              </p>
              <p className="mt-1 text-xs text-slate-500">
                {doc.subject ?? 'Subject pending'} · {doc.chunkCount} chunks · {doc.fileSizeMb.toFixed(1)} MB
              </p>
              {doc.topics.length > 0 && (
                <div className="mt-3 flex flex-wrap gap-1">
                  {doc.topics.slice(0, 3).map((t) => (
                    <span key={t} className="rounded-md bg-white/5 px-2 py-0.5 text-[11px] text-slate-400">
                      {t}
                    </span>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
