import { useQuery } from '@tanstack/react-query'
import { PageHeader } from '@/components/ui'
import { api } from '@/lib/api'
import { ShieldCheckIcon, ShieldExclamationIcon, PlusIcon } from '@heroicons/react/24/outline'

interface FraudRule {
  id: number
  name: string
  threshold: number
  timeframe_minutes?: number
  action: 'FLAG' | 'BLOCK' | 'REQUIRE_MFA'
  status: 'ACTIVE' | 'INACTIVE'
}

export function FraudRulesScreen() {
  const { data: rules, isLoading, error } = useQuery({
    queryKey: ['fraudRules'],
    queryFn: async () => {
      const res = await api.get<FraudRule[]>('/admin/fraud/rules')
      return res.data
    },
  })

  const formatThreshold = (rule: FraudRule) => {
    if (rule.timeframe_minutes) {
      return `${rule.threshold} operations / ${rule.timeframe_minutes} min`
    }
    return new Intl.NumberFormat('en-NG', {
      style: 'currency',
      currency: 'NGN',
    }).format(rule.threshold / 100)
  }

  const getActionBadge = (action: string) => {
    switch (action) {
      case 'BLOCK':
        return <span className="px-2 py-1 bg-red-100 text-red-800 rounded-md text-xs font-semibold">BLOCK</span>
      case 'FLAG':
        return <span className="px-2 py-1 bg-yellow-100 text-yellow-800 rounded-md text-xs font-semibold">FLAG</span>
      case 'REQUIRE_MFA':
        return <span className="px-2 py-1 bg-blue-100 text-blue-800 rounded-md text-xs font-semibold">MFA CHALLENGE</span>
      default:
        return <span className="px-2 py-1 bg-slate-100 text-slate-800 rounded-md text-xs font-semibold">{action}</span>
    }
  }

  return (
    <div className="p-6 animate-fade-in">
      <div className="flex justify-between items-start mb-6">
        <PageHeader title="Fraud Rules" subtitle="Configure automated detection thresholds and system responses" />
        <button className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg font-medium shadow-sm transition-colors">
          <PlusIcon className="w-5 h-5" />
          Create Rule
        </button>
      </div>

      <div className="glass-panel overflow-hidden">
        {isLoading ? (
          <div className="p-12 text-center text-slate-500 animate-pulse">Loading fraud rules…</div>
        ) : error ? (
          <div className="p-12 text-center text-red-600 bg-red-50">Failed to load rules.</div>
        ) : rules?.length === 0 ? (
          <div className="p-12 text-center text-slate-500">No active fraud rules configured.</div>
        ) : (
          <table className="min-w-full divide-y divide-slate-200/60">
            <thead className="bg-slate-50/50">
              <tr>
                <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Rule Name</th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Threshold / Condition</th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Automated Action</th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Status</th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200/60">
              {rules?.map((rule) => (
                <tr key={rule.id} className="hover:bg-slate-50/50 transition-colors">
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center gap-3">
                      {rule.status === 'ACTIVE' ? (
                        <ShieldCheckIcon className="w-5 h-5 text-green-500" />
                      ) : (
                        <ShieldExclamationIcon className="w-5 h-5 text-slate-400" />
                      )}
                      <span className="font-medium text-slate-900">{rule.name}</span>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-600">
                    {formatThreshold(rule)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    {getActionBadge(rule.action)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`px-2 py-1 text-xs font-semibold rounded-full ${
                      rule.status === 'ACTIVE' ? 'bg-green-100 text-green-800' : 'bg-slate-100 text-slate-800'
                    }`}>
                      {rule.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                    <button className="text-blue-600 hover:text-blue-800 mr-4 transition-colors">Edit</button>
                    <button className="text-slate-500 hover:text-slate-700 transition-colors">
                      {rule.status === 'ACTIVE' ? 'Disable' : 'Enable'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
