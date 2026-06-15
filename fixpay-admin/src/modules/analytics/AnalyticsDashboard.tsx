import { useQuery } from '@tanstack/react-query'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts'
import { PageHeader } from '@/components/ui'
import { api } from '@/lib/api'

interface AnalyticsData {
  kpis: {
    total_processing_volume: number
    active_tenants: number
    success_rate: number
    total_revenue: number
  }
  chart_data: Array<{ date: string; volume: number }>
}

export function AnalyticsDashboard() {
  const { data, isLoading } = useQuery({
    queryKey: ['analytics'],
    queryFn: async () => {
      const res = await api.get<AnalyticsData>('/admin/analytics')
      return res.data
    },
  })

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('en-NG', {
      style: 'currency',
      currency: 'NGN',
      notation: 'compact',
      maximumFractionDigits: 1,
    }).format(value)
  }

  if (isLoading) {
    return (
      <div className="p-6">
        <PageHeader title="Analytics" subtitle="Platform KPIs, volume trends and revenue breakdown" />
        <div className="mt-6 text-center text-slate-500 animate-pulse">Loading analytics…</div>
      </div>
    )
  }

  const kpis = data?.kpis
  const chartData = data?.chart_data || []

  return (
    <div className="p-6 animate-fade-in">
      <PageHeader title="Platform Analytics" subtitle="Real-time monitoring and key performance indicators" />

      {/* KPI Cards */}
      {kpis && (
        <div className="mt-6 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <div className="glass-card p-6 hover-lift">
            <p className="text-sm font-medium text-slate-500 tracking-wide">Processing Volume</p>
            <p className="mt-3 text-3xl font-bold text-slate-900 bg-clip-text text-transparent bg-gradient-to-r from-blue-600 to-indigo-600">
              {formatCurrency(kpis.total_processing_volume)}
            </p>
          </div>
          <div className="glass-card p-6 hover-lift">
            <p className="text-sm font-medium text-slate-500 tracking-wide">Total Revenue</p>
            <p className="mt-3 text-3xl font-bold text-slate-900 bg-clip-text text-transparent bg-gradient-to-r from-emerald-500 to-teal-500">
              {formatCurrency(kpis.total_revenue)}
            </p>
          </div>
          <div className="glass-card p-6 hover-lift">
            <p className="text-sm font-medium text-slate-500 tracking-wide">Success Rate</p>
            <p className="mt-3 text-3xl font-bold text-slate-900">{kpis.success_rate.toFixed(1)}%</p>
          </div>
          <div className="glass-card p-6 hover-lift">
            <p className="text-sm font-medium text-slate-500 tracking-wide">Active Tenants</p>
            <p className="mt-3 text-3xl font-bold text-slate-900">{kpis.active_tenants}</p>
          </div>
        </div>
      )}

      {/* Charts */}
      <div className="mt-8">
        <div className="glass-card p-6">
          <h3 className="text-lg font-semibold text-slate-800 mb-6">Transaction Volume (Last 7 Days)</h3>
          <div className="h-[400px]">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                <XAxis dataKey="date" stroke="#94a3b8" tick={{ fill: '#64748b' }} tickMargin={10} />
                <YAxis stroke="#94a3b8" tick={{ fill: '#64748b' }} tickFormatter={(val) => formatCurrency(val)} />
                <Tooltip
                  contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 10px 15px -3px rgb(0 0 0 / 0.1)' }}
                  formatter={(value: number) => formatCurrency(value)}
                />
                <Legend />
                <Line 
                  type="monotone" 
                  dataKey="volume" 
                  stroke="#3b82f6" 
                  strokeWidth={3} 
                  dot={{ r: 4, fill: '#3b82f6', strokeWidth: 2 }} 
                  activeDot={{ r: 6, strokeWidth: 0 }} 
                  name="Volume" 
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>
    </div>
  )
}
