import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { api } from '@/lib/api'
import { PageHeader, Card, Badge, Button, Input } from '@/components/ui'
import { TrashIcon, EyeIcon, EyeSlashIcon } from '@heroicons/react/24/outline'

interface ApiKey {
  id: string
  name: string
  environment: 'SANDBOX' | 'LIVE'
  keyPrefix: string
  scopes: string[]
  lastUsedAt: string | null
  expiresAt: string | null
  revokedAt: string | null
  createdAt: string
}

type Env = 'SANDBOX' | 'LIVE'

export function ApiKeysScreen() {
  const qc = useQueryClient()
  const [env, setEnv] = useState<Env>('SANDBOX')
  const [newKey, setNewKey] = useState<string | null>(null)
  const [showModal, setShowModal] = useState(false)

  const { data: keys = [], isLoading } = useQuery<ApiKey[]>({
    queryKey: ['api-keys', env],
    queryFn: async () => {
      const res = await api.get<{ data: ApiKey[] }>(`/portal/api-keys?environment=${env}`)
      return res.data.data
    },
  })

  const { register, handleSubmit, reset, formState: { errors } } = useForm<{ name: string }>()

  const generate = useMutation({
    mutationFn: async (data: { name: string }) => {
      const res = await api.post<{ data: { key: string } }>('/portal/api-keys', {
        name: data.name,
        environment: env,
      })
      return res.data.data.key
    },
    onSuccess: (rawKey) => {
      setNewKey(rawKey)
      reset()
      qc.invalidateQueries({ queryKey: ['api-keys'] })
    },
  })

  const revoke = useMutation({
    mutationFn: (id: string) => api.delete(`/portal/api-keys/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['api-keys'] }),
  })

  return (
    <div className="p-6">
      <PageHeader
        title="API Keys"
        subtitle="Manage test and production keys for accessing the FixPay API"
        action={
          <Button onClick={() => setShowModal(true)}>Generate Key</Button>
        }
      />

      {/* Env tabs */}
      <div className="flex border-b border-slate-200 mb-6">
        {(['SANDBOX', 'LIVE'] as Env[]).map(e => (
          <button
            key={e}
            onClick={() => setEnv(e)}
            className={`px-5 py-2 text-sm font-medium border-b-2 -mb-px transition-colors ${
              env === e ? 'border-blue-600 text-blue-600' : 'border-transparent text-slate-500 hover:text-slate-700'
            }`}
          >
            {e === 'SANDBOX' ? '🔶 Sandbox' : '🟢 Live'}
          </button>
        ))}
      </div>

      {/* Raw key reveal after generation */}
      {newKey && (
        <div className="mb-6 bg-amber-50 border border-amber-200 rounded-xl p-4">
          <p className="text-sm font-semibold text-amber-800 mb-2">
            ⚠️ Copy this key now — it will never be shown again.
          </p>
          <div className="flex items-center gap-2">
            <code className="flex-1 text-sm bg-white border border-amber-200 rounded px-3 py-2 font-mono break-all">
              {newKey}
            </code>
            <button
              onClick={() => { void navigator.clipboard.writeText(newKey); setNewKey(null) }}
              className="text-xs px-3 py-2 bg-amber-600 text-white rounded-lg hover:bg-amber-700"
            >
              Copy & Dismiss
            </button>
          </div>
        </div>
      )}

      {/* Keys list */}
      <Card>
        {isLoading ? (
          <p className="text-slate-400 text-sm">Loading…</p>
        ) : keys.length === 0 ? (
          <p className="text-slate-400 text-sm">No {env.toLowerCase()} keys yet.</p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-xs text-slate-500 border-b border-slate-100">
                <th className="text-left py-2 pr-4">Name</th>
                <th className="text-left py-2 pr-4">Key prefix</th>
                <th className="text-left py-2 pr-4">Status</th>
                <th className="text-left py-2 pr-4">Last used</th>
                <th />
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50">
              {keys.map(k => (
                <tr key={k.id}>
                  <td className="py-3 pr-4 font-medium text-slate-800">{k.name}</td>
                  <td className="py-3 pr-4 font-mono text-slate-600">{k.keyPrefix}…</td>
                  <td className="py-3 pr-4">
                    {k.revokedAt ? (
                      <Badge label="Revoked" variant="red" />
                    ) : (
                      <Badge label="Active" variant="green" />
                    )}
                  </td>
                  <td className="py-3 pr-4 text-slate-500">
                    {k.lastUsedAt ? new Date(k.lastUsedAt).toLocaleDateString() : 'Never'}
                  </td>
                  <td className="py-3 text-right">
                    {!k.revokedAt && (
                      <button
                        onClick={() => revoke.mutate(k.id)}
                        className="text-red-400 hover:text-red-600 transition-colors"
                        title="Revoke"
                      >
                        <TrashIcon className="h-4 w-4" />
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>

      {/* Generate modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl p-6 w-full max-w-sm shadow-xl">
            <h3 className="text-base font-semibold text-slate-800 mb-4">Generate {env} key</h3>
            <form
              onSubmit={handleSubmit(d => {
                generate.mutate(d)
                setShowModal(false)
              })}
              className="space-y-4"
            >
              <Input
                label="Key name"
                placeholder="e.g. Mobile app"
                error={errors.name?.message}
                {...register('name', { required: 'Name required' })}
              />
              <div className="flex justify-end gap-2">
                <Button variant="ghost" type="button" onClick={() => setShowModal(false)}>Cancel</Button>
                <Button type="submit" loading={generate.isPending}>Generate</Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
