'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { authService } from '@/lib/services'
import { useAdminAuth } from '@/lib/admin-auth-context'
import { ADMIN_DEMO_USERS } from '@/lib/demo-accounts'
import type { User } from '@/lib/types'

export default function AdminLoginPage() {
  const router = useRouter()
  const { setAdminAuth } = useAdminAuth()
  const [form, setForm] = useState({ identifier: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const res = await authService.login(form)
      setAdminAuth(res.user as User, res.access_token)
      router.push('/admin/tenants')
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-100 px-4">
      <div className="w-full max-w-sm rounded-2xl bg-white p-8 shadow">
        <h1 className="mb-6 text-2xl font-bold text-gray-900">Admin Sign In</h1>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-gray-500">Use seeded admin user</p>
            <div className="grid gap-2">
              {ADMIN_DEMO_USERS.map(user => (
                <button
                  key={user.label}
                  type="button"
                  onClick={() => setForm({ identifier: user.identifier, password: user.password })}
                  className="rounded-lg border border-gray-300 bg-gray-100 px-3 py-2 text-left text-xs text-gray-700 hover:bg-gray-200"
                >
                  <span className="block font-semibold">{user.label}</span>
                  <span>{user.identifier}</span>
                </button>
              ))}
            </div>
          </div>
          {['identifier', 'password'].map(f => (
            <div key={f}>
              <label className="mb-1 block text-sm font-medium capitalize text-gray-700">{f === 'identifier' ? 'Email or Phone' : 'Password'}</label>
              <input
                type={f === 'password' ? 'password' : 'text'}
                required
                value={form[f as keyof typeof form]}
                onChange={e => setForm({ ...form, [f]: e.target.value })}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-gray-800"
              />
            </div>
          ))}
          {error && <p className="text-sm text-red-600">{error}</p>}
          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-lg bg-gray-900 py-2 text-sm font-semibold text-white hover:bg-gray-800 disabled:opacity-50"
          >
            {loading ? 'Signing in…' : 'Sign in'}
          </button>
        </form>
      </div>
    </div>
  )
}
