import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { railApi, type PluginInfo } from './railApi'

const STATE_COLOR: Record<string, string> = {
  STARTED: 'text-emerald-600',
  STOPPED: 'text-slate-500',
  FAILED:  'text-red-600',
}

export function PluginsPanel() {
  const qc = useQueryClient()
  const { data: plugins = [], isLoading } = useQuery({
    queryKey: ['admin', 'plugins'],
    queryFn: railApi.listPlugins,
  })

  const reload = useMutation({
    mutationFn: railApi.reloadPlugins,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'plugins'] }),
  })

  const unload = useMutation({
    mutationFn: (pluginId: string) => railApi.unloadPlugin(pluginId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'plugins'] }),
  })

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-base font-semibold text-slate-900">PF4J Plugins</h3>
          <p className="text-xs text-slate-500 mt-0.5">
            Hot-loadable processor plugins from the <code className="font-mono">./plugins</code> directory.
          </p>
        </div>
        <button onClick={() => reload.mutate()} disabled={reload.isPending}
          className="text-sm px-3 py-1.5 rounded-lg bg-indigo-600 text-white disabled:opacity-50 hover:bg-indigo-700">
          {reload.isPending ? 'Reloading…' : 'Reload All'}
        </button>
      </div>

      {isLoading && <p className="text-sm text-slate-500">Loading…</p>}

      <div className="bg-white rounded-xl border border-slate-200 divide-y">
        {(plugins as PluginInfo[]).map(p => (
          <div key={p.pluginId} className="flex items-center justify-between px-5 py-3.5">
            <div>
              <p className="text-sm font-medium text-slate-900">{p.pluginId}</p>
              <p className="text-xs text-slate-500">v{p.version}</p>
            </div>
            <div className="flex items-center gap-3">
              <span className={`text-xs font-semibold ${STATE_COLOR[p.state] ?? 'text-slate-600'}`}>{p.state}</span>
              <button
                onClick={() => { if (window.confirm(`Unload plugin "${p.pluginId}"?`)) unload.mutate(p.pluginId) }}
                disabled={unload.isPending}
                className="text-xs px-2 py-1 rounded-lg border text-red-600 hover:bg-red-50 disabled:opacity-50">
                Unload
              </button>
            </div>
          </div>
        ))}
        {!isLoading && plugins.length === 0 && (
          <p className="px-5 py-4 text-sm text-slate-500 text-center">
            No plugins loaded. Drop JAR files into <code className="font-mono">./plugins</code> and click Reload.
          </p>
        )}
      </div>
    </div>
  )
}
