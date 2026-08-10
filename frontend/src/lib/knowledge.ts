import { api, unwrap } from '@/lib/api'
import type { ApiResponse, MasteryProfile, TopicMastery, UserStats } from '@/types/api'

export function getMasteryProfile(userId: string) {
  return unwrap(api.get<ApiResponse<MasteryProfile>>(`/knowledge/${userId}/profile`))
}

export function getStats(userId: string) {
  return unwrap(api.get<ApiResponse<UserStats>>(`/knowledge/${userId}/stats`))
}

export function getWeakTopics(userId: string) {
  return unwrap(api.get<ApiResponse<TopicMastery[]>>(`/knowledge/${userId}/weak-topics`))
}
