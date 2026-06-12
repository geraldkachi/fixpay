import { useMemo } from 'react'
import { AreaChart, Area, XAxis, YAxis, Tooltip } from 'recharts'
import { formatCurrency } from '@/lib/utils'

interface AnalyticsSummaryChartProps {
  data: Array<{ date: string; income: number; expense: number }>
  incomeTotal: number
  expenseTotal: number
  period: string
}

export function AnalyticsSummaryChart({ data, incomeTotal, expenseTotal }: AnalyticsSummaryChartProps) {
  
  const chartData = useMemo(() => {
    return data.map(d => ({
      name: new Date(d.date).toLocaleDateString(undefined, { weekday: 'short' }),
      income: d.income / 100, // format to NGN
      expense: d.expense / 100
    }))
  }, [data])

  return (
    <div className="bg-white rounded-[20px] p-4" style={{ boxShadow: '0 4px 20px rgba(0,0,0,0.03)' }}>
      <div className="flex items-center justify-between mb-4">
        <div>
          <p className="text-[12px] text-gray-500 font-medium">Income</p>
          <p className="text-[15px] font-bold text-[#34C759]">{formatCurrency(incomeTotal)}</p>
        </div>
        <div className="text-right">
          <p className="text-[12px] text-gray-500 font-medium">Expenses</p>
          <p className="text-[15px] font-bold text-[#FF3B30]">{formatCurrency(expenseTotal)}</p>
        </div>
      </div>
      
      <div className="h-[120px] w-full overflow-hidden">
        {chartData.length > 0 ? (
          <AreaChart width={340} height={120} data={chartData} margin={{ top: 5, right: 0, left: 0, bottom: 0 }}>
            <defs>
              <linearGradient id="colorIncome" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#34C759" stopOpacity={0.3}/>
                <stop offset="95%" stopColor="#34C759" stopOpacity={0}/>
              </linearGradient>
              <linearGradient id="colorExpense" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#FF3B30" stopOpacity={0.3}/>
                <stop offset="95%" stopColor="#FF3B30" stopOpacity={0}/>
              </linearGradient>
            </defs>
            <XAxis dataKey="name" hide />
            <YAxis hide />
            <Tooltip 
              contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}
              itemStyle={{ fontSize: '12px', fontWeight: 'bold' }}
              labelStyle={{ fontSize: '11px', color: '#8E8E93', marginBottom: '4px' }}
              formatter={(value: number) => [`₦${value.toLocaleString()}`, '']}
            />
            <Area type="monotone" dataKey="income" stroke="#34C759" strokeWidth={2} fillOpacity={1} fill="url(#colorIncome)" />
            <Area type="monotone" dataKey="expense" stroke="#FF3B30" strokeWidth={2} fillOpacity={1} fill="url(#colorExpense)" />
          </AreaChart>
        ) : (
          <div className="h-full flex items-center justify-center text-gray-400 text-xs">No data for this period</div>
        )}
      </div>
    </div>
  )
}
