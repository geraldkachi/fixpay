/**
 * Simple Zustand-free auth store using React context.
 * Wraps the app at the route-group layout level.
 */
'use client'

import { createContext, useContext, useState, useCallback, useEffect, type ReactNode } from 'react'
import type { User } from '@/lib/types'

interface AuthState {
  user: User | null
  token: string | null
  isLoading: boolean
  setAuth: (user: User, token: string) => void
  clearAuth: () => void
}

const AuthContext = createContext<AuthState | null>(null)

const TOKEN_KEY = 'fp_token'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [token, setToken] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const stored = localStorage.getItem(TOKEN_KEY)
    if (stored) {
      try {
        const { token: t, user: u } = JSON.parse(stored)
        setToken(t)
        setUser(u)
      } catch {
        localStorage.removeItem(TOKEN_KEY)
      }
    }
    setIsLoading(false)
  }, [])

  const setAuth = useCallback((u: User, t: string) => {
    setUser(u)
    setToken(t)
    localStorage.setItem(TOKEN_KEY, JSON.stringify({ token: t, user: u }))
  }, [])

  const clearAuth = useCallback(() => {
    setUser(null)
    setToken(null)
    localStorage.removeItem(TOKEN_KEY)
  }, [])

  return (
    <AuthContext.Provider value={{ user, token, isLoading, setAuth, clearAuth }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within <AuthProvider>')
  return ctx
}
