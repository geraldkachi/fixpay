import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { PageHeader, Card, Badge, Button } from '@/components/ui'
import { CheckCircleIcon, XCircleIcon, RocketLaunchIcon } from '@heroicons/react/24/outline'

interface TenantProfile {
  id: string
  name: string
  status: 'SANDBOX' | 'ACTIVE' | 'SUSPENDED' | 'OFFBOARDED'
  kybStatus: 'PENDING' | 'IN_REVIEW' | 'APPROVED' | 'REJECTED'
  goLiveRequestedAt: string | null
  wentLiveAt: string | null
}

interface SettlementAccount { id: string; verified: boolean }
interface ApiKey { id: string; environment: 'SANDBOX' | 'LIVE'; revokedAt: string | null }

export function GoLiveScreen() {
  const qc = useQueryClient()

  const { data: tenant }     = useQuery<TenantProfile>({
    queryKey: ['portal-me'],
    queryFn: async () => (await api.get<{ data: TenantProfile }>('/portal/me')).data.data,
  })

  const { data: settlement } = useQuery<SettlementAccount | null>({
    queryKey: ['portal-settlement'],
    queryFn: async () => {
      const res = await api.get<{ data?: SettlementAccount | null }>('/portal/settlement')
      return res.data.data ?? null
    },
  })

  const { data: liveKeys = [] } = useQuery<ApiKey[]>({
    queryKey: ['api-keys', 'LIVE'],
    queryFn: async () => (await api.get<{ data: ApiKey[] }>('/portal/api-keys?environment=LIVE')).data.data,
  })

  const requestGoLive = useMutation({
    mutationFn: () => api.post('/portal/me/request-go-live'),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['portal-me'] }),
  })

  if (!tenant) return <div className="p-6 text-slate-400 text-sm">Loading…</div>

  const kybApproved     = tenant.kybStatus === 'APPROVED'
  const hasSettlement   = !!settlement
  const hasLiveKey      = liveKeys.some(k => !k.revokedAt)
  const isLive          = tenant.status === 'ACTIVE'
  const isPending       = !!tenant.goLiveRequestedAt && !isLive
  const allChecksPassed = kybApproved && hasSettlement && hasLiveKey

  const checks: { label: string; done: boolean; detail: string }[] = [
    {
      label: 'KYB Approved',
      done: kybApproved,
      detail: kybApproved ? 'Business verified ✓' : 'Complete and submit KYB for review',
    },
    {
      label: 'Settlement account set',
      done: hasSettlement,
      detail: hasSettlement ? 'Account saved ✓' : 'Add your bank account details',
    },
    {
      label: 'LIVE API key created',
      done: hasLiveKey,
      detail: hasLiveKey ? 'Key exists ✓' : 'Generate at least one LIVE API key',
    },
  ]

  return (
    <div className="p-6 max-w-xl">
      <PageHeader
        title="Go Live"
        subtitle="Complete all requirements to start processing real transactions"
      />

      {isLive && (
        <div className="mb-6 flex items-center gap-3 p-5 bg-emerald-50 border border-emerald-200 rounded-xl">
          <RocketLaunchIcon className="h-8 w-8 text-emerald-500 shrink-0" />
          <div>
            <p className="font-semibold text-emerald-800">You're live! 🎉</p>
            <p className="text-sm text-emerald-600 mt-0.5">
              Approved {tenant.wentLiveAt ? `on ${new Date(tenant.wentLiveAt).toLocaleDateString()}` : ''}.
              You can now process real payments.
            </p>
          </div>
        </div>
      )}

      {isPending && (
        <div className="mb-6 p-4 bg-blue-50 border border-blue-200 rounded-xl text-sm text-blue-700">
          Your go-live request is pending review by the FixPay team. We'll update you shortly.
        </div>
      )}

      <Card className="mb-6">
        <h2 className="text-sm font-semibold text-slate-700 mb-4">Go-Live Checklist</h2>
        <ul className="space-y-4">
          {checks.map(c => (
            <li key={c.label} className="flex items-start gap-3">
              {c.done ? (
                <CheckCircleIcon className="h-5 w-5 text-emerald-500 shrink-0 mt-0.5" />
              ) : (
                <XCircleIcon className="h-5 w-5 text-slate-300 shrink-0 mt-0.5" />
              )}
              <div>
                <p className="text-sm font-medium text-slate-800">{c.label}</p>
                <p className="text-xs text-slate-500 mt-0.5">{c.detail}</p>
              </div>
            </li>
          ))}
        </ul>
      </Card>

      {!isLive && !isPending && (
        <Button
          disabled={!allChecksPassed}
          loading={requestGoLive.isPending}
          onClick={() => requestGoLive.mutate()}
          className="w-full"
        >
          Request Go-Live
        </Button>
      )}

      {!allChecksPassed && !isLive && !isPending && (
        <p className="text-xs text-slate-400 mt-2 text-center">
          Complete all checklist items above to enable this button.
        </p>
      )}
    </div>
  )
}
