'use client'

import Link from 'next/link'
import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { authService } from '@/lib/services'
import { useAuth } from '@/lib/auth-context'
import { APP_DEMO_USERS } from '@/lib/demo-accounts'
import type { User } from '@/lib/types'

export default function LoginPage() {
  const router = useRouter()
  const { setAuth } = useAuth()
  const [form, setForm] = useState({ identifier: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const res = await authService.login(form)
      setAuth(res.user as User, res.access_token)
      router.push('/consumer/dashboard')
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 px-4">
      <div className="w-full max-w-sm rounded-2xl bg-white p-8 shadow">
        <h1 className="mb-6 text-2xl font-bold text-gray-900">Sign in to FixPay</h1>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-gray-500">Use seeded user</p>
            <div className="grid gap-2">
              {APP_DEMO_USERS.map(user => (
                <button
                  key={user.label}
                  type="button"
                  onClick={() => setForm({ identifier: user.identifier, password: user.password })}
                  className="rounded-lg border border-green-200 bg-green-50 px-3 py-2 text-left text-xs text-green-700 hover:bg-green-100"
                >
                  <span className="block font-semibold">{user.label}</span>
                  <span>{user.identifier}</span>
                </button>
              ))}
            </div>
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">Phone or Email</label>
            <input
              type="text"
              required
              value={form.identifier}
              onChange={e => setForm({ ...form, identifier: e.target.value })}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">Password</label>
            <input
              type="password"
              required
              value={form.password}
              onChange={e => setForm({ ...form, password: e.target.value })}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
            />
          </div>
          {error && <p className="text-sm text-red-600">{error}</p>}
          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-lg bg-green-600 py-2 text-sm font-semibold text-white hover:bg-green-700 disabled:opacity-50"
          >
            {loading ? 'Signing in…' : 'Sign in'}
          </button>
        </form>
        <p className="mt-4 text-center text-sm text-gray-500">
          No account?{' '}
          <Link href="/consumer/auth/register" className="text-green-600 hover:underline">
            Register
          </Link>
        </p>
      </div>
    </div>
  )
}
