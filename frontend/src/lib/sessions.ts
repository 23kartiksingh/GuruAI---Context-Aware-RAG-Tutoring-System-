import { api, unwrap } from '@/lib/api'
import type { ApiResponse, CreateSessionRequest, StudySession } from '@/types/api'

export function listSessions(userId: string) {
  return unwrap(api.get<ApiResponse<StudySession[]>>(`/sessions/${userId}`))
}

export function createSession(body: CreateSessionRequest) {
  return unwrap(api.post<ApiResponse<StudySession>>('/sessions', body))
}

/**
 * Delete a session. The backend removes the session and its chat messages,
 * then publishes session.deleted so document-, flashcard- and
 * knowledge-service drop their data for it — that part is eventually
 * consistent, so those lists may lag by a moment.
 */
export function deleteSession(sessionId: string) {
  return unwrap(api.delete<ApiResponse<null>>(`/sessions/${sessionId}`))
}
