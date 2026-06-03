/**
 * Thin Axios-style fetch wrapper for the Laravel API.
 * Consumer routes use cookie-based Sanctum SPA auth.
 * Portal/admin routes use Bearer tokens.
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
  }

  const res = await fetch(`${API_URL}${path}`, {
    credentials: 'include', // send Sanctum cookies
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
