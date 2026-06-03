'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { authService } from '@/lib/services'
import { useAdminAuth } from '@/lib/admin-auth-context'
import type { User } from '@/lib/types'

export default function AdminLoginPage() {
  const router = useRouter()
  const { setAdminAuth } = useAdminAuth()
  const [form, setForm] = useState({ login: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const res = await authService.login(form)
      setAdminAuth(res.user as User, res.token)
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
          {['login', 'password'].map(f => (
            <div key={f}>
              <label className="mb-1 block text-sm font-medium capitalize text-gray-700">{f === 'login' ? 'Email' : 'Password'}</label>
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
