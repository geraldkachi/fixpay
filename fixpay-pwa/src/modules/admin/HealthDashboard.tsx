import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { adminApi, type ProcessorHealthStatus } from './adminApi'

const STATE_COLOR: Record<string, string> = {
  CLOSED:      'text-green-600',
  HALF_OPEN:   'text-yellow-600',
  OPEN:        'text-red-600',
  FORCED_OPEN: 'text-red-800',
  DISABLED:    'text-gray-400',
  METRICS_ONLY:'text-gray-400',
}

export function HealthDashboard() {
  const qc = useQueryClient()
  const { data: items = [], isLoading } = useQuery({
    queryKey: ['admin', 'health'],
    queryFn: adminApi.getHealth,
    refetchInterval: 15_000,
  })

  const reload = useMutation({
    mutationFn: adminApi.reloadPlugins,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin'] }),
  })

  return (
    <div className="p-4 space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">Processor Health</h2>
        <button
          onClick={() => reload.mutate()}
          disabled={reload.isPending}
          className="text-sm px-3 py-1 rounded bg-blue-600 text-white disabled:opacity-50"
        >
          {reload.isPending ? 'Reloading…' : 'Reload Plugins'}
        </button>
      </div>

      {isLoading && <p className="text-sm text-gray-500">Loading…</p>}

      <div className="divide-y rounded border">
        {items.map((item: ProcessorHealthStatus) => (
          <div key={item.processorId} className="flex items-center justify-between px-4 py-3">
            <div>
              <p className="font-medium text-sm">{item.processorId}</p>
              <p className="text-xs text-gray-500">
                {item.isPlugin ? 'Plugin' : 'Built-in'} &bull; {item.totalCalls} calls &bull; {item.failureRate.toFixed(1)}% failures
              </p>
            </div>
            <span className={`text-sm font-semibold ${STATE_COLOR[item.cbState] ?? 'text-gray-600'}`}>
              {item.cbState}
            </span>
          </div>
        ))}
        {!isLoading && items.length === 0 && (
          <p className="px-4 py-3 text-sm text-gray-500">No processors registered.</p>
        )}
      </div>
    </div>
  )
}
