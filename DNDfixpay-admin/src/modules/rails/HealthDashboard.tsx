import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { railApi, type ProcessorHealthStatus } from './railApi'

const STATE_COLOR: Record<string, string> = {
  CLOSED:       'text-emerald-600',
  HALF_OPEN:    'text-amber-600',
  OPEN:         'text-red-600',
  FORCED_OPEN:  'text-red-800 font-bold',
  DISABLED:     'text-slate-400',
  METRICS_ONLY: 'text-slate-400',
}

export function HealthDashboard() {
  const qc = useQueryClient()
  const { data: items = [], isLoading } = useQuery({
    queryKey: ['admin', 'health'],
    queryFn: railApi.getHealth,
    refetchInterval: 15_000,
  })

  const reload = useMutation({
    mutationFn: railApi.reloadPlugins,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin'] }),
  })

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-semibold text-slate-900">Processor Health</h2>
          <p className="text-xs text-slate-500 mt-0.5">Auto-refreshes every 15 seconds</p>
        </div>
        <button onClick={() => reload.mutate()} disabled={reload.isPending}
          className="text-sm px-3 py-1.5 rounded-lg bg-indigo-600 text-white disabled:opacity-50 hover:bg-indigo-700">
          {reload.isPending ? 'Reloading…' : 'Reload Plugins'}
        </button>
      </div>

      {isLoading && <p className="text-sm text-slate-500">Loading…</p>}

      <div className="bg-white rounded-xl border border-slate-200 divide-y">
        {(items as ProcessorHealthStatus[]).map(item => (
          <div key={item.processorId} className="flex items-center justify-between px-5 py-3.5">
            <div>
              <p className="font-medium text-sm text-slate-900">{item.processorId}</p>
              <p className="text-xs text-slate-500">
                {item.isPlugin ? 'Plugin' : 'Built-in'} &bull; {item.totalCalls} calls &bull; {item.failureRate.toFixed(1)}% failures
              </p>
            </div>
            <span className={`text-sm ${STATE_COLOR[item.cbState] ?? 'text-slate-600'}`}>
              {item.cbState}
            </span>
          </div>
        ))}
        {!isLoading && items.length === 0 && (
          <p className="px-5 py-4 text-sm text-slate-500 text-center">No processors registered.</p>
        )}
      </div>
    </div>
  )
}
