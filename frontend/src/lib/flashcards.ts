import { api, unwrap } from '@/lib/api'
import type { ApiResponse, Flashcard, ReviewResult } from '@/types/api'

export function getDueToday(userId: string) {
  return unwrap(api.get<ApiResponse<Flashcard[]>>(`/flashcards/${userId}/due-today`))
}

export function getAll(userId: string) {
  return unwrap(api.get<ApiResponse<Flashcard[]>>(`/flashcards/${userId}/all`))
}

// SM-2 quality score, 0-5 (0 = total blackout, 5 = perfect recall).
export function reviewCard(cardId: string, quality: number) {
  return unwrap(api.post<ApiResponse<ReviewResult>>(`/flashcards/${cardId}/review`, { quality }))
}
