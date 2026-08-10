import { api, unwrap } from '@/lib/api'
import type { ApiResponse, StudyDocument } from '@/types/api'

export function listDocuments(sessionId: string) {
  return unwrap(api.get<ApiResponse<StudyDocument[]>>(`/documents/${sessionId}`))
}

export function uploadDocument(sessionId: string, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return unwrap(
    api.post<ApiResponse<StudyDocument>>(`/documents/${sessionId}/upload`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
  )
}

/** Result of one file in a multi-file upload — mirrors Promise.allSettled. */
export type UploadOutcome =
  | { filename: string; ok: true; document: StudyDocument }
  | { filename: string; ok: false; error: string }

/**
 * Upload several files to the same session.
 *
 * The backend's upload endpoint takes exactly one file per request, so
 * "multi-upload" here means issuing one request per file. They run
 * sequentially on purpose: document-service does Tika parsing + embedding
 * per upload and is memory-capped in Docker, so firing 5 at once is a good
 * way to OOM it. One failure doesn't abort the rest — each result is
 * reported back individually.
 */
export async function uploadDocuments(sessionId: string, files: File[]): Promise<UploadOutcome[]> {
  const outcomes: UploadOutcome[] = []
  for (const file of files) {
    try {
      const document = await uploadDocument(sessionId, file)
      outcomes.push({ filename: file.name, ok: true, document })
    } catch (err) {
      outcomes.push({
        filename: file.name,
        ok: false,
        error: err instanceof Error ? err.message : 'Upload failed',
      })
    }
  }
  return outcomes
}

export function deleteDocument(sessionId: string, documentId: string) {
  // This endpoint returns 204 No Content (no ApiResponse envelope), so it
  // skips `unwrap` — there's no body to unwrap.
  return api.delete(`/documents/${sessionId}/${documentId}`)
}
