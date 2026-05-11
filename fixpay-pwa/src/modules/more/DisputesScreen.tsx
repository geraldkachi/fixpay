import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { formatDateShort, formatCurrency } from '@/lib/utils'
import type { Dispute } from '@/types'
import { PageHeader } from '@/components/layout/PageHeader'
import { Badge, statusBadge } from '@/components/ui/Badge'
import { Spinner } from '@/components/ui/Spinner'

export function DisputesScreen() {
  const navigate = useNavigate()
  const { data, isLoading } = useQuery({
    queryKey: ['disputes'],
    queryFn: () => api.get<{ content: Dispute[] }>('/disputes').then(r => r.data),
  })
  const disputes = data?.content ?? []

  return (
    <div className="flex flex-col h-[100dvh] bg-[#F2F2F7]">
      <PageHeader title="Disputes" onBack="default" />
      <div className="flex-1 overflow-y-auto no-scrollbar px-4 pt-4 pb-8 animate-slide-up">
        {isLoading ? <div className="flex justify-center py-8"><Spinner /></div> : (
          disputes.length === 0 ? (
            <div className="bg-white rounded-[16px] p-8 text-center text-gray-400">No disputes raised</div>
          ) : (
            <div className="flex flex-col gap-3">
              {disputes.map(d => {
                const { label, variant } = statusBadge(d.status)
                return (
                  <div key={d.id} className="bg-white rounded-[20px] p-4 pressable cursor-pointer" onClick={() => navigate(`/more/disputes/${d.id}`)}>
                    <div className="flex items-start justify-between">
                      <p className="font-bold text-gray-900 flex-1 truncate pr-2">{d.transaction?.description ?? 'Transaction'}</p>
                      <Badge variant={variant}>{label}</Badge>
                    </div>
                    <p className="text-[13px] text-gray-500 mt-1">{d.description.slice(0, 80)}…</p>
                    <div className="flex items-center justify-between mt-2">
                      <p className="text-[12px] text-gray-400">{formatDateShort(d.createdAt)}</p>
                      {d.transaction && <p className="text-[13px] font-semibold text-gray-700">{formatCurrency(d.transaction.amountKobo)}</p>}
                    </div>
                  </div>
                )
              })}
            </div>
          )
        )}
      </div>
    </div>
  )
}
