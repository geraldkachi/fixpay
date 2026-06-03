import { useQuery } from '@tanstack/react-query'
import { PageHeader } from '@/components/ui'
import { api } from '@/lib/api'

interface SettlementCycle {
  cycleDate: string
  tenantId: string
  totalTransactions: number
  totalAmountKobo: number
  totalProcessorFeesKobo: number
  platformRevenueKobo: number
}

function fmt(kobo: number) {
  return new Intl.NumberFormat('en-NG', {
    style: 'currency',
    currency: 'NGN',
    notation: 'compact',
    maximumFractionDigits: 1,
  }).format(kobo / 100)
}

export function SettlementDashboard() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['settlement-cycles'],
    queryFn: async () => {
      const res = await api.get<{ data: { cycles: SettlementCycle[] } }>('/admin/settlement/cycles')
      return res.data.data.cycles
    },
  })

  return (
    <div className="p-6">
      <PageHeader title="Settlement" subtitle="Daily settlement cycles, volume and platform revenue" />

      {isLoading && (
        <p className="mt-6 text-sm text-slate-500">Loading settlement cycles…</p>
      )}

      {isError && (
        <p className="mt-6 text-sm text-red-500">Failed to load settlement data.</p>
      )}

      {data && (
        <div className="mt-6 overflow-x-auto rounded-lg shadow ring-1 ring-black ring-opacity-5">
          <table className="min-w-full divide-y divide-slate-200 bg-white text-sm">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">Date</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">Tenant</th>
                <th className="px-4 py-3 text-right text-xs font-semibold text-slate-500 uppercase tracking-wider">Transactions</th>
                <th className="px-4 py-3 text-right text-xs font-semibold text-slate-500 uppercase tracking-wider">Volume</th>
                <th className="px-4 py-3 text-right text-xs font-semibold text-slate-500 uppercase tracking-wider">Processor Fees</th>
                <th className="px-4 py-3 text-right text-xs font-semibold text-slate-500 uppercase tracking-wider">Platform Revenue</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {data.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-4 py-8 text-center text-slate-400">No settlement cycles yet.</td>
                </tr>
              ) : (
                data.map((cycle, i) => (
                  <tr key={`${cycle.cycleDate}-${cycle.tenantId}-${i}`} className="hover:bg-slate-50">
                    <td className="px-4 py-3 whitespace-nowrap text-slate-900 font-medium">{cycle.cycleDate}</td>
                    <td className="px-4 py-3 whitespace-nowrap text-slate-500 font-mono text-xs">{cycle.tenantId.slice(0, 8)}…</td>
                    <td className="px-4 py-3 text-right text-slate-900">{cycle.totalTransactions.toLocaleString()}</td>
                    <td className="px-4 py-3 text-right text-slate-900">{fmt(cycle.totalAmountKobo)}</td>
                    <td className="px-4 py-3 text-right text-orange-600">{fmt(cycle.totalProcessorFeesKobo)}</td>
                    <td className="px-4 py-3 text-right text-green-600 font-medium">{fmt(cycle.platformRevenueKobo)}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
