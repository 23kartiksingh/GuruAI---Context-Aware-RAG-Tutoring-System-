import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import type { ApiResponse } from '@/types/api'

// Every request from the frontend goes through the API gateway (:8080) —
// never directly at a downstream service. The gateway is what stamps the
// internal-access secret and validates the JWT before proxying.
const BASE_URL = import.meta.env.VITE_API_BASE_URL

// Module-level token holder (not localStorage/sessionStorage — a short-lived
// access token kept in memory is safer against XSS; refreshing it on page
// load happens via the httpOnly refresh-token cookie, see AuthContext).
let accessToken: string | null = null
export function setAccessToken(token: string | null) {
  accessToken = token
}
export function getAccessToken() {
  return accessToken
}

// A callback AuthContext registers so the interceptor below can trigger a
// refresh without importing AuthContext directly (avoids a circular import
// between api.ts and the context that uses it).
let onUnauthorized: (() => void) | null = null
export function setOnUnauthorized(handler: () => void) {
  onUnauthorized = handler
}

export const api = axios.create({
  baseURL: BASE_URL,
  // Required so the browser sends/receives the httpOnly access_token and
  // refresh-token cookies auth-service sets on login/refresh.
  withCredentials: true,
  // Without a timeout axios waits forever, so a service that is still booting
  // (Spring contexts here can take minutes on a small machine) leaves the UI
  // stuck on "Logging in…" with no clue why. 90s is comfortably longer than a
  // real AI call needs while still failing eventually instead of never.
  timeout: 90_000,
})

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  if (accessToken) {
    config.headers.set('Authorization', `Bearer ${accessToken}`)
  }
  return config
})

let refreshPromise: Promise<string | null> | null = null

async function refreshAccessToken(): Promise<string | null> {
  // Dedupe concurrent 401s into a single refresh call.
  if (!refreshPromise) {
    refreshPromise = axios
      .post<ApiResponse<string>>(
        `${BASE_URL}/auth/refresh`,
        {},
        { withCredentials: true },
      )
      .then((res) => res.data.data ?? null)
      .catch(() => null)
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined

    if (error.response?.status === 401 && original && !original._retried) {
      original._retried = true
      const newToken = await refreshAccessToken()
      if (newToken) {
        setAccessToken(newToken)
        original.headers.set('Authorization', `Bearer ${newToken}`)
        return api(original)
      }
      onUnauthorized?.()
    }
    return Promise.reject(error)
  },
)

// Small helper so callers don't repeat `.data.data` / error-shape handling
// everywhere. Throws a plain Error with the server's actual message on
// failure — covering both shapes a failure can take:
//   1. HTTP 2xx but the ApiResponse body says success:false (rare — most
//      failures are surfaced as a real HTTP status by GlobalExceptionHandler).
//   2. HTTP 4xx/5xx, which axios rejects before this function's body ever
//      runs — every service's GlobalExceptionHandler still wraps the error
//      in the same ApiResponse envelope on the way out, it's just sitting on
//      the rejected error's `.response.data` instead of a resolved one.
// Without step 2, every failed request surfaced Axios's generic
// "Request failed with status code 409" instead of the real reason (e.g.
// "Username 'kartik' is already taken").
export async function unwrap<T>(promise: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  try {
    const { data } = await promise
    if (!data.success || data.data === null) {
      throw new Error(data.error ?? data.message ?? 'Request failed')
    }
    return data.data
  } catch (err) {
    if (axios.isAxiosError(err)) {
      // Distinguish the two "no response" cases from a real API error, since
      // axios's own messages ("timeout of 90000ms exceeded", "Network Error")
      // don't tell the user anything actionable.
      if (err.code === 'ECONNABORTED') {
        throw new Error('The server took too long to respond. It may still be starting up — try again in a moment.')
      }
      if (!err.response) {
        throw new Error('Cannot reach the server. Check that the API gateway is running on ' + BASE_URL + '.')
      }
      const body = err.response.data as ApiResponse<unknown> | undefined
      throw new Error(body?.error ?? body?.message ?? err.message)
    }
    throw err
  }
}
