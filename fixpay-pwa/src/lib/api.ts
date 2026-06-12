import axios from 'axios'
import { useAuthStore } from '@/store/auth.store'
import { purgeDbKey } from '@/lib/crypto'
import { clearTransactions } from '@/lib/db'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '/api',
  timeout: 30_000,
  // Always send cookies so the httpOnly refresh_token cookie is included
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  // In mock/dev mode the token lives in memory (MSW can't set httpOnly cookies).
  // In production the httpOnly cookie is sent automatically by the browser;
  // the Authorization header is kept as a fallback for clients that prefer it.
  const token = useAuthStore.getState().token
  if (token) config.headers.Authorization = `Bearer ${token}`
  const fp = localStorage.getItem('device_fp')
  if (fp) config.headers['X-Device-Fingerprint'] = fp
  // tenant_slug is set server-side at login from user.tenant_id FK — not from client input.
  // Sending it as a header scopes tenant/config and portal calls to the correct tenant.
  const tenantSlug = localStorage.getItem('tenant_slug')
  if (tenantSlug) config.headers['X-Tenant-Slug'] = tenantSlug
  return config
})

let refreshing = false
let queue: Array<(t: string) => void> = []

api.interceptors.response.use(
  r => r,
  async (err) => {
    const orig = err.config
    if (err.response?.status === 401 && !orig._retry) {
      orig._retry = true

      // Only attempt a token refresh when the user has an active session.
      // Skipping this for unauthenticated requests (login page, splash, tenant-config)
      // prevents an infinite reload loop: 401 → refresh fails → hard redirect → 401 → …
      const { token, isAuthenticated } = useAuthStore.getState()
      if (!token && !isAuthenticated) return Promise.reject(err)

      try {
        await serverLogout()
      } finally {
        // Avoid hard-redirecting if already on an auth page (prevents reload loop)
        if (!window.location.pathname.startsWith('/auth')) {
          window.location.href = '/auth/login'
        }
      }
    }
    return Promise.reject(err)
  }
)

/**
 * Full logout:
 * 1. Tell the server to clear the httpOnly refresh_token cookie.
 * 2. Wipe the AES encryption key so all cached IndexedDB data becomes unreadable.
 * 3. Drop the IndexedDB transaction table.
 * 4. Clear Zustand auth state.
 */
export async function serverLogout(): Promise<void> {
  try {
    await axios.post('/api/auth/logout', {}, { withCredentials: true })
  } catch {
    // best-effort — proceed with local cleanup regardless
  }
  // Clear server-authoritative tenant context so a new login starts clean
  localStorage.removeItem('tenant_slug')
  purgeDbKey()
  await clearTransactions()
  useAuthStore.getState().logout()
}

export { api }
export default api
