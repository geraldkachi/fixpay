'use client'

import { useState, useEffect } from 'react'
import { usePortalAuth } from '@/lib/portal-auth-context'
import { portalService } from '@/lib/portal-services'

interface SettlementConfig {
  account_number: string
  account_name: string
  bank_code: string
  bank_name: string
  settlement_cycle: string
}

export default function SettlementPage() {
  const { token } = usePortalAuth()
  const svc = token ? portalService(token) : null
  const [form, setForm] = useState<SettlementConfig>({
    account_number: '',
    account_name: '',
    bank_code: '',
    bank_name: '',
    settlement_cycle: 'daily',
  })
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [msg, setMsg] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    if (!svc) return
    svc.settlement().then((res: unknown) => {
      if (res && typeof res === 'object') setForm(f => ({ ...f, ...(res as Partial<SettlementConfig>) }))
    }).finally(() => setLoading(false))
  }, [token]) // eslint-disable-line react-hooks/exhaustive-deps

  async function handleSave(e: React.FormEvent) {
    e.preventDefault()
    if (!svc) return
    setSaving(true)
    setError('')
    setMsg('')
    try {
      await svc.upsertSettlement(form as unknown as Record<string, unknown>)
      setMsg('Settlement account saved.')
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to save')
    } finally {
      setSaving(false)
    }
  }

  const fields: { key: keyof SettlementConfig; label: string }[] = [
    { key: 'account_number', label: 'Account Number' },
    { key: 'account_name', label: 'Account Name' },
    { key: 'bank_code', label: 'Bank Code' },
    { key: 'bank_name', label: 'Bank Name' },
  ]

  if (loading) return <p className="text-sm text-gray-400">Loading…</p>

  return (
    <div>
      <h1 className="mb-6 text-2xl font-bold text-gray-900">Settlement Account</h1>
      <form onSubmit={handleSave} className="max-w-md space-y-4 rounded-2xl bg-white p-6 shadow-sm">
        {fields.map(f => (
          <div key={f.key}>
            <label className="mb-1 block text-sm font-medium text-gray-700">{f.label}</label>
            <input
              type="text"
              required
              value={form[f.key]}
              onChange={e => setForm(prev => ({ ...prev, [f.key]: e.target.value }))}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
        ))}
        <div>
          <label className="mb-1 block text-sm font-medium text-gray-700">Settlement Cycle</label>
          <select
            value={form.settlement_cycle}
            onChange={e => setForm(prev => ({ ...prev, settlement_cycle: e.target.value }))}
            className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
          >
            <option value="daily">Daily</option>
            <option value="weekly">Weekly</option>
            <option value="manual">Manual</option>
          </select>
        </div>
        {error && <p className="text-sm text-red-600">{error}</p>}
        {msg && <p className="text-sm text-green-600">{msg}</p>}
        <button
          type="submit"
          disabled={saving}
          className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {saving ? 'Saving…' : 'Save Settlement Details'}
        </button>
      </form>
    </div>
  )
}
