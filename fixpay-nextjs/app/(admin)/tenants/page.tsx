'use client'

import { useState, useEffect } from 'react'
import { useAdminAuth } from '@/lib/admin-auth-context'
import { adminService } from '@/lib/portal-services'
import type { Tenant, Paginated } from '@/lib/types'

export default function AdminTenantsPage() {
  const { token } = useAdminAuth()
  const svc = token ? adminService(token) : null
  const [tenants, setTenants] = useState<Tenant[]>([])
  const [loading, setLoading] = useState(true)
  const [actionMsg, setActionMsg] = useState<Record<string, string>>({})

  useEffect(() => {
    if (!svc) return
    svc.tenants().then(res => {
      const p = res as Paginated<Tenant>
      setTenants(p.data ?? (res as Tenant[]))
    }).finally(() => setLoading(false))
  }, [token]) // eslint-disable-line react-hooks/exhaustive-deps

  async function handleAction(id: string, action: 'activate' | 'suspend') {
    if (!svc) return
    try {
      if (action === 'activate') await svc.activateTenant(id)
      else await svc.suspendTenant(id)
      setTenants(prev =>
        prev.map(t => t.id === id ? { ...t, status: action === 'activate' ? 'ACTIVE' : 'SUSPENDED' } : t)
      )
      setActionMsg(m => ({ ...m, [id]: `${action === 'activate' ? 'Activated' : 'Suspended'} successfully` }))
    } catch (err: unknown) {
      setActionMsg(m => ({ ...m, [id]: err instanceof Error ? err.message : 'Action failed' }))
    }
  }

  return (
    <div>
      <h1 className="mb-6 text-2xl font-bold text-gray-900">Tenants</h1>
      {loading ? (
        <p className="text-sm text-gray-400">Loading…</p>
      ) : (
        <div className="overflow-x-auto rounded-2xl bg-white shadow-sm">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50">
              <tr>
                {['Name', 'Email', 'Plan', 'Status', 'KYB', 'Actions'].map(h => (
                  <th key={h} className="px-4 py-3 text-left text-xs font-semibold uppercase text-gray-500">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {tenants.length === 0 && (
                <tr><td colSpan={6} className="px-4 py-6 text-center text-gray-400">No tenants.</td></tr>
              )}
              {tenants.map(t => (
                <tr key={t.id}>
                  <td className="px-4 py-3 font-medium text-gray-800">{t.name}</td>
                  <td className="px-4 py-3 text-gray-600">{t.email}</td>
                  <td className="px-4 py-3 text-gray-600">{t.plan}</td>
                  <td className="px-4 py-3">
                    <span className={`rounded px-2 py-0.5 text-xs font-medium ${
                      t.status === 'ACTIVE' ? 'bg-green-100 text-green-700' :
                      t.status === 'SUSPENDED' ? 'bg-red-100 text-red-700' : 'bg-yellow-100 text-yellow-700'
                    }`}>{t.status}</span>
                  </td>
                  <td className="px-4 py-3">
                    <span className={`rounded px-2 py-0.5 text-xs font-medium ${
                      t.kyb_status === 'APPROVED' ? 'bg-green-100 text-green-700' :
                      t.kyb_status === 'REJECTED' ? 'bg-red-100 text-red-700' : 'bg-yellow-100 text-yellow-700'
                    }`}>{t.kyb_status}</span>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      {t.status !== 'ACTIVE' && (
                        <button onClick={() => handleAction(t.id, 'activate')}
                          className="text-xs text-green-600 hover:underline">Activate</button>
                      )}
                      {t.status === 'ACTIVE' && (
                        <button onClick={() => handleAction(t.id, 'suspend')}
                          className="text-xs text-red-600 hover:underline">Suspend</button>
                      )}
                      {actionMsg[t.id] && <span className="text-xs text-gray-500">{actionMsg[t.id]}</span>}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
