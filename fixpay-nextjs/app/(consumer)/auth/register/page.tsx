'use client'

import Link from 'next/link'
import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { authService } from '@/lib/services'

export default function RegisterPage() {
  const router = useRouter()
  const [form, setForm] = useState({
    first_name: '',
    last_name: '',
    phone: '',
    email: '',
    password: '',
  })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await authService.register(form)
      router.push(`/consumer/auth/verify-otp?identifier=${encodeURIComponent(form.email || form.phone)}&purpose=EMAIL_VERIFICATION`)
    } catch (err: unknown) {
      const e = err as { errors?: Record<string, string[]>; message?: string }
      const first = Object.values(e.errors ?? {})[0]?.[0]
      setError(first ?? e.message ?? 'Registration failed')
    } finally {
      setLoading(false)
    }
  }

  const field = (name: keyof typeof form, label: string, type = 'text') => (
    <div key={name}>
      <label className="mb-1 block text-sm font-medium text-gray-700">{label}</label>
      <input
        type={type}
        required
        value={form[name]}
        onChange={e => setForm({ ...form, [name]: e.target.value })}
        className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
      />
    </div>
  )

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 px-4">
      <div className="w-full max-w-sm rounded-2xl bg-white p-8 shadow">
        <h1 className="mb-6 text-2xl font-bold text-gray-900">Create account</h1>
        <form onSubmit={handleSubmit} className="space-y-4">
          {field('first_name', 'First name')}
          {field('last_name', 'Last name')}
          {field('phone', 'Phone (+234…)')}
          {field('email', 'Email', 'email')}
          {field('password', 'Password', 'password')}
          {error && <p className="text-sm text-red-600">{error}</p>}
          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-lg bg-green-600 py-2 text-sm font-semibold text-white hover:bg-green-700 disabled:opacity-50"
          >
            {loading ? 'Creating account…' : 'Register'}
          </button>
        </form>
        <p className="mt-4 text-center text-sm text-gray-500">
          Already have an account?{' '}
          <Link href="/consumer/auth/login" className="text-green-600 hover:underline">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  )
}
