import { api, unwrap } from '@/lib/api'
import type { ApiResponse, AuthResponse, LoginRequest, RegisterRequest, UserProfile } from '@/types/api'

export function login(body: LoginRequest) {
  return unwrap(api.post<ApiResponse<AuthResponse>>('/auth/login', body))
}

export function register(body: RegisterRequest) {
  return unwrap(api.post<ApiResponse<AuthResponse>>('/auth/register', body))
}

export function logout() {
  return unwrap(api.post<ApiResponse<null>>('/auth/logout'))
}

// Called once on app load to silently restore a session from the httpOnly
// refresh_token cookie, so a page refresh doesn't force a re-login.
export function refresh() {
  return unwrap(api.post<ApiResponse<string>>('/auth/refresh'))
}

// /auth/refresh only returns a new access token, not user info — this fills
// in the real identity afterward so a page reload doesn't leave user.userId
// blank (documents/sessions/etc. all need a real userId).
export function getProfile() {
  return unwrap(api.get<ApiResponse<UserProfile>>('/auth/user/profile'))
}
