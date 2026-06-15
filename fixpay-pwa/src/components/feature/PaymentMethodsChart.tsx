import { useMemo } from 'react'
import { ResponsiveContainer, PieChart, Pie, Cell, Tooltip } from 'recharts'

interface PaymentMethodsChartProps {
  data: Record<string, number>
}

// System colors
const COLORS = ['#007AFF', '#34C759', '#FF9500', '#5856D6']

export function PaymentMethodsChart({ data }: PaymentMethodsChartProps) {
  
  const chartData = useMemo(() => {
    return Object.entries(data).map(([key, value]) => ({
      name: key,
      value: value / 100
    }))
  }, [data])

  if (chartData.length === 0) {
    return null
  }

  return (
    <div className="bg-white rounded-[20px] p-4 mt-4" style={{ boxShadow: '0 4px 20px rgba(0,0,0,0.03)' }}>
      <h3 className="text-[14px] font-semibold text-gray-900 mb-2">Payment Methods</h3>
      <div className="h-[150px] w-full flex items-center justify-between">
        <ResponsiveContainer width="50%" height="100%">
          <PieChart>
            <Pie
              data={chartData}
              innerRadius={30}
              outerRadius={50}
              paddingAngle={2}
              dataKey="value"
            >
              {chartData.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
              ))}
            </Pie>
            <Tooltip 
              contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}
              formatter={(value: number) => [`₦${value.toLocaleString()}`, 'Amount']}
            />
          </PieChart>
        </ResponsiveContainer>
        <div className="w-[50%] flex flex-col justify-center pl-2">
          {chartData.map((entry, index) => (
            <div key={entry.name} className="flex items-center gap-2 mb-1.5">
              <div className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: COLORS[index % COLORS.length] }} />
              <span className="text-[11px] text-gray-500">{entry.name}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
