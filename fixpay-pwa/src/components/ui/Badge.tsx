import { cn } from '@/lib/utils'

type Variant = 'default' | 'success' | 'warning' | 'error' | 'info' | 'purple'

const styles: Record<Variant, string> = {
  default: 'bg-gray-100 text-gray-600',
  success: 'bg-green-100 text-green-700',
  warning: 'bg-orange-100 text-orange-700',
  error:   'bg-red-100   text-red-700',
  info:    'bg-blue-100  text-blue-700',
  purple:  'bg-purple-100 text-purple-700',
}

interface BadgeProps {
  children: React.ReactNode
  variant?: Variant
  className?: string
  dot?: boolean
}

export function Badge({ children, variant = 'default', className, dot }: BadgeProps) {
  return (
    <span className={cn('inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-[12px] font-semibold', styles[variant], className)}>
      {dot && <span className="w-1.5 h-1.5 rounded-full bg-current" />}
      {children}
    </span>
  )
}

export function statusBadge(status: string): { label: string; variant: Variant } {
  const map: Record<string, { label: string; variant: Variant }> = {
    completed:       { label: 'Completed',       variant: 'success' },
    delivered:       { label: 'Delivered',       variant: 'success' },
    processing:      { label: 'Processing',      variant: 'info'    },
    initiated:       { label: 'Initiated',       variant: 'info'    },
    pending:         { label: 'Pending',         variant: 'warning' },
    pending_auth:    { label: 'Pending Auth',    variant: 'warning' },
    failed:          { label: 'Failed',          variant: 'error'   },
    reversed:        { label: 'Reversed',        variant: 'error'   },
    active:          { label: 'Active',          variant: 'success' },
    cancelled:       { label: 'Cancelled',       variant: 'error'   },
    expired:         { label: 'Expired',         variant: 'default' },
    open:            { label: 'Open',            variant: 'info'    },
    awaiting_review: { label: 'Awaiting Review', variant: 'warning' },
    under_review:    { label: 'Under Review',    variant: 'warning' },
    resolved:        { label: 'Resolved',        variant: 'success' },
    escalated:       { label: 'Escalated',       variant: 'purple'  },
    closed:          { label: 'Closed',          variant: 'default' },
    verified:        { label: 'Verified',        variant: 'success' },
    partial:         { label: 'Partial',         variant: 'warning' },
    pending_kyc:     { label: 'KYC Pending',     variant: 'warning' },
    rejected:        { label: 'Rejected',        variant: 'error'   },
  }
  return map[status] ?? { label: status, variant: 'default' }
}
