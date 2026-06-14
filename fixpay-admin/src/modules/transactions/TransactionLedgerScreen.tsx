import { useState, useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import {
  createColumnHelper,
  flexRender,
  getCoreRowModel,
  useReactTable,
} from '@tanstack/react-table'
import { PageHeader } from '@/components/ui'
import { api } from '@/lib/api'
import { EyeIcon } from '@heroicons/react/24/outline'

interface LedgerEntry {
  id: string
  wallet_id: string
  entry_type: 'DEBIT' | 'CREDIT'
  amount_kobo: number
  running_balance_kobo: number
  currency: string
  correlation_id: string
  description: string
  created_at: string
}

interface LaravelPaginatedResponse<T> {
  data: T[]
  current_page: number
  last_page: number
  total: number
}

const columnHelper = createColumnHelper<LedgerEntry>()

export function TransactionLedgerScreen() {
  const navigate = useNavigate()
  const [page, setPage] = useState(1)
  const [days, setDays] = useState(7)
  const [entryTypeFilter, setEntryTypeFilter] = useState<string>('')

  const { data, isLoading, error } = useQuery({
    queryKey: ['transactions', page, days, entryTypeFilter],
    queryFn: async () => {
      const params = new URLSearchParams({
        page: page.toString(),
        days: days.toString(),
      })
      if (entryTypeFilter) params.append('entryType', entryTypeFilter)

      const res = await api.get<LaravelPaginatedResponse<LedgerEntry>>(`/admin/transactions/ledger?${params}`)
      return res.data
    },
  })

  const formatCurrency = (amountKobo: number) => {
    return new Intl.NumberFormat('en-NG', {
      style: 'currency',
      currency: 'NGN',
    }).format(amountKobo / 100)
  }

  const columns = useMemo(() => [
    columnHelper.accessor('correlation_id', {
      header: 'Correlation ID',
      cell: info => <span className="font-mono text-xs text-slate-500">{info.getValue()?.substring(0, 12)}…</span>,
    }),
    columnHelper.accessor('entry_type', {
      header: 'Type',
      cell: info => {
        const type = info.getValue()
        const color = type === 'CREDIT' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
        return <span className={`px-2 py-1 text-xs font-semibold rounded-full ${color}`}>{type}</span>
      },
    }),
    columnHelper.accessor('amount_kobo', {
      header: 'Amount',
      cell: info => <span className="font-medium text-slate-900">{formatCurrency(info.getValue())}</span>,
    }),
    columnHelper.accessor('running_balance_kobo', {
      header: 'Balance',
      cell: info => <span className="text-slate-600">{formatCurrency(info.getValue())}</span>,
    }),
    columnHelper.accessor('created_at', {
      header: 'Date',
      cell: info => <span className="text-sm text-slate-500">{new Date(info.getValue()).toLocaleString()}</span>,
    }),
    columnHelper.display({
      id: 'actions',
      cell: ({ row }) => (
        <button
          onClick={() => navigate(`/transactions/${row.original.id}`)}
          className="text-blue-600 hover:text-blue-800 transition-colors"
          title="View details"
        >
          <EyeIcon className="w-5 h-5" />
        </button>
      ),
    })
  ], [navigate])

  const table = useReactTable({
    data: data?.data ?? [],
    columns,
    getCoreRowModel: getCoreRowModel(),
    manualPagination: true,
  })

  return (
    <div className="p-6 animate-fade-in">
      <PageHeader title="Transaction Ledger" subtitle="Global ledger of all tenant and platform transactions" />

      {/* Filters */}
      <div className="mt-6 glass-card p-4 flex flex-wrap gap-4 items-end">
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-2">Timeframe</label>
          <select
            value={days}
            onChange={(e) => {
              setDays(Number(e.target.value))
              setPage(1)
            }}
            className="px-4 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white/50"
          >
            <option value={1}>Last 24 Hours</option>
            <option value={7}>Last 7 Days</option>
            <option value={30}>Last 30 Days</option>
            <option value={90}>Last 90 Days</option>
          </select>
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-2">Type</label>
          <select
            value={entryTypeFilter}
            onChange={(e) => {
              setEntryTypeFilter(e.target.value)
              setPage(1)
            }}
            className="px-4 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white/50"
          >
            <option value="">All Types</option>
            <option value="DEBIT">Debit</option>
            <option value="CREDIT">Credit</option>
          </select>
        </div>
      </div>

      {/* Data Table */}
      <div className="mt-6 glass-panel overflow-hidden">
        <div className="overflow-x-auto">
          {isLoading ? (
            <div className="p-12 text-center text-slate-500 animate-pulse">Loading ledger entries…</div>
          ) : error ? (
            <div className="p-12 text-center text-red-600 bg-red-50">Failed to load ledger data</div>
          ) : data?.data.length === 0 ? (
            <div className="p-12 text-center text-slate-500">No transactions found in this period.</div>
          ) : (
            <table className="min-w-full divide-y divide-slate-200/60">
              <thead className="bg-slate-50/50">
                {table.getHeaderGroups().map(headerGroup => (
                  <tr key={headerGroup.id}>
                    {headerGroup.headers.map(header => (
                      <th key={header.id} className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">
                        {flexRender(header.column.columnDef.header, header.getContext())}
                      </th>
                    ))}
                  </tr>
                ))}
              </thead>
              <tbody className="divide-y divide-slate-200/60">
                {table.getRowModel().rows.map(row => (
                  <tr key={row.id} className="hover:bg-slate-50/50 transition-colors">
                    {row.getVisibleCells().map(cell => (
                      <td key={cell.id} className="px-6 py-4 whitespace-nowrap">
                        {flexRender(cell.column.columnDef.cell, cell.getContext())}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* Pagination Controls */}
        {data && data.last_page > 1 && (
          <div className="px-6 py-4 border-t border-slate-200/60 bg-slate-50/30 flex justify-between items-center">
            <span className="text-sm text-slate-600 font-medium">
              Page {data.current_page} of {data.last_page}
            </span>
            <div className="flex gap-2">
              <button
                onClick={() => setPage(p => Math.max(1, p - 1))}
                disabled={data.current_page === 1}
                className="px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-200 rounded-lg hover:bg-slate-50 disabled:opacity-50 transition-colors shadow-sm"
              >
                Previous
              </button>
              <button
                onClick={() => setPage(p => p + 1)}
                disabled={data.current_page >= data.last_page}
                className="px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-200 rounded-lg hover:bg-slate-50 disabled:opacity-50 transition-colors shadow-sm"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
