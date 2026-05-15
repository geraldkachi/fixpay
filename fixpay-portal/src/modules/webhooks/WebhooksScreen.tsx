import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { api } from '@/lib/api'
import { PageHeader, Card, Badge, Button, Input } from '@/components/ui'
import { TrashIcon, PencilIcon } from '@heroicons/react/24/outline'

interface Webhook {
  id: string
  url: string
  events: string[]
  environment: 'SANDBOX' | 'LIVE'
  active: boolean
  failureCount: number
  lastTriggered: string | null
  createdAt: string
}

type Env = 'SANDBOX' | 'LIVE'

const ALL_EVENTS = [
  'payment.completed',
  'payment.failed',
  'wallet.credited',
  'wallet.debited',
  'kyb.approved',
  'kyb.rejected',
]

export function WebhooksScreen() {
  const qc = useQueryClient()
  const [env, setEnv] = useState<Env>('SANDBOX')
  const [showModal, setShowModal] = useState(false)
  const [editing, setEditing] = useState<Webhook | null>(null)

  const { data: webhooks = [], isLoading } = useQuery<Webhook[]>({
    queryKey: ['webhooks', env],
    queryFn: async () => {
      const res = await api.get<{ data: Webhook[] }>(`/portal/webhooks?environment=${env}`)
      return res.data.data
    },
  })

  const { register, handleSubmit, reset, setValue, formState: { errors } } = useForm<{
    url: string
    events: string[]
  }>()

  function openCreate() {
    setEditing(null)
    reset({ url: '', events: ['payment.completed'] })
    setShowModal(true)
  }

  function openEdit(w: Webhook) {
    setEditing(w)
    reset({ url: w.url, events: w.events })
    setShowModal(true)
  }

  const save = useMutation({
    mutationFn: async (d: { url: string; events: string[] }) => {
      if (editing) {
        return api.patch(`/portal/webhooks/${editing.id}`, { url: d.url, events: d.events, active: true })
      }
      return api.post('/portal/webhooks', { url: d.url, events: d.events, environment: env })
    },
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['webhooks'] }); setShowModal(false) },
  })

  const remove = useMutation({
    mutationFn: (id: string) => api.delete(`/portal/webhooks/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['webhooks'] }),
  })

  return (
    <div className="p-6">
      <PageHeader
        title="Webhooks"
        subtitle="Receive real-time event notifications to your server"
        action={<Button onClick={openCreate}>Add Endpoint</Button>}
      />

      {/* Env tabs */}
      <div className="flex border-b border-slate-200 mb-6">
        {(['SANDBOX', 'LIVE'] as Env[]).map(e => (
          <button key={e} onClick={() => setEnv(e)}
            className={`px-5 py-2 text-sm font-medium border-b-2 -mb-px transition-colors ${
              env === e ? 'border-blue-600 text-blue-600' : 'border-transparent text-slate-500 hover:text-slate-700'
            }`}
          >
            {e === 'SANDBOX' ? '🔶 Sandbox' : '🟢 Live'}
          </button>
        ))}
      </div>

      <Card>
        {isLoading ? (
          <p className="text-slate-400 text-sm">Loading…</p>
        ) : webhooks.length === 0 ? (
          <p className="text-slate-400 text-sm">No webhook endpoints yet.</p>
        ) : (
          <div className="divide-y divide-slate-50">
            {webhooks.map(w => (
              <div key={w.id} className="py-4 flex items-start justify-between gap-4">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <code className="text-sm font-mono text-slate-800 truncate">{w.url}</code>
                    <Badge label={w.active ? 'Active' : 'Disabled'} variant={w.active ? 'green' : 'slate'} />
                  </div>
                  <div className="flex gap-1 flex-wrap mt-1">
                    {w.events.map(ev => (
                      <span key={ev} className="text-xs bg-slate-100 text-slate-600 px-2 py-0.5 rounded">
                        {ev}
                      </span>
                    ))}
                  </div>
                  {w.failureCount > 0 && (
                    <p className="text-xs text-red-500 mt-1">{w.failureCount} delivery failure(s)</p>
                  )}
                </div>
                <div className="flex gap-2 shrink-0">
                  <button onClick={() => openEdit(w)} className="text-slate-400 hover:text-blue-500">
                    <PencilIcon className="h-4 w-4" />
                  </button>
                  <button onClick={() => remove.mutate(w.id)} className="text-slate-400 hover:text-red-500">
                    <TrashIcon className="h-4 w-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>

      {showModal && (
        <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl p-6 w-full max-w-md shadow-xl">
            <h3 className="text-base font-semibold text-slate-800 mb-4">
              {editing ? 'Edit Webhook' : 'Add Webhook Endpoint'}
            </h3>
            <form onSubmit={handleSubmit(d => save.mutate(d))} className="space-y-4">
              <Input
                label="Endpoint URL"
                placeholder="https://yoursite.com/webhooks/fixpay"
                error={errors.url?.message}
                {...register('url', { required: 'URL required' })}
              />
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-2">Events to subscribe</label>
                <div className="space-y-2">
                  {ALL_EVENTS.map(ev => (
                    <label key={ev} className="flex items-center gap-2 text-sm cursor-pointer">
                      <input
                        type="checkbox"
                        value={ev}
                        {...register('events')}
                        className="rounded border-slate-300 text-blue-600 focus:ring-blue-500"
                      />
                      <code className="text-slate-700">{ev}</code>
                    </label>
                  ))}
                </div>
              </div>
              <div className="flex justify-end gap-2">
                <Button variant="ghost" type="button" onClick={() => setShowModal(false)}>Cancel</Button>
                <Button type="submit" loading={save.isPending}>Save</Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
