import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { User } from '@/types'

interface AuthState {
  token: string | null
  user: User | null
  isAuthenticated: boolean
  pinCreated: boolean
  kycCompleted: boolean
  pendingPhone: string | null
  pendingEmail: string | null
  setToken: (token: string) => void
  setUser: (user: User) => void
  setPinCreated: (v: boolean) => void
  setKycCompleted: (v: boolean) => void
  setPending: (phone?: string, email?: string) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      isAuthenticated: false,
      pinCreated: false,
      kycCompleted: false,
      pendingPhone: null,
      pendingEmail: null,
      setToken: (token) => set({ token, isAuthenticated: true }),
      setUser: (user) => set({ user }),
      setPinCreated: (v) => set({ pinCreated: v }),
      setKycCompleted: (v) => set({ kycCompleted: v }),
      setPending: (phone, email) => set({ pendingPhone: phone ?? null, pendingEmail: email ?? null }),
      logout: () => set({ token: null, user: null, isAuthenticated: false, pinCreated: false, kycCompleted: false }),
    }),
    { name: 'fixpay-auth' }
  )
)
