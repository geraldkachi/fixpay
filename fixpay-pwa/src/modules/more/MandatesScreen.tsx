import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDateShort } from '@/lib/utils'
import type { MandateResponse } from '@/types'
import { mandatesService } from '@/lib/services/mandates.service'
import { PageHeader } from '@/components/layout/PageHeader'
import { Badge, statusBadge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Spinner } from '@/components/ui/Spinner'

export function MandatesScreen() {
  const qc = useQueryClient()
  const { data: mandates = [], isLoading } = useQuery<MandateResponse[]>({
    queryKey: ['mandates'],
    queryFn: () => mandatesService.list(),
  })

  const { mutate: sync, isPending: syncing } = useMutation({
    mutationFn: (ref: string) => mandatesService.sync(ref),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['mandates'] }),
  })

  return (
    <div className="flex flex-col h-[100dvh] bg-[#F2F2F7]">
      <PageHeader title="Direct Debit Mandates" onBack="default" />
      <div className="flex-1 overflow-y-auto no-scrollbar px-4 pt-4 pb-8 animate-slide-up">
        {isLoading ? <div className="flex justify-center py-8"><Spinner /></div> : (
          mandates.length === 0 ? (
            <div className="bg-white rounded-[16px] p-8 text-center text-gray-400">No mandates set up</div>
          ) : (
            <div className="flex flex-col gap-3">
              {mandates.map(m => {
                const { label, variant } = statusBadge(m.status)
                return (
                  <div key={m.mandateReference} className="bg-white rounded-[20px] p-4">
                    <div className="flex items-start justify-between">
                      <div className="flex-1 min-w-0">
                        <p className="font-bold text-gray-900">Bank Code: {m.bankCode}</p>
                        <p className="text-[13px] text-gray-500 mt-0.5">{m.accountNumber}</p>
                        {m.providerMessage && (
                          <p className="text-[13px] text-gray-500 mt-0.5">{m.providerMessage}</p>
                        )}
                        <p className="text-[15px] font-semibold mt-2" style={{ color: 'var(--brand-primary)' }}>
                          ₦{m.maxAmount.toLocaleString('en-NG')} max
                        </p>
                        <p className="text-[11px] text-gray-400 mt-1">
                          {formatDateShort(m.startDate)} – {m.endDate ? formatDateShort(m.endDate) : 'ongoing'}
                        </p>
                        <p className="text-[10px] text-gray-300 mt-0.5 font-mono">{m.mandateReference}</p>
                      </div>
                      <Badge variant={variant}>{label}</Badge>
                    </div>
                    {(m.status === 'active' || m.status === 'pending_auth') && (
                      <Button variant="outline" size="sm" fullWidth className="mt-3"
                        loading={syncing} onClick={() => sync(m.mandateReference)}>
                        Sync Status
                      </Button>
                    )}
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
