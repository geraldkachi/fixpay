import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { User } from '@/types'

interface AuthState {
  /**
   * In-memory access token.
   * NOT stored in localStorage — the real backend delivers it via an
   * httpOnly cookie. This field acts as a session-scoped cache so the
   * Axios interceptor can still attach Authorization headers in mock mode
   * (where httpOnly cookies aren't available from MSW).
   * It is cleared on logout and NOT written to disk.
   */
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
    {
      name: 'fixpay-auth',
      // token is memory-only — never written to localStorage.
      // The server manages session persistence via httpOnly cookies.
      partialize: (state) => ({
        user: state.user,
        isAuthenticated: state.isAuthenticated,
        pinCreated: state.pinCreated,
        kycCompleted: state.kycCompleted,
        pendingPhone: state.pendingPhone,
        pendingEmail: state.pendingEmail,
      }),
    }
  )
)
