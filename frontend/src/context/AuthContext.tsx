import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { getAccessToken, setAccessToken, setOnUnauthorized } from '@/lib/api'
import * as authApi from '@/lib/auth'
import type { LoginRequest, RegisterRequest } from '@/types/api'

interface AuthUser {
  userId: string
  username: string
  name: string
}

interface AuthContextValue {
  user: AuthUser | null
  isAuthenticated: boolean
  // True only during the initial silent-refresh check on app load, so
  // routes can avoid a login-page flash before we know the real state.
  isLoading: boolean
  login: (body: LoginRequest) => Promise<void>
  register: (body: RegisterRequest) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const applyAuth = useCallback((auth: { accessToken: string; userId: string; username: string; name: string }) => {
    setAccessToken(auth.accessToken)
    setUser({ userId: auth.userId, username: auth.username, name: auth.name })
  }, [])

  const clearAuth = useCallback(() => {
    setAccessToken(null)
    setUser(null)
  }, [])

  // On first mount, try to silently restore a session from the httpOnly
  // refresh_token cookie so a hard page reload doesn't force a re-login.
  // /auth/refresh only returns a new access token (not user info), so a
  // restored session re-derives the JWT's identity lazily the first time a
  // real request runs (api.ts already attaches the token everywhere).
  useEffect(() => {
    authApi
      .refresh()
      .then(async (token: string | null) => {
        setAccessToken(token)
        // /auth/refresh only returns a new access token, not user info —
        // fetch the real profile so userId/username/name are populated
        // (session/document lists all key off userId).
        const profile = await authApi.getProfile()
        setUser({ userId: profile.userId, username: profile.username, name: profile.name })
      })
      .catch(() => clearAuth())
      .finally(() => setIsLoading(false))
  }, [clearAuth])

  useEffect(() => {
    setOnUnauthorized(() => clearAuth())
  }, [clearAuth])

  const login = useCallback(
    async (body: LoginRequest) => {
      const auth = await authApi.login(body)
      applyAuth(auth)
    },
    [applyAuth],
  )

  const register = useCallback(
    async (body: RegisterRequest) => {
      const auth = await authApi.register(body)
      applyAuth(auth)
    },
    [applyAuth],
  )

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } finally {
      clearAuth()
    }
  }, [clearAuth])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: !!user && !!getAccessToken(),
      isLoading,
      login,
      register,
      logout,
    }),
    [user, isLoading, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider')
  return ctx
}
