import { create } from 'zustand'
import keycloak from '@/lib/keycloak'

let initPromise: Promise<void> | null = null

export type AdminRole =
  | 'PLATFORM_ADMIN'
  | 'SUPPORT_AGENT'
  | 'COMPLIANCE_OFFICER'
  | 'FINANCE_OPS'

interface AdminAuthState {
  isAuthenticated: boolean
  isInitialised: boolean
  username: string
  email: string
  roles: AdminRole[]
  /** Call once on app boot — initialises Keycloak PKCE flow */
  init: () => Promise<void>
  logout: () => void
  hasRole: (role: AdminRole) => boolean
  hasAnyRole: (...roles: AdminRole[]) => boolean
}

export const useAdminAuthStore = create<AdminAuthState>((set, get) => ({
  isAuthenticated: false,
  isInitialised: false,
  username: '',
  email: '',
  roles: [],

  init: async () => {
    if (get().isInitialised) return
    if (initPromise) return initPromise

    initPromise = (async () => {
      try {
        const authenticated = await keycloak.init({
          onLoad: 'login-required',
          pkceMethod: 'S256',
          checkLoginIframe: false,
        })

        if (authenticated) {
          const profile = keycloak.tokenParsed as Record<string, unknown>
          const realmRoles = (
            (profile?.realm_access as { roles?: string[] })?.roles ?? []
          ) as AdminRole[]

          set({
            isAuthenticated: true,
            isInitialised: true,
            username: (profile?.preferred_username as string) ?? '',
            email: (profile?.email as string) ?? '',
            roles: realmRoles,
          })
        } else {
          set({ isInitialised: true })
        }
      } catch {
        set({ isInitialised: true })
      } finally {
        initPromise = null
      }
    })()

    return initPromise
  },

  logout: () => {
    keycloak.logout({ redirectUri: window.location.origin })
  },

  hasRole: (role) => get().roles.includes(role),
  hasAnyRole: (...roles) => roles.some(r => get().roles.includes(r)),
}))
