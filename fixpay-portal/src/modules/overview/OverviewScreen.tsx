import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api } from '@/lib/api'
import { PageHeader, Card, Badge } from '@/components/ui'
import { formatCurrency } from '@/lib/utils'
import {
  KeyIcon,
  GlobeAltIcon,
  IdentificationIcon,
  BanknotesIcon,
  RocketLaunchIcon,
  CheckCircleIcon,
} from '@heroicons/react/24/outline'

interface TenantProfile {
  id: string
  name: string
  slug: string
  status: 'SANDBOX' | 'ACTIVE' | 'SUSPENDED' | 'OFFBOARDED'
  kybStatus: 'PENDING' | 'IN_REVIEW' | 'APPROVED' | 'REJECTED'
  plan: string
  sandboxWalletBalance: number
  goLiveRequestedAt: string | null
  wentLiveAt: string | null
}

const statusVariant: Record<string, 'blue' | 'green' | 'yellow' | 'red'> = {
  SANDBOX:    'blue',
  ACTIVE:     'green',
  SUSPENDED:  'yellow',
  OFFBOARDED: 'red',
}

const checklist = [
  { label: 'Generate a SANDBOX API key',  to: '/api-keys',    field: null },
  { label: 'Add a webhook endpoint',       to: '/webhooks',    field: null },
  { label: 'Complete KYB verification',    to: '/kyb',         field: 'kybApproved' },
  { label: 'Set up settlement account',    to: '/settlement',  field: null },
  { label: 'Request go-live',              to: '/go-live',     field: 'goLiveRequested' },
]

export function OverviewScreen() {
  const { data: tenant, isLoading } = useQuery<TenantProfile>({
    queryKey: ['portal-me'],
    queryFn: async () => {
      const res = await api.get<{ data: TenantProfile }>('/portal/me')
      return res.data.data
    },
  })

  if (isLoading || !tenant) {
    return <div className="p-6 text-slate-400 text-sm">Loading…</div>
  }

  const kybApproved     = tenant.kybStatus === 'APPROVED'
  const goLiveRequested = !!tenant.goLiveRequestedAt

  const checklistDone: Record<string, boolean> = {
    kybApproved,
    goLiveRequested,
  }

  return (
    <div className="p-6">
      <PageHeader
        title={`Welcome, ${tenant.name}`}
        subtitle={`@${tenant.slug} · ${tenant.plan} plan`}
        action={<Badge label={tenant.status} variant={statusVariant[tenant.status] ?? 'slate'} />}
      />

      {/* Stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        <StatCard
          icon={<BanknotesIcon className="h-5 w-5 text-blue-500" />}
          label="Sandbox Balance"
          value={formatCurrency(tenant.sandboxWalletBalance)}
        />
        <StatCard
          icon={<IdentificationIcon className="h-5 w-5 text-purple-500" />}
          label="KYB Status"
          value={tenant.kybStatus.replace('_', ' ')}
        />
        <StatCard
          icon={<RocketLaunchIcon className="h-5 w-5 text-emerald-500" />}
          label="Environment"
          value={tenant.status}
        />
        <StatCard
          icon={<KeyIcon className="h-5 w-5 text-amber-500" />}
          label="Portal ID"
          value={tenant.slug}
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Onboarding checklist */}
        <Card>
          <h2 className="text-sm font-semibold text-slate-700 mb-4">Onboarding Checklist</h2>
          <ul className="space-y-3">
            {checklist.map(({ label, to, field }) => {
              const done = field ? checklistDone[field] : false
              return (
                <li key={label} className="flex items-center gap-3">
                  <CheckCircleIcon
                    className={`h-5 w-5 shrink-0 ${done ? 'text-emerald-500' : 'text-slate-300'}`}
                  />
                  <Link to={to} className="text-sm text-slate-700 hover:text-blue-600 transition-colors">
                    {label}
                  </Link>
                </li>
              )
            })}
          </ul>
        </Card>

        {/* Quick links */}
        <Card>
          <h2 className="text-sm font-semibold text-slate-700 mb-4">Quick Actions</h2>
          <div className="grid grid-cols-2 gap-3">
            {[
              { label: 'API Keys',    to: '/api-keys',    icon: KeyIcon },
              { label: 'Webhooks',    to: '/webhooks',    icon: GlobeAltIcon },
              { label: 'KYB',         to: '/kyb',         icon: IdentificationIcon },
              { label: 'Go Live',     to: '/go-live',     icon: RocketLaunchIcon },
            ].map(({ label, to, icon: Icon }) => (
              <Link
                key={to}
                to={to}
                className="flex flex-col items-center justify-center gap-2 p-4 border border-slate-200 rounded-xl hover:bg-blue-50 hover:border-blue-200 transition-colors"
              >
                <Icon className="h-6 w-6 text-blue-500" />
                <span className="text-xs font-medium text-slate-700">{label}</span>
              </Link>
            ))}
          </div>
        </Card>
      </div>
    </div>
  )
}

function StatCard({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <Card className="flex items-start gap-3">
      <div className="p-2 rounded-lg bg-slate-50">{icon}</div>
      <div>
        <div className="text-xs text-slate-500">{label}</div>
        <div className="text-sm font-semibold text-slate-900 mt-0.5">{value}</div>
      </div>
    </Card>
  )
}
