import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { formatCurrency, formatDateShort } from '@/lib/utils'
import type { Mandate } from '@/types'
import { PageHeader } from '@/components/layout/PageHeader'
import { Badge, statusBadge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Spinner } from '@/components/ui/Spinner'

export function MandatesScreen() {
  const qc = useQueryClient()
  const { data, isLoading } = useQuery({
    queryKey: ['mandates'],
    queryFn: () => api.get<{ content: Mandate[] }>('/direct-debit/mandates').then(r => r.data),
  })
  const mandates = data?.content ?? []

  const { mutate: cancel, isPending: cancelling } = useMutation({
    mutationFn: (id: string) => api.delete(`/direct-debit/mandates/${id}`),
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
                  <div key={m.id} className="bg-white rounded-[20px] p-4">
                    <div className="flex items-start justify-between">
                      <div className="flex-1 min-w-0">
                        <p className="font-bold text-gray-900">{m.bankName}</p>
                        <p className="text-[13px] text-gray-500 mt-0.5">{m.accountNumber}</p>
                        <p className="text-[13px] text-gray-500 mt-0.5">{m.narrative}</p>
                        <p className="text-[15px] font-semibold mt-2" style={{ color: 'var(--brand-primary)' }}>
                          {formatCurrency(m.amountKobo)} / {m.frequency}
                        </p>
                        <p className="text-[11px] text-gray-400 mt-1">{formatDateShort(m.startDate)} – {formatDateShort(m.endDate)}</p>
                      </div>
                      <Badge variant={variant}>{label}</Badge>
                    </div>
                    {m.authorizationUrl && m.status === 'pending_auth' && (
                      <a href={m.authorizationUrl} target="_blank" rel="noreferrer"
                        className="mt-3 block w-full text-center py-2.5 rounded-[12px] text-[14px] font-semibold text-white"
                        style={{ background: 'var(--brand-primary)' }}>
                        Authorize Mandate
                      </a>
                    )}
                    {(m.status === 'active' || m.status === 'pending_auth') && (
                      <Button variant="outline" size="sm" fullWidth className="mt-2"
                        loading={cancelling} onClick={() => cancel(m.id)}>
                        Cancel Mandate
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
