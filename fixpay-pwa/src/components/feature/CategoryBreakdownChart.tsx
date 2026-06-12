import { useMemo } from 'react'
import { ResponsiveContainer, BarChart, Bar, XAxis, Tooltip, Cell, YAxis } from 'recharts'
import { formatCurrency } from '@/lib/utils'

interface CategoryBreakdownChartProps {
  data: Record<string, number>
}

// System colors for categories
const COLORS = ['#007AFF', '#5856D6', '#FF9500', '#FF3B30', '#34C759', '#AF52DE', '#FF2D55']

export function CategoryBreakdownChart({ data }: CategoryBreakdownChartProps) {
  
  const chartData = useMemo(() => {
    return Object.entries(data)
      .map(([key, value]) => ({
        name: key.replace('Transfer_', 'Transfer '),
        amount: value / 100
      }))
      .sort((a, b) => b.amount - a.amount)
  }, [data])

  if (chartData.length === 0) {
    return <div className="text-center p-6 text-gray-400 text-sm">No expenses to display</div>
  }

  return (
    <div className="bg-white rounded-[20px] p-4" style={{ boxShadow: '0 4px 20px rgba(0,0,0,0.03)' }}>
      <h3 className="text-[14px] font-semibold text-gray-900 mb-4">Top Categories</h3>
      <div className="h-[200px] w-full">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={chartData} layout="vertical" margin={{ top: 0, right: 0, left: -20, bottom: 0 }}>
            <XAxis type="number" hide />
            <YAxis dataKey="name" type="category" axisLine={false} tickLine={false} tick={{ fontSize: 11, fill: '#8E8E93' }} />
            <Tooltip 
              cursor={{ fill: '#F2F2F7' }}
              contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}
              formatter={(value: number) => [`₦${value.toLocaleString()}`, 'Amount']}
            />
            <Bar dataKey="amount" radius={[0, 4, 4, 0]} barSize={20}>
              {chartData.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}
