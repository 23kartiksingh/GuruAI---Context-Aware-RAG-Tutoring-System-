import { api, unwrap } from '@/lib/api'
import type { ApiResponse, MemoryItemsResponse } from '@/types/api'

// user-memory-service is keyed on userId only (no sessionId) — whatever's
// stored here applies to every session and chat the user has, which is the
// whole point: "I'm a Java coder and anime fan" only needs to be said once.

export function getMemory(userId: string) {
  return unwrap(api.get<ApiResponse<MemoryItemsResponse>>(`/memory/${userId}`))
}

// Freeform text goes through an LLM extraction step server-side (pulls out
// distinct preference statements, dedupes against what's already stored) —
// the caller doesn't need to phrase it as a list.
export function addMemory(userId: string, message: string) {
  return unwrap(api.post<ApiResponse<MemoryItemsResponse>>(`/memory/${userId}`, { message }))
}

export function clearMemory(userId: string) {
  return unwrap(api.delete<ApiResponse<string>>(`/memory/${userId}`))
}

// Direct overwrite of one item's text — a user edit, not extraction. No LLM
// call, whatever they typed is stored verbatim.
export function updateMemoryItem(userId: string, itemId: string, text: string) {
  return unwrap(
    api.put<ApiResponse<MemoryItemsResponse>>(`/memory/${userId}/items/${itemId}`, { text }),
  )
}

export function deleteMemoryItem(userId: string, itemId: string) {
  return unwrap(api.delete<ApiResponse<MemoryItemsResponse>>(`/memory/${userId}/items/${itemId}`))
}
