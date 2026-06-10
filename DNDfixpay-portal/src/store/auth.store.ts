import { create } from 'zustand'
import keycloak from '@/lib/keycloak'

let initPromise: Promise<void> | null = null

export type TenantRole = 'TENANT_ADMIN' | 'TENANT_STAFF' | 'TENANT_READONLY'

interface PortalAuthState {
  isAuthenticated: boolean
  isInitialised:   boolean
  accessToken:     string | null
  username:        string
  email:           string
  tenantId:        string | null
  roles:           TenantRole[]
  /** Call once on app boot — initialises Keycloak PKCE flow */
  init:    () => Promise<void>
  logout:  () => void
  hasRole: (role: TenantRole) => boolean
}

export const usePortalAuthStore = create<PortalAuthState>((set, get) => ({
  isAuthenticated: false,
  isInitialised:   false,
  accessToken:     null,
  username:        '',
  email:           '',
  tenantId:        null,
  roles:           [],

  init: async () => {
    if (get().isInitialised) return
    if (initPromise) return initPromise

    initPromise = (async () => {
      try {
        const authenticated = await keycloak.init({
          onLoad: 'check-sso',
          pkceMethod: 'S256',
          checkLoginIframe: false,
        })

        if (authenticated && keycloak.token && keycloak.tokenParsed) {
          const profile = keycloak.tokenParsed as Record<string, unknown>
          const realmRoles = (
            (profile?.realm_access as { roles?: string[] })?.roles ?? []
          ) as TenantRole[]

          set({
            isAuthenticated: true,
            isInitialised: true,
            accessToken: keycloak.token,
            username: (profile?.preferred_username as string) ?? '',
            email: (profile?.email as string) ?? '',
            tenantId: (profile?.tenant_id as string) ?? null,
            roles: realmRoles,
          })
        } else {
          set({
            isAuthenticated: false,
            isInitialised: true,
            accessToken: null,
            username: '',
            email: '',
            tenantId: null,
            roles: [],
          })
        }
      } catch {
        set({
          isAuthenticated: false,
          isInitialised: true,
          accessToken: null,
          username: '',
          email: '',
          tenantId: null,
          roles: [],
        })
      } finally {
        initPromise = null
      }
    })()

    return initPromise
  },

  logout: () => keycloak.logout({ redirectUri: window.location.origin }),

  hasRole: (role) => get().roles.includes(role),
}))
