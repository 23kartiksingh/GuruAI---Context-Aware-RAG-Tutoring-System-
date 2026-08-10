import { api, unwrap } from '@/lib/api'
import type { ApiResponse, Notification } from '@/types/api'

export function getAll(userId: string) {
  return unwrap(api.get<ApiResponse<Notification[]>>(`/notifications/${userId}`))
}

export function markAllRead(userId: string) {
  return unwrap(api.post<ApiResponse<string>>(`/notifications/${userId}/mark-read`))
}
