import { useQuery } from '@tanstack/react-query'
import { LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts'
import { PageHeader } from '@/components/ui'
import { api } from '@/lib/api'

interface AnalyticsData {
  dailyVolume: Array<{ date: string; transactions: number; volume: number }>
  revenueTrend: Array<{ month: string; fixed: number; percentage: number }>
  processorBreakdown: Array<{ name: string; value: number; percentage: number }>
  kpis: {
    totalTransactions: number
    totalVolume: number
    platformRevenue: number
    averageTransactionValue: number
    successRate: number
  }
}

const COLORS = ['#3b82f6', '#8b5cf6', '#ec4899', '#f59e0b', '#10b981']

export function AnalyticsDashboard() {
  const { data, isLoading } = useQuery({
    queryKey: ['analytics'],
    queryFn: async () => {
      try {
        const res = await api.get<{ data: AnalyticsData }>('/admin/analytics')
        return res.data.data
      } catch {
        // Return mock data for demo
        return {
          dailyVolume: [
            { date: 'May 1', transactions: 1200, volume: 45000000 },
            { date: 'May 2', transactions: 1908, volume: 72000000 },
            { date: 'May 3', transactions: 2000, volume: 75000000 },
            { date: 'May 4', transactions: 2780, volume: 104000000 },
            { date: 'May 5', transactions: 1890, volume: 71000000 },
            { date: 'May 6', transactions: 2390, volume: 90000000 },
            { date: 'May 7', transactions: 3490, volume: 131000000 },
          ],
          revenueTrend: [
            { month: 'January', fixed: 240000, percentage: 180000 },
            { month: 'February', fixed: 280000, percentage: 210000 },
            { month: 'March', fixed: 320000, percentage: 240000 },
            { month: 'April', fixed: 380000, percentage: 285000 },
            { month: 'May', fixed: 450000, percentage: 337500 },
          ],
          processorBreakdown: [
            { name: 'Airtime', value: 35, percentage: 35 },
            { name: 'Data', value: 28, percentage: 28 },
            { name: 'Electricity', value: 22, percentage: 22 },
            { name: 'TV', value: 10, percentage: 10 },
            { name: 'Other', value: 5, percentage: 5 },
          ],
          kpis: {
            totalTransactions: 124500,
            totalVolume: 3_980_000_000,
            platformRevenue: 2_850_000,
            averageTransactionValue: 31_961,
            successRate: 97.8,
          },
        }
      }
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
        <div className="mt-6 text-center text-slate-500">Loading analytics…</div>
      </div>
    )
  }

  const kpis = data?.kpis
  const dailyVolume = data?.dailyVolume || []
  const revenueTrend = data?.revenueTrend || []
  const processorBreakdown = data?.processorBreakdown || []

  return (
    <div className="p-6">
      <PageHeader title="Analytics" subtitle="Platform KPIs, volume trends and revenue breakdown" />

      {/* KPI Cards */}
      {kpis && (
        <div className="mt-6 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
          <div className="bg-white p-4 rounded-lg shadow ring-1 ring-black ring-opacity-5">
            <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">Total Transactions</p>
            <p className="mt-2 text-2xl font-bold text-slate-900">{kpis.totalTransactions.toLocaleString()}</p>
          </div>
          <div className="bg-white p-4 rounded-lg shadow ring-1 ring-black ring-opacity-5">
            <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">Total Volume</p>
            <p className="mt-2 text-2xl font-bold text-slate-900">{formatCurrency(kpis.totalVolume)}</p>
          </div>
          <div className="bg-white p-4 rounded-lg shadow ring-1 ring-black ring-opacity-5">
            <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">Platform Revenue</p>
            <p className="mt-2 text-2xl font-bold text-slate-900">{formatCurrency(kpis.platformRevenue)}</p>
          </div>
          <div className="bg-white p-4 rounded-lg shadow ring-1 ring-black ring-opacity-5">
            <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">Avg Tx Value</p>
            <p className="mt-2 text-2xl font-bold text-slate-900">{formatCurrency(kpis.averageTransactionValue)}</p>
          </div>
          <div className="bg-white p-4 rounded-lg shadow ring-1 ring-black ring-opacity-5">
            <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">Success Rate</p>
            <p className="mt-2 text-2xl font-bold text-green-600">{kpis.successRate.toFixed(1)}%</p>
          </div>
        </div>
      )}

      {/* Charts */}
      <div className="mt-8 grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Daily Volume */}
        <div className="bg-white p-6 rounded-lg shadow ring-1 ring-black ring-opacity-5">
          <h3 className="text-sm font-semibold text-slate-900 mb-4">Daily Transaction Volume (Last 7 days)</h3>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={dailyVolume}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Line type="monotone" dataKey="transactions" stroke="#3b82f6" name="Transactions" />
              <Line type="monotone" dataKey="volume" stroke="#8b5cf6" name="Volume (NGN)" />
            </LineChart>
          </ResponsiveContainer>
        </div>

        {/* Revenue Trend */}
        <div className="bg-white p-6 rounded-lg shadow ring-1 ring-black ring-opacity-5">
          <h3 className="text-sm font-semibold text-slate-900 mb-4">Revenue Trend (Monthly)</h3>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={revenueTrend}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="month" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Bar dataKey="fixed" fill="#3b82f6" name="Fixed Fees" />
              <Bar dataKey="percentage" fill="#8b5cf6" name="Percentage Fees" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Processor Breakdown */}
      <div className="mt-6 bg-white p-6 rounded-lg shadow ring-1 ring-black ring-opacity-5">
        <h3 className="text-sm font-semibold text-slate-900 mb-4">Transaction Breakdown by Category</h3>
        <div className="flex justify-center">
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={processorBreakdown}
                cx="50%"
                cy="50%"
                labelLine={false}
                label={(entry) => `${entry.name} (${entry.percentage}%)`}
                outerRadius={100}
                fill="#8884d8"
                dataKey="value"
              >
                {processorBreakdown.map((_, index) => (
                  <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  )
}
