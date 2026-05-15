import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { adminApi, type AuditLogEntry } from './adminApi'

interface Props {
  /** When provided, shows audit log for a specific rail config. Otherwise shows global log. */
  railId?: string
}

export function AuditLogView({ railId }: Props) {
  const [page, setPage] = useState(0)
  const pageSize = 20

  const { data, isLoading } = useQuery({
    queryKey: ['admin', 'audit', railId ?? 'all', page],
    queryFn: () =>
      railId
        ? adminApi.getEntityAuditLog(railId, page, pageSize)
        : adminApi.getAuditLog(page, pageSize),
  })

  const entries: AuditLogEntry[] = data?.content ?? []
  const total = data?.totalElements ?? 0
  const totalPages = Math.ceil(total / pageSize)

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold text-gray-700">
          {railId ? 'Rail Audit Log' : 'All Changes'} ({total})
        </h3>
      </div>

      {isLoading && <p className="text-xs text-gray-500">Loading…</p>}

      <div className="divide-y rounded border text-xs">
        {entries.map(entry => (
          <div key={entry.id} className="px-3 py-2 space-y-1">
            <div className="flex items-center justify-between gap-2">
              <span className="font-medium">{entry.action}</span>
              <span className="text-gray-400">{new Date(entry.createdAt).toLocaleString()}</span>
            </div>
            <div className="text-gray-500 flex gap-4">
              <span>{entry.entityType}{entry.entityId ? ` · ${entry.entityId.slice(0, 8)}…` : ''}</span>
              {entry.ipAddress && <span>from {entry.ipAddress}</span>}
            </div>
            {entry.afterStateJson && (
              <details className="mt-1">
                <summary className="cursor-pointer text-blue-600 hover:underline">View change</summary>
                <pre className="mt-1 p-2 bg-gray-50 rounded text-[11px] overflow-x-auto whitespace-pre-wrap">
                  {JSON.stringify(JSON.parse(entry.afterStateJson), null, 2)}
                </pre>
              </details>
            )}
          </div>
        ))}
        {!isLoading && entries.length === 0 && (
          <p className="px-3 py-4 text-gray-500 text-center">No audit records.</p>
        )}
      </div>

      {totalPages > 1 && (
        <div className="flex items-center gap-2 justify-center pt-1">
          <button
            disabled={page === 0}
            onClick={() => setPage(p => p - 1)}
            className="px-3 py-1 rounded border text-xs disabled:opacity-40"
          >
            ← Prev
          </button>
          <span className="text-xs text-gray-600">{page + 1} / {totalPages}</span>
          <button
            disabled={page >= totalPages - 1}
            onClick={() => setPage(p => p + 1)}
            className="px-3 py-1 rounded border text-xs disabled:opacity-40"
          >
            Next →
          </button>
        </div>
      )}
    </div>
  )
}
