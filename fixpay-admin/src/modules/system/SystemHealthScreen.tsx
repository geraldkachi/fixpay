import { useQuery } from '@tanstack/react-query'
import { PageHeader } from '@/components/ui'
import { api } from '@/lib/api'
import { CheckCircleIcon, ExclamationTriangleIcon, XCircleIcon } from '@heroicons/react/24/solid'

interface ThirdPartyRail {
  name: string
  status: 'OPERATIONAL' | 'DEGRADED' | 'DOWN'
  latency_ms: number
}

interface SystemHealthResponse {
  status: 'OPERATIONAL' | 'DEGRADED' | 'SYSTEM_ISSUE'
  components: {
    database: 'OK' | 'DOWN'
    redis: 'OK' | 'DOWN'
    memory_usage_mb: number
  }
  third_party_rails: ThirdPartyRail[]
}

export function SystemHealthScreen() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['systemHealth'],
    queryFn: async () => {
      const res = await api.get<SystemHealthResponse>('/admin/system/health')
      return res.data
    },
    refetchInterval: 30000, // Refresh every 30 seconds
  })

  const getStatusIcon = (status: string, sizeClass = 'w-6 h-6') => {
    switch (status) {
      case 'OPERATIONAL':
      case 'OK':
        return <CheckCircleIcon className={`${sizeClass} text-green-500`} />
      case 'DEGRADED':
        return <ExclamationTriangleIcon className={`${sizeClass} text-yellow-500`} />
      default:
        return <XCircleIcon className={`${sizeClass} text-red-500`} />
    }
  }

  const getStatusTextColor = (status: string) => {
    switch (status) {
      case 'OPERATIONAL':
      case 'OK':
        return 'text-green-700'
      case 'DEGRADED':
        return 'text-yellow-700'
      default:
        return 'text-red-700'
    }
  }

  return (
    <div className="p-6 animate-fade-in">
      <PageHeader title="System Health" subtitle="Service availability, infrastructure metrics and incident feed" />

      {isLoading ? (
        <div className="mt-8 text-center text-slate-500 animate-pulse">Loading system metrics…</div>
      ) : error ? (
        <div className="mt-8 text-center text-red-600 bg-red-50 p-6 rounded-xl border border-red-100">
          Failed to load system health data. Please check connection.
        </div>
      ) : data ? (
        <div className="mt-6 space-y-6">
          {/* Overall Status */}
          <div className="glass-card p-6 flex items-center gap-4">
            {getStatusIcon(data.status, 'w-10 h-10')}
            <div>
              <h2 className="text-xl font-bold text-slate-900">Platform Status</h2>
              <p className={`text-sm font-medium ${getStatusTextColor(data.status)}`}>
                {data.status === 'OPERATIONAL' ? 'All Systems Operational' : 'Some Systems Experiencing Issues'}
              </p>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Core Components */}
            <div className="glass-panel p-6">
              <h3 className="text-lg font-semibold text-slate-800 mb-4">Core Infrastructure</h3>
              <div className="space-y-4">
                <div className="flex items-center justify-between p-4 bg-slate-50/50 rounded-lg">
                  <div className="flex items-center gap-3">
                    {getStatusIcon(data.components.database)}
                    <span className="font-medium text-slate-700">Primary Database</span>
                  </div>
                  <span className={`text-sm font-semibold ${getStatusTextColor(data.components.database)}`}>
                    {data.components.database}
                  </span>
                </div>
                <div className="flex items-center justify-between p-4 bg-slate-50/50 rounded-lg">
                  <div className="flex items-center gap-3">
                    {getStatusIcon(data.components.redis)}
                    <span className="font-medium text-slate-700">Redis Cache</span>
                  </div>
                  <span className={`text-sm font-semibold ${getStatusTextColor(data.components.redis)}`}>
                    {data.components.redis}
                  </span>
                </div>
                <div className="flex items-center justify-between p-4 bg-slate-50/50 rounded-lg">
                  <div className="flex flex-col">
                    <span className="font-medium text-slate-700">App Server Memory</span>
                    <span className="text-xs text-slate-500">Current allocation</span>
                  </div>
                  <span className="text-lg font-mono font-bold text-slate-800">
                    {data.components.memory_usage_mb} MB
                  </span>
                </div>
              </div>
            </div>

            {/* Third Party Rails */}
            <div className="glass-panel p-6">
              <h3 className="text-lg font-semibold text-slate-800 mb-4">Payment & Service Providers</h3>
              <div className="space-y-4">
                {data.third_party_rails.map((rail) => (
                  <div key={rail.name} className="flex items-center justify-between p-4 bg-slate-50/50 rounded-lg">
                    <div className="flex items-center gap-3">
                      {getStatusIcon(rail.status)}
                      <div>
                        <span className="block font-medium text-slate-700">{rail.name}</span>
                        <span className="block text-xs text-slate-500 font-mono">{rail.latency_ms} ms latency</span>
                      </div>
                    </div>
                    <span className={`text-sm font-semibold ${getStatusTextColor(rail.status)}`}>
                      {rail.status}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  )
}
