/**
 * Thin Axios-style fetch wrapper for the Laravel API.
 * All routes use Bearer token auth.
 */

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8000/api'

type RequestOptions = Omit<RequestInit, 'body'> & {
  body?: unknown
  token?: string
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { body, token, headers: extraHeaders, ...rest } = options

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'application/json',
    ...(extraHeaders as Record<string, string> | undefined),
  }

  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  } else if (typeof window !== 'undefined') {
    // Auto-read whichever token is stored for the current role
    const raw =
      localStorage.getItem('fp_token') ??
      localStorage.getItem('fp_portal_token') ??
      localStorage.getItem('fp_admin_token')
    if (raw) {
      try {
        const parsed = JSON.parse(raw)
        const t = parsed?.token ?? parsed?.access_token ?? raw
        headers['Authorization'] = `Bearer ${t}`
      } catch {
        headers['Authorization'] = `Bearer ${raw}`
      }
    }
  }

  const res = await fetch(`${API_URL}${path}`, {
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
    ...rest,
  })

  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }))
    throw Object.assign(new Error(err.message ?? 'Request failed'), {
      status: res.status,
      errors: err.errors,
    })
  }

  if (res.status === 204) return undefined as T
  return res.json() as Promise<T>
}

export const api = {
  get:    <T>(path: string, opts?: RequestOptions) => request<T>(path, { method: 'GET', ...opts }),
  post:   <T>(path: string, body?: unknown, opts?: RequestOptions) => request<T>(path, { method: 'POST', body, ...opts }),
  put:    <T>(path: string, body?: unknown, opts?: RequestOptions) => request<T>(path, { method: 'PUT', body, ...opts }),
  patch:  <T>(path: string, body?: unknown, opts?: RequestOptions) => request<T>(path, { method: 'PATCH', body, ...opts }),
  delete: <T>(path: string, opts?: RequestOptions) => request<T>(path, { method: 'DELETE', ...opts }),
}
