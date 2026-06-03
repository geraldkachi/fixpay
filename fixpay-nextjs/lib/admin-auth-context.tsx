'use client'

import { createContext, useContext, useState, useCallback, useEffect, type ReactNode } from 'react'
import type { User } from '@/lib/types'

interface AdminAuthState {
  admin: User | null
  token: string | null
  isLoading: boolean
  setAdminAuth: (admin: User, token: string) => void
  clearAdminAuth: () => void
}

const AdminAuthContext = createContext<AdminAuthState | null>(null)
const KEY = 'fp_admin_token'

export function AdminAuthProvider({ children }: { children: ReactNode }) {
  const [admin, setAdmin] = useState<User | null>(null)
  const [token, setToken] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const stored = localStorage.getItem(KEY)
    if (stored) {
      try {
        const { token: t, admin: a } = JSON.parse(stored)
        setToken(t)
        setAdmin(a)
      } catch {
        localStorage.removeItem(KEY)
      }
    }
    setIsLoading(false)
  }, [])

  const setAdminAuth = useCallback((a: User, t: string) => {
    setAdmin(a)
    setToken(t)
    localStorage.setItem(KEY, JSON.stringify({ token: t, admin: a }))
  }, [])

  const clearAdminAuth = useCallback(() => {
    setAdmin(null)
    setToken(null)
    localStorage.removeItem(KEY)
  }, [])

  return (
    <AdminAuthContext.Provider value={{ admin, token, isLoading, setAdminAuth, clearAdminAuth }}>
      {children}
    </AdminAuthContext.Provider>
  )
}

export function useAdminAuth() {
  const ctx = useContext(AdminAuthContext)
  if (!ctx) throw new Error('useAdminAuth must be used within <AdminAuthProvider>')
  return ctx
}
