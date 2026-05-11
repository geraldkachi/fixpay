import axios from 'axios'
import { useAuthStore } from '@/store/auth.store'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '/api',
  timeout: 30_000,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
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
      if (refreshing) {
        return new Promise(res => { queue.push(t => { orig.headers.Authorization = `Bearer ${t}`; res(api(orig)) }) })
      }
      refreshing = true
      try {
        const { data } = await axios.post('/api/auth/refresh', {}, { withCredentials: true })
        const t = data.accessToken as string
        useAuthStore.getState().setToken(t)
        queue.forEach(cb => cb(t)); queue = []
        orig.headers.Authorization = `Bearer ${t}`
        return api(orig)
      } catch {
        useAuthStore.getState().logout()
        window.location.href = '/auth/login'
      } finally {
        refreshing = false
      }
    }
    return Promise.reject(err)
  }
)

export { api }
export default api
