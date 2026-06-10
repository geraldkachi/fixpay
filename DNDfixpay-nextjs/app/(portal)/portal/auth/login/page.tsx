'use client'

import Link from 'next/link'
import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { portalAuthService } from '@/lib/portal-services'
import { usePortalAuth } from '@/lib/portal-auth-context'
import { PORTAL_DEMO_USERS } from '@/lib/demo-accounts'
import type { Tenant } from '@/lib/types'

export default function PortalLoginPage() {
  const router = useRouter()
  const { setPortalAuth } = usePortalAuth()
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const res = await portalAuthService.login(form)
      setPortalAuth(res.tenant as Tenant, res.token)
      router.push('/portal/dashboard')
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 px-4">
      <div className="w-full max-w-sm rounded-2xl bg-white p-8 shadow">
        <h1 className="mb-2 text-2xl font-bold text-gray-900">Portal Sign In</h1>
        <p className="mb-6 text-sm text-gray-500">Developer &amp; Business Portal</p>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-gray-500">Use seeded portal user</p>
            <div className="grid gap-2">
              {PORTAL_DEMO_USERS.map(user => (
                <button
                  key={user.label}
                  type="button"
                  onClick={() => setForm({ email: user.identifier, password: user.password })}
                  className="rounded-lg border border-blue-200 bg-blue-50 px-3 py-2 text-left text-xs text-blue-700 hover:bg-blue-100"
                >
                  <span className="block font-semibold">{user.label}</span>
                  <span>{user.identifier}</span>
                </button>
              ))}
            </div>
          </div>
          {['email', 'password'].map(f => (
            <div key={f}>
              <label className="mb-1 block text-sm font-medium capitalize text-gray-700">{f}</label>
              <input
                type={f}
                required
                value={form[f as keyof typeof form]}
                onChange={e => setForm({ ...form, [f]: e.target.value })}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          ))}
          {error && <p className="text-sm text-red-600">{error}</p>}
          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-lg bg-blue-600 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {loading ? 'Signing in…' : 'Sign in'}
          </button>
        </form>
        <p className="mt-4 text-center text-sm text-gray-500">
          No account?{' '}
          <Link href="/portal/auth/register" className="text-blue-600 hover:underline">
            Register
          </Link>
        </p>
      </div>
    </div>
  )
}
