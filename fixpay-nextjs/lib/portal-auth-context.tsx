'use client'

import { createContext, useContext, useState, useCallback, useEffect, type ReactNode } from 'react'
import type { Tenant } from '@/lib/types'

interface PortalAuthState {
  tenant: Tenant | null
  token: string | null
  isLoading: boolean
  setPortalAuth: (tenant: Tenant, token: string) => void
  clearPortalAuth: () => void
}

const PortalAuthContext = createContext<PortalAuthState | null>(null)
const KEY = 'fp_portal_token'

export function PortalAuthProvider({ children }: { children: ReactNode }) {
  const [tenant, setTenant] = useState<Tenant | null>(null)
  const [token, setToken] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const stored = localStorage.getItem(KEY)
    if (stored) {
      try {
        const { token: t, tenant: tn } = JSON.parse(stored)
        setToken(t)
        setTenant(tn)
      } catch {
        localStorage.removeItem(KEY)
      }
    }
    setIsLoading(false)
  }, [])

  const setPortalAuth = useCallback((tn: Tenant, t: string) => {
    setTenant(tn)
    setToken(t)
    localStorage.setItem(KEY, JSON.stringify({ token: t, tenant: tn }))
  }, [])

  const clearPortalAuth = useCallback(() => {
    setTenant(null)
    setToken(null)
    localStorage.removeItem(KEY)
  }, [])

  return (
    <PortalAuthContext.Provider value={{ tenant, token, isLoading, setPortalAuth, clearPortalAuth }}>
      {children}
    </PortalAuthContext.Provider>
  )
}

export function usePortalAuth() {
  const ctx = useContext(PortalAuthContext)
  if (!ctx) throw new Error('usePortalAuth must be used within <PortalAuthProvider>')
  return ctx
}
