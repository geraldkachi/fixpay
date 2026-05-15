import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { api } from '@/lib/api'
import { PageHeader, Card, Badge, Button, Input } from '@/components/ui'
import { TrashIcon, ShieldCheckIcon } from '@heroicons/react/24/outline'

interface IpRule {
  id: string
  ipCidr: string
  label: string
  environment: 'SANDBOX' | 'LIVE'
  active: boolean
  createdAt: string
}

type Env = 'SANDBOX' | 'LIVE'

export function IpWhitelistScreen() {
  const qc = useQueryClient()
  const [env, setEnv] = useState<Env>('SANDBOX')
  const [showModal, setShowModal] = useState(false)

  const { data: rules = [], isLoading } = useQuery<IpRule[]>({
    queryKey: ['ip-whitelist', env],
    queryFn: async () => {
      const res = await api.get<{ data: IpRule[] }>(`/portal/ip-whitelist?environment=${env}`)
      return res.data.data
    },
  })

  const { register, handleSubmit, reset, formState: { errors } } = useForm<{
    ipCidr: string
    label: string
  }>()

  const add = useMutation({
    mutationFn: (d: { ipCidr: string; label: string }) =>
      api.post('/portal/ip-whitelist', { ...d, environment: env }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['ip-whitelist'] }); setShowModal(false); reset() },
  })

  const remove = useMutation({
    mutationFn: (id: string) => api.delete(`/portal/ip-whitelist/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['ip-whitelist'] }),
  })

  return (
    <div className="p-6">
      <PageHeader
        title="IP Whitelist"
        subtitle="Restrict API access to specific IP addresses or CIDR ranges"
        action={<Button onClick={() => { reset(); setShowModal(true) }}>Add Rule</Button>}
      />

      <div className="mb-4 p-3 bg-blue-50 border border-blue-200 rounded-lg text-sm text-blue-700">
        <ShieldCheckIcon className="h-4 w-4 inline mr-1" />
        If any rules exist for an environment, requests from non-listed IPs will be rejected.
      </div>

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
        ) : rules.length === 0 ? (
          <p className="text-slate-400 text-sm">No IP rules. API is accessible from any IP.</p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-xs text-slate-500 border-b border-slate-100">
                <th className="text-left py-2 pr-4">CIDR / IP</th>
                <th className="text-left py-2 pr-4">Label</th>
                <th className="text-left py-2 pr-4">Status</th>
                <th className="text-left py-2 pr-4">Added</th>
                <th />
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50">
              {rules.map(r => (
                <tr key={r.id}>
                  <td className="py-3 pr-4 font-mono text-slate-800">{r.ipCidr}</td>
                  <td className="py-3 pr-4 text-slate-600">{r.label}</td>
                  <td className="py-3 pr-4">
                    <Badge label={r.active ? 'Active' : 'Disabled'} variant={r.active ? 'green' : 'slate'} />
                  </td>
                  <td className="py-3 pr-4 text-slate-500">{new Date(r.createdAt).toLocaleDateString()}</td>
                  <td className="py-3 text-right">
                    <button onClick={() => remove.mutate(r.id)} className="text-slate-400 hover:text-red-500">
                      <TrashIcon className="h-4 w-4" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>

      {showModal && (
        <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl p-6 w-full max-w-sm shadow-xl">
            <h3 className="text-base font-semibold text-slate-800 mb-4">Add IP Rule</h3>
            <form onSubmit={handleSubmit(d => add.mutate(d))} className="space-y-4">
              <Input
                label="IP address or CIDR"
                placeholder="203.0.113.0/24 or 1.2.3.4"
                error={errors.ipCidr?.message}
                {...register('ipCidr', { required: 'Required' })}
              />
              <Input
                label="Label (optional)"
                placeholder="Office IP"
                {...register('label')}
              />
              <div className="flex justify-end gap-2">
                <Button variant="ghost" type="button" onClick={() => setShowModal(false)}>Cancel</Button>
                <Button type="submit" loading={add.isPending}>Add</Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
