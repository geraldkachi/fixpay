'use client'

import { useState } from 'react'
import { usePortalAuth } from '@/lib/portal-auth-context'
import { portalService } from '@/lib/portal-services'

const KYB_FIELDS = [
  { key: 'rc_number', label: 'RC Number' },
  { key: 'business_address', label: 'Business Address' },
  { key: 'director_name', label: 'Director Full Name' },
  { key: 'director_bvn', label: 'Director BVN' },
]

export default function KybPage() {
  const { token } = usePortalAuth()
  const [form, setForm] = useState<Record<string, string>>(
    Object.fromEntries(KYB_FIELDS.map(f => [f.key, '']))
  )
  const [loading, setLoading] = useState(false)
  const [success, setSuccess] = useState('')
  const [error, setError] = useState('')

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!token) return
    setLoading(true)
    setError('')
    setSuccess('')
    try {
      await portalService(token).kyb(form)
      setSuccess('KYB documents submitted. We will review and notify you within 24 hours.')
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Submission failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <h1 className="mb-2 text-2xl font-bold text-gray-900">KYB Verification</h1>
      <p className="mb-6 text-sm text-gray-500">Submit your business documents to unlock production access.</p>

      <form onSubmit={handleSubmit} className="max-w-md space-y-4 rounded-2xl bg-white p-6 shadow-sm">
        {KYB_FIELDS.map(f => (
          <div key={f.key}>
            <label className="mb-1 block text-sm font-medium text-gray-700">{f.label}</label>
            <input
              type="text"
              required
              value={form[f.key]}
              onChange={e => setForm({ ...form, [f.key]: e.target.value })}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
        ))}
        {error && <p className="text-sm text-red-600">{error}</p>}
        {success && <p className="text-sm text-green-600">{success}</p>}
        <button
          type="submit"
          disabled={loading}
          className="w-full rounded-lg bg-blue-600 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {loading ? 'Submitting…' : 'Submit KYB'}
        </button>
      </form>
    </div>
  )
}
