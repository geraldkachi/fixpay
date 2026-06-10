'use client'

import { useState, useEffect } from 'react'
import { usePortalAuth } from '@/lib/portal-auth-context'
import { portalService } from '@/lib/portal-services'
import type { Tenant } from '@/lib/types'

export default function PortalDashboardPage() {
  const { tenant, token } = usePortalAuth()
  const [profile, setProfile] = useState<Tenant | null>(null)
  const [goLiveLoading, setGoLiveLoading] = useState(false)
  const [msg, setMsg] = useState('')

  useEffect(() => {
    if (!token) return
    portalService(token).profile().then(setProfile).catch(() => null)
  }, [token])

  async function requestGoLive() {
    if (!token) return
    setGoLiveLoading(true)
    try {
      const res = await portalService(token).requestGoLive()
      setMsg(res.message)
    } catch (err: unknown) {
      setMsg(err instanceof Error ? err.message : 'Failed to request go-live')
    } finally {
      setGoLiveLoading(false)
    }
  }

  const t = profile ?? tenant

  return (
    <div>
      <h1 className="mb-6 text-2xl font-bold text-gray-900">Dashboard</h1>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        {[
          { label: 'Account Status', value: t?.status },
          { label: 'Plan', value: t?.plan },
          { label: 'KYB Status', value: t?.kyb_status },
        ].map(card => (
          <div key={card.label} className="rounded-2xl bg-white p-5 shadow-sm">
            <p className="text-xs text-gray-500">{card.label}</p>
            <p className="mt-1 text-lg font-semibold text-gray-900">{card.value ?? '—'}</p>
          </div>
        ))}
      </div>

      {t?.kyb_status === 'APPROVED' && t?.status === 'SANDBOX' && (
        <div className="mt-6 rounded-2xl bg-white p-5 shadow-sm">
          <h2 className="mb-2 font-semibold text-gray-900">Go Live</h2>
          <p className="mb-4 text-sm text-gray-500">Your KYB is approved. You can now request production access.</p>
          {msg && <p className="mb-3 text-sm text-green-600">{msg}</p>}
          <button
            onClick={requestGoLive}
            disabled={goLiveLoading}
            className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {goLiveLoading ? 'Requesting…' : 'Request Go Live'}
          </button>
        </div>
      )}
    </div>
  )
}
