import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { formatDateFull, formatCurrency } from '@/lib/utils'
import type { Dispute } from '@/types'
import { PageHeader } from '@/components/layout/PageHeader'
import { Badge, statusBadge } from '@/components/ui/Badge'
import { Spinner } from '@/components/ui/Spinner'

export function DisputeDetailScreen() {
  const { id } = useParams<{ id: string }>()
  const { data: dispute, isLoading } = useQuery<Dispute>({
    queryKey: ['disputes', id],
    queryFn: () => api.get<Dispute>(`/disputes/${id}`).then(r => r.data),
    enabled: !!id,
  })

  if (isLoading) return <div className="h-[100dvh] flex items-center justify-center"><Spinner size="lg" /></div>
  if (!dispute) return <div className="h-[100dvh] flex items-center justify-center text-gray-400">Dispute not found</div>

  const { label, variant } = statusBadge(dispute.status)

  return (
    <div className="flex flex-col h-[100dvh] bg-[#F2F2F7]">
      <PageHeader title="Dispute Details" onBack="default" />
      <div className="flex-1 overflow-y-auto no-scrollbar px-4 pt-4 pb-8 animate-slide-up">

        {/* Status banner */}
        <div className="bg-white rounded-[20px] p-4 mb-4 flex items-center justify-between">
          <div>
            <p className="text-[13px] text-gray-400">Status</p>
            <Badge variant={variant} dot className="mt-1">{label}</Badge>
          </div>
          <div className="text-right">
            <p className="text-[13px] text-gray-400">Raised On</p>
            <p className="text-[13px] font-semibold text-gray-700 mt-0.5">{formatDateFull(dispute.createdAt)}</p>
          </div>
        </div>

        {/* Transaction */}
        {dispute.transaction && (
          <div className="bg-white rounded-[20px] p-4 mb-4">
            <p className="text-[12px] text-gray-400 uppercase tracking-wide mb-2">Related Transaction</p>
            <p className="font-bold text-gray-900">{dispute.transaction.description}</p>
            <div className="flex items-center justify-between mt-1">
              <p className="text-[13px] text-gray-500">{formatDateFull(dispute.transaction.createdAt)}</p>
              <p className="text-[15px] font-bold text-gray-900">{formatCurrency(dispute.transaction.amountKobo)}</p>
            </div>
          </div>
        )}

        {/* Description */}
        <div className="bg-white rounded-[20px] p-4 mb-4">
          <p className="text-[12px] text-gray-400 uppercase tracking-wide mb-2">Your Complaint</p>
          <p className="text-[15px] text-gray-800 leading-relaxed">{dispute.description}</p>
          <p className="text-[12px] text-gray-400 mt-2">Category: <strong className="text-gray-600">{dispute.category.replace(/_/g, ' ')}</strong></p>
        </div>

        {/* SLA */}
        <div className="bg-orange-50 rounded-[16px] p-4 mb-4">
          <p className="text-[13px] text-orange-700">
            ⏱ Resolution deadline: <strong>{formatDateFull(dispute.slaDeadline)}</strong>
          </p>
        </div>

        {/* Resolution */}
        {dispute.resolution && (
          <div className="bg-green-50 rounded-[20px] p-4">
            <p className="text-[12px] text-green-700 uppercase tracking-wide mb-2 font-semibold">Resolution</p>
            <p className="text-[14px] text-green-900 leading-relaxed">{dispute.resolution}</p>
          </div>
        )}
      </div>
    </div>
  )
}
