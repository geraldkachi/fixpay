'use client'

import { useState, useEffect } from 'react'
import { usePortalAuth } from '@/lib/portal-auth-context'
import { portalService } from '@/lib/portal-services'

interface WebhookConfig {
  id?: string
  url: string
  events: string[]
}

const EVENT_OPTIONS = [
  'payment.completed', 'payment.failed',
  'transfer.completed', 'transfer.failed',
  'kyc.approved', 'kyc.rejected',
]

export default function WebhooksPage() {
  const { token } = usePortalAuth()
  const svc = token ? portalService(token) : null
  const [webhook, setWebhook] = useState<WebhookConfig>({ url: '', events: [] })
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [msg, setMsg] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    if (!svc) return
    svc.webhooks().then((res: unknown) => {
      if (res && typeof res === 'object') {
        const w = res as WebhookConfig
        setWebhook({ url: w.url ?? '', events: w.events ?? [] })
      }
    }).finally(() => setLoading(false))
  }, [token]) // eslint-disable-line react-hooks/exhaustive-deps

  function toggleEvent(ev: string) {
    setWebhook(w => ({
      ...w,
      events: w.events.includes(ev) ? w.events.filter(e => e !== ev) : [...w.events, ev],
    }))
  }

  async function handleSave(e: React.FormEvent) {
    e.preventDefault()
    if (!svc) return
    setSaving(true)
    setError('')
    setMsg('')
    try {
      await svc.upsertWebhook(webhook.url, webhook.events)
      setMsg('Webhook configuration saved.')
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to save')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <p className="text-sm text-gray-400">Loading…</p>

  return (
    <div>
      <h1 className="mb-6 text-2xl font-bold text-gray-900">Webhooks</h1>
      <form onSubmit={handleSave} className="max-w-lg space-y-5 rounded-2xl bg-white p-6 shadow-sm">
        <div>
          <label className="mb-1 block text-sm font-medium text-gray-700">Endpoint URL</label>
          <input
            type="url"
            required
            value={webhook.url}
            onChange={e => setWebhook(w => ({ ...w, url: e.target.value }))}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            placeholder="https://yourdomain.com/webhooks/fixpay"
          />
        </div>
        <div>
          <p className="mb-2 text-sm font-medium text-gray-700">Events to subscribe</p>
          <div className="grid grid-cols-2 gap-2">
            {EVENT_OPTIONS.map(ev => (
              <label key={ev} className="flex items-center gap-2 text-sm text-gray-700">
                <input
                  type="checkbox"
                  checked={webhook.events.includes(ev)}
                  onChange={() => toggleEvent(ev)}
                  className="rounded border-gray-300"
                />
                {ev}
              </label>
            ))}
          </div>
        </div>
        {error && <p className="text-sm text-red-600">{error}</p>}
        {msg && <p className="text-sm text-green-600">{msg}</p>}
        <button
          type="submit"
          disabled={saving}
          className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {saving ? 'Saving…' : 'Save Webhook'}
        </button>
      </form>
    </div>
  )
}
