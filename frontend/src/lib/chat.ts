import { api, unwrap } from '@/lib/api'
import type { ApiResponse, ChatMessage, ChatRequest, ChatResponse } from '@/types/api'

export function getHistory(sessionId: string) {
  return unwrap(api.get<ApiResponse<ChatMessage[]>>(`/sessions/${sessionId}/history`))
}

export function sendChat(body: ChatRequest) {
  return unwrap(api.post<ApiResponse<ChatResponse>>('/chat', body))
}
