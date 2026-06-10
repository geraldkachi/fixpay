'use client'

import { useState, useEffect } from 'react'
import { useAdminAuth } from '@/lib/admin-auth-context'
import { adminService } from '@/lib/portal-services'

interface ComplianceUser {
  id: string
  first_name: string
  last_name: string
  phone?: string
  email?: string
  kyc_status: string
  pep_hit?: boolean
  sanctions_hit?: boolean
}

export default function AdminCompliancePage() {
  const { token } = useAdminAuth()
  const svc = token ? adminService(token) : null
  const [users, setUsers] = useState<ComplianceUser[]>([])
  const [loading, setLoading] = useState(true)
  const [screening, setScreening] = useState<string | null>(null)
  const [screenResult, setScreenResult] = useState<Record<string, unknown>>({})

  useEffect(() => {
    if (!svc) return
    svc.complianceUsers().then(res => {
      const p = res as { data?: ComplianceUser[] }
      setUsers(p.data ?? (res as ComplianceUser[]))
    }).finally(() => setLoading(false))
  }, [token]) // eslint-disable-line react-hooks/exhaustive-deps

  async function handleScreen(userId: string) {
    if (!svc) return
    setScreening(userId)
    try {
      const res = await svc.screenUser(userId)
      setScreenResult(prev => ({ ...prev, [userId]: res }))
    } finally {
      setScreening(null)
    }
  }

  return (
    <div>
      <h1 className="mb-6 text-2xl font-bold text-gray-900">Compliance</h1>
      {loading ? <p className="text-sm text-gray-400">Loading…</p> : (
        <div className="overflow-x-auto rounded-2xl bg-white shadow-sm">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50">
              <tr>
                {['Name', 'Phone / Email', 'KYC', 'PEP', 'Sanctions', 'Action'].map(h => (
                  <th key={h} className="px-4 py-3 text-left text-xs font-semibold uppercase text-gray-500">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {users.length === 0 && (
                <tr><td colSpan={6} className="px-4 py-6 text-center text-gray-400">No users.</td></tr>
              )}
              {users.map(u => {
                const sr = screenResult[u.id] as { pep_hit?: boolean; sanctions_hit?: boolean } | undefined
                return (
                  <tr key={u.id}>
                    <td className="px-4 py-3 font-medium text-gray-800">{u.first_name} {u.last_name}</td>
                    <td className="px-4 py-3 text-gray-600 text-xs">{u.phone ?? u.email}</td>
                    <td className="px-4 py-3">
                      <span className={`rounded px-2 py-0.5 text-xs font-medium ${
                        u.kyc_status === 'VERIFIED' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'
                      }`}>{u.kyc_status}</span>
                    </td>
                    <td className="px-4 py-3 text-xs">
                      {sr ? (sr.pep_hit ? '⚠️ Yes' : 'No') : '—'}
                    </td>
                    <td className="px-4 py-3 text-xs">
                      {sr ? (sr.sanctions_hit ? '🚫 Yes' : 'No') : '—'}
                    </td>
                    <td className="px-4 py-3">
                      <button
                        onClick={() => handleScreen(u.id)}
                        disabled={screening === u.id}
                        className="text-xs text-blue-600 hover:underline disabled:opacity-50"
                      >
                        {screening === u.id ? 'Screening…' : 'Screen'}
                      </button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
