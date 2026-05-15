import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { adminApi, type PluginInfo } from './adminApi'

const STATE_COLOR: Record<string, string> = {
  STARTED: 'text-green-600',
  STOPPED: 'text-gray-500',
  FAILED:  'text-red-600',
}

export function PluginsPanel() {
  const qc = useQueryClient()

  const { data: plugins = [], isLoading } = useQuery({
    queryKey: ['admin', 'plugins'],
    queryFn: adminApi.listPlugins,
  })

  const reload = useMutation({
    mutationFn: adminApi.reloadPlugins,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'plugins'] }),
  })

  const unload = useMutation({
    mutationFn: (pluginId: string) => adminApi.unloadPlugin(pluginId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'plugins'] }),
  })

  return (
    <div className="p-4 space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-semibold">PF4J Plugins</h2>
          <p className="text-xs text-gray-500">
            Hot-loadable processor plugins from the <code>./plugins</code> directory.
          </p>
        </div>
        <button
          onClick={() => reload.mutate()}
          disabled={reload.isPending}
          className="text-sm px-3 py-1 rounded bg-blue-600 text-white disabled:opacity-50"
        >
          {reload.isPending ? 'Reloading…' : 'Reload All'}
        </button>
      </div>

      {isLoading && <p className="text-sm text-gray-500">Loading…</p>}

      <div className="divide-y rounded border">
        {(plugins as PluginInfo[]).map(p => (
          <div key={p.pluginId} className="flex items-center justify-between px-4 py-3">
            <div>
              <p className="text-sm font-medium">{p.pluginId}</p>
              <p className="text-xs text-gray-500">v{p.version}</p>
            </div>
            <div className="flex items-center gap-3">
              <span className={`text-xs font-semibold ${STATE_COLOR[p.state] ?? 'text-gray-600'}`}>
                {p.state}
              </span>
              <button
                onClick={() => {
                  if (window.confirm(`Unload plugin "${p.pluginId}"?`)) {
                    unload.mutate(p.pluginId)
                  }
                }}
                disabled={unload.isPending}
                className="text-xs px-2 py-0.5 rounded border text-red-600 hover:bg-red-50 disabled:opacity-50"
              >
                Unload
              </button>
            </div>
          </div>
        ))}
        {!isLoading && plugins.length === 0 && (
          <p className="px-4 py-4 text-sm text-gray-500 text-center">
            No plugins loaded. Drop JAR files into <code>./plugins</code> and click Reload.
          </p>
        )}
      </div>
    </div>
  )
}
