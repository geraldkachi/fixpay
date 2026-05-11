import { useAuthStore } from '@/store/auth.store'
import { PageHeader } from '@/components/layout/PageHeader'
import { Badge, statusBadge } from '@/components/ui/Badge'

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between px-4 py-3 border-b border-black/5 last:border-0">
      <span className="text-[14px] text-gray-500">{label}</span>
      <span className="text-[14px] font-semibold text-gray-900">{value}</span>
    </div>
  )
}

export function ProfileScreen() {
  const { user } = useAuthStore()
  if (!user) return null
  const { label, variant } = statusBadge(user.kycStatus ?? 'pending_kyc')
  return (
    <div className="flex flex-col h-[100dvh] bg-[#F2F2F7]">
      <PageHeader title="Profile" onBack="default" />
      <div className="flex-1 overflow-y-auto no-scrollbar px-4 pt-4 pb-8 animate-slide-up">

        {/* Avatar */}
        <div className="flex flex-col items-center mb-6">
          <div className="w-24 h-24 rounded-full flex items-center justify-center text-white text-[40px] font-black"
            style={{ background: 'var(--brand-primary)' }}>
            {(user.firstName ?? 'U')[0]}
          </div>
          <h2 className="text-[20px] font-bold text-gray-900 mt-3">{user.firstName} {user.lastName}</h2>
          <Badge variant={variant} dot className="mt-2">{label}</Badge>
        </div>

        {/* Details */}
        <div className="bg-white rounded-[20px] overflow-hidden">
          {user.phone  && <Row label="Phone"       value={user.phone} />}
          {user.email  && <Row label="Email"       value={user.email} />}
          <Row label="Account Tier"  value={`Tier ${user.tier}`} />
          <Row label="KYC Status"    value={label} />
          <Row label="Member Since"  value={new Date(user.createdAt).toLocaleDateString('en-NG', { year: 'numeric', month: 'long' })} />
        </div>
      </div>
    </div>
  )
}
