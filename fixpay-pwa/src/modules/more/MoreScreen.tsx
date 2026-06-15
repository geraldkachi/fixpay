import { useNavigate } from 'react-router-dom'
import { ChevronRightIcon, UserCircleIcon, BanknotesIcon, ExclamationTriangleIcon, ArrowRightStartOnRectangleIcon, ShieldCheckIcon, ChartBarIcon } from '@heroicons/react/24/outline'
import { useAuthStore } from '@/store/auth.store'
import { useTenantStore } from '@/store/tenant.store'
import { serverLogout } from '@/lib/api'
import { PageHeader } from '@/components/layout/PageHeader'
import { Badge, statusBadge } from '@/components/ui/Badge'
import { cn } from '@/lib/utils'

function MenuItem({ icon: Icon, label, sub, onClick, variant = 'default', last }: {
  icon: React.FC<React.SVGProps<SVGSVGElement>>; label: string; sub?: string; onClick: () => void; variant?: 'default' | 'danger'; last?: boolean
}) {
  return (
    <button onClick={onClick}
      className={cn('w-full flex items-center gap-3 px-4 py-3.5 bg-white pressable active:bg-gray-50 text-left', !last && 'border-b border-black/5')}>
      <div className={cn('w-9 h-9 rounded-full flex items-center justify-center shrink-0', variant === 'danger' ? 'bg-red-100' : 'bg-gray-100')}>
        <Icon className={cn('w-5 h-5', variant === 'danger' ? 'text-ios-red' : 'text-gray-600')} />
      </div>
      <div className="flex-1 min-w-0">
        <p className={cn('text-[15px] font-medium', variant === 'danger' ? 'text-ios-red' : 'text-gray-900')}>{label}</p>
        {sub && <p className="text-[12px] text-gray-400 mt-0.5">{sub}</p>}
      </div>
      <ChevronRightIcon className="w-4 h-4 text-gray-300 shrink-0" />
    </button>
  )
}

export function MoreScreen() {
  const navigate = useNavigate()
  const { user } = useAuthStore()
  const { config } = useTenantStore()
  const { label, variant } = statusBadge(user?.kycStatus ?? 'pending')

  return (
    <div className="flex flex-col bg-[#F2F2F7] min-h-[100dvh] pb-nav">
      <PageHeader title="More" />

      {/* Profile summary */}
      <div className="mx-4 mt-4 bg-white rounded-[20px] p-4 flex items-center gap-4 animate-slide-up">
        <div className="w-16 h-16 rounded-full flex items-center justify-center text-white text-[24px] font-black shrink-0"
          style={{ background: 'var(--brand-primary)' }}>
          {(user?.firstName ?? 'U')[0]}
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-[17px] font-bold text-gray-900 truncate">{user?.firstName} {user?.lastName}</p>
          <p className="text-[13px] text-gray-500 truncate">{user?.phone ?? user?.email}</p>
          <div className="mt-1">
            <Badge variant={variant} dot>{label}</Badge>
            {user?.tier && <Badge variant="info" className="ml-1">Tier {user.tier}</Badge>}
          </div>
        </div>
      </div>

      {/* Menu items */}
      <div className="mx-4 mt-4 rounded-[20px] overflow-hidden animate-slide-up stagger">
        <MenuItem icon={UserCircleIcon}      label="Profile"          sub="Manage your account"            onClick={() => navigate('/more/profile')} />
        <MenuItem icon={ShieldCheckIcon}     label="KYC & Security"  sub="Identity & PIN settings"         onClick={() => navigate('/more/security')} />
        <MenuItem icon={BanknotesIcon}       label="Direct Debit"    sub="Manage NIBSS mandates"            onClick={() => navigate('/more/mandates')} />
        <MenuItem icon={ChartBarIcon}        label="Analytics"       sub="Track your spending & income"     onClick={() => navigate('/more/analytics')} />
        <MenuItem icon={ExclamationTriangleIcon} label="Disputes"   sub="Raise & track disputes"           onClick={() => navigate('/more/disputes')} last />
      </div>

      <div className="mx-4 mt-4 rounded-[20px] overflow-hidden animate-slide-up">
        <MenuItem icon={ArrowRightStartOnRectangleIcon} label="Sign Out" variant="danger" onClick={() => { serverLogout().then(() => navigate('/welcome', { replace: true })) }} last />
      </div>

      <p className="text-center text-[11px] text-gray-300 mt-6">{config.appName} v1.0.0-poc</p>
    </div>
  )
}
