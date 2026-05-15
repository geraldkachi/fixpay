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
  const tid = localStorage.getItem('tenant_id')
  if (tid) config.headers['X-Tenant-ID'] = tid
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

      if (refreshing) {
        return new Promise(res => { queue.push(t => { orig.headers.Authorization = `Bearer ${t}`; res(api(orig)) }) })
      }
      refreshing = true
      try {
        // withCredentials sends the httpOnly refresh_token cookie automatically.
        // The server returns a new accessToken in the body AND rotates the cookie.
        const { data } = await axios.post('/api/auth/refresh', {}, { withCredentials: true })
        // Backend wraps in ApiResponse<{accessToken}>: {success, data: {accessToken}}
        const t = (data.data?.accessToken ?? data.accessToken) as string
        // Store the new token in memory only — NOT in localStorage
        useAuthStore.getState().setToken(t)
        queue.forEach(cb => cb(t)); queue = []
        orig.headers.Authorization = `Bearer ${t}`
        return api(orig)
      } catch {
        await serverLogout()
        // Avoid hard-redirecting if already on an auth page (prevents reload loop)
        if (!window.location.pathname.startsWith('/auth')) {
          window.location.href = '/auth/login'
        }
      } finally {
        refreshing = false
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
  purgeDbKey()
  await clearTransactions()
  useAuthStore.getState().logout()
}

export { api }
export default api
