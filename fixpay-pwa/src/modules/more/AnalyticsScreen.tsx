import { useEffect, useState } from 'react'
import { PageHeader } from '@/components/layout/PageHeader'
import { useAnalyticsStore } from '@/store/analytics.store'
import { AnalyticsSummaryChart } from '@/components/feature/AnalyticsSummaryChart'
import { CategoryBreakdownChart } from '@/components/feature/CategoryBreakdownChart'
import { PaymentMethodsChart } from '@/components/feature/PaymentMethodsChart'
import { Badge } from '@/components/ui/Badge'
import { ExclamationTriangleIcon } from '@heroicons/react/24/outline'

export function AnalyticsScreen() {
  const { data, loading, fetchAnalytics } = useAnalyticsStore()
  const [period, setPeriod] = useState<'7d' | '30d' | '1y'>('30d')

  useEffect(() => {
    fetchAnalytics(period)
  }, [period, fetchAnalytics])

  return (
    <div className="flex flex-col bg-[#F2F2F7] min-h-[100dvh] pb-nav">
      <PageHeader title="Analytics" />
      
      <div className="px-4 py-3 animate-slide-up">
        <div className="bg-gray-200/50 p-1 rounded-full flex gap-1 mb-4">
          {(['7d', '30d', '1y'] as const).map(p => (
            <button 
              key={p} 
              onClick={() => setPeriod(p)}
              className={`flex-1 text-center py-1.5 rounded-full text-[13px] font-medium transition-colors ${period === p ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500'}`}
            >
              {p === '7d' ? 'Week' : p === '30d' ? 'Month' : 'Year'}
            </button>
          ))}
        </div>

        {loading && !data && (
          <div className="text-center py-10 text-sm text-gray-400">Loading insights...</div>
        )}

        {data && (
          <div className="space-y-4">
            <AnalyticsSummaryChart 
              data={data.trend_data} 
              incomeTotal={data.income_total} 
              expenseTotal={data.expense_total} 
              period={period} 
            />
            
            <CategoryBreakdownChart data={data.categories_breakdown} />
            
            {/* Fallback to categories for payment methods if it's missing from backend for now */}
            <PaymentMethodsChart data={data.payment_methods_breakdown || { 'WALLET': data.expense_total }} />

            <div className="bg-white rounded-[20px] p-4 flex items-center justify-between" style={{ boxShadow: '0 4px 20px rgba(0,0,0,0.03)' }}>
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-orange-50 flex items-center justify-center">
                  <ExclamationTriangleIcon className="w-5 h-5 text-orange-500" />
                </div>
                <div>
                  <p className="text-[14px] font-medium text-gray-900">Open Disputes</p>
                  <p className="text-[12px] text-gray-500">Requires attention</p>
                </div>
              </div>
              <Badge variant={data.disputes_summary?.OPEN > 0 ? 'warning' : 'success'}>
                {data.disputes_summary?.OPEN || 0} Open
              </Badge>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
