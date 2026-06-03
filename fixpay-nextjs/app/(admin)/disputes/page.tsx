'use client'

import { useState, useEffect } from 'react'
import { useAdminAuth } from '@/lib/admin-auth-context'
import { adminService } from '@/lib/portal-services'

interface Dispute {
  id: string
  category: string
  reason: string
  status: string
  sla_deadline: string
  created_at: string
  user?: { id: string; first_name: string; last_name: string }
}

export default function AdminDisputesPage() {
  const { token } = useAdminAuth()
  const svc = token ? adminService(token) : null
  const [disputes, setDisputes] = useState<Dispute[]>([])
  const [loading, setLoading] = useState(true)
  const [resolving, setResolving] = useState<string | null>(null)
  const [resolution, setResolution] = useState('')

  useEffect(() => {
    if (!svc) return
    svc.disputes().then(res => {
      const p = res as { data?: Dispute[] }
      setDisputes(p.data ?? (res as Dispute[]))
    }).finally(() => setLoading(false))
  }, [token]) // eslint-disable-line react-hooks/exhaustive-deps

  async function handleResolve(id: string, status: string) {
    if (!svc || !resolution.trim()) return
    await svc.resolveDispute(id, { resolution, status })
    setDisputes(prev => prev.map(d => d.id === id ? { ...d, status } : d))
    setResolving(null)
    setResolution('')
  }

  return (
    <div>
      <h1 className="mb-6 text-2xl font-bold text-gray-900">Disputes</h1>
      {loading ? <p className="text-sm text-gray-400">Loading…</p> : (
        <div className="space-y-3">
          {disputes.length === 0 && <p className="text-sm text-gray-400">No disputes.</p>}
          {disputes.map(d => (
            <div key={d.id} className="rounded-2xl bg-white p-5 shadow-sm">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-medium text-gray-800">{d.category}</p>
                  <p className="mt-1 text-sm text-gray-600">{d.reason}</p>
                  {d.user && (
                    <p className="mt-1 text-xs text-gray-400">
                      User: {d.user.first_name} {d.user.last_name}
                    </p>
                  )}
                  <p className="mt-1 text-xs text-gray-400">SLA: {d.sla_deadline}</p>
                </div>
                <span className={`rounded px-2 py-0.5 text-xs font-medium ${
                  d.status === 'RESOLVED' ? 'bg-green-100 text-green-700' :
                  d.status === 'OPEN' ? 'bg-yellow-100 text-yellow-700' : 'bg-blue-100 text-blue-700'
                }`}>{d.status}</span>
              </div>
              {d.status === 'OPEN' || d.status === 'UNDER_REVIEW' ? (
                resolving === d.id ? (
                  <div className="mt-3 space-y-2">
                    <textarea
                      value={resolution}
                      onChange={e => setResolution(e.target.value)}
                      placeholder="Resolution notes…"
                      className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-gray-700"
                      rows={2}
                    />
                    <div className="flex gap-2">
                      <button onClick={() => handleResolve(d.id, 'RESOLVED')}
                        className="rounded bg-green-600 px-3 py-1 text-xs font-semibold text-white hover:bg-green-700">
                        Resolve
                      </button>
                      <button onClick={() => handleResolve(d.id, 'REJECTED')}
                        className="rounded bg-red-600 px-3 py-1 text-xs font-semibold text-white hover:bg-red-700">
                        Reject
                      </button>
                      <button onClick={() => setResolving(null)}
                        className="rounded border px-3 py-1 text-xs text-gray-600">Cancel</button>
                    </div>
                  </div>
                ) : (
                  <button onClick={() => setResolving(d.id)}
                    className="mt-2 text-xs text-blue-600 hover:underline">Resolve / Reject</button>
                )
              ) : null}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
