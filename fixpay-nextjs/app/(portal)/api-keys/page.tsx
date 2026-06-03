'use client'

import { useState, useEffect } from 'react'
import { usePortalAuth } from '@/lib/portal-auth-context'
import { portalService } from '@/lib/portal-services'
import type { ApiKey } from '@/lib/types'

export default function ApiKeysPage() {
  const { token } = usePortalAuth()
  const svc = token ? portalService(token) : null
  const [keys, setKeys] = useState<ApiKey[]>([])
  const [loading, setLoading] = useState(true)
  const [newName, setNewName] = useState('')
  const [env, setEnv] = useState<'sandbox' | 'live'>('sandbox')
  const [plaintext, setPlaintext] = useState('')
  const [creating, setCreating] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!svc) return
    svc.apiKeys().then(res => setKeys(res as ApiKey[])).finally(() => setLoading(false))
  }, [token]) // eslint-disable-line react-hooks/exhaustive-deps

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault()
    if (!svc) return
    setCreating(true)
    setError('')
    try {
      const res = await svc.createApiKey(newName, env) as ApiKey & { plaintext_key: string }
      setPlaintext(res.plaintext_key)
      setKeys(prev => [res, ...prev])
      setNewName('')
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to create key')
    } finally {
      setCreating(false)
    }
  }

  async function handleRevoke(id: string) {
    if (!svc) return
    await svc.revokeApiKey(id)
    setKeys(prev => prev.map(k => k.id === id ? { ...k, is_revoked: true } : k))
  }

  return (
    <div>
      <h1 className="mb-6 text-2xl font-bold text-gray-900">API Keys</h1>

      {/* Create form */}
      <form onSubmit={handleCreate} className="mb-6 flex items-end gap-3 rounded-2xl bg-white p-5 shadow-sm">
        <div className="flex-1">
          <label className="mb-1 block text-sm font-medium text-gray-700">Key name</label>
          <input
            required
            value={newName}
            onChange={e => setNewName(e.target.value)}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <div>
          <label className="mb-1 block text-sm font-medium text-gray-700">Environment</label>
          <select
            value={env}
            onChange={e => setEnv(e.target.value as 'sandbox' | 'live')}
            className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
          >
            <option value="sandbox">Sandbox</option>
            <option value="live">Live</option>
          </select>
        </div>
        <button
          type="submit"
          disabled={creating}
          className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {creating ? 'Creating…' : 'Create'}
        </button>
      </form>

      {/* Plaintext reveal (once) */}
      {plaintext && (
        <div className="mb-6 rounded-xl bg-yellow-50 border border-yellow-200 p-4 text-sm">
          <p className="font-semibold text-yellow-800 mb-1">Save this key now — it will not be shown again</p>
          <code className="break-all text-yellow-900">{plaintext}</code>
          <button onClick={() => setPlaintext('')} className="ml-4 text-xs text-yellow-700 underline">Dismiss</button>
        </div>
      )}

      {error && <p className="mb-4 text-sm text-red-600">{error}</p>}

      {/* Key list */}
      {loading ? (
        <p className="text-sm text-gray-400">Loading…</p>
      ) : (
        <div className="space-y-2">
          {keys.length === 0 && <p className="text-sm text-gray-400">No API keys yet.</p>}
          {keys.map(k => (
            <div key={k.id} className="flex items-center justify-between rounded-xl bg-white p-4 shadow-sm">
              <div>
                <p className="text-sm font-medium text-gray-800">{k.name}</p>
                <p className="text-xs text-gray-400">{k.key_prefix}… · {k.environment}</p>
                {k.last_used_at && <p className="text-xs text-gray-400">Last used: {k.last_used_at}</p>}
              </div>
              <div className="flex items-center gap-3">
                <span className={`rounded px-2 py-0.5 text-xs font-medium ${k.is_revoked ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700'}`}>
                  {k.is_revoked ? 'Revoked' : 'Active'}
                </span>
                {!k.is_revoked && (
                  <button
                    onClick={() => handleRevoke(k.id)}
                    className="rounded text-xs text-red-600 hover:underline"
                  >
                    Revoke
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
