import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { PageHeader } from '@/components/ui'
import { api } from '@/lib/api'
import { EyeIcon } from '@heroicons/react/24/outline'

interface LedgerEntry {
  id: string
  walletId: string
  userId: string
  tenantId: string
  correlationId: string
  entryType: 'DEBIT' | 'CREDIT'
  amount: number
  runningBalance: number
  currency: string
  reference: string
  description: string
  createdAt: string
}

interface LedgerListResponse {
  content: LedgerEntry[]
  totalElements: number
  totalPages: number
  number: number
}

export function TransactionLedgerScreen() {
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const [entryTypeFilter, setEntryTypeFilter] = useState<string>('')
  const [searchTerm, setSearchTerm] = useState('')
  const [fromDate, setFromDate] = useState('')
  const [toDate, setToDate] = useState('')

  const { data, isLoading, error } = useQuery({
    queryKey: ['transactions', page, entryTypeFilter, searchTerm, fromDate, toDate],
    queryFn: async () => {
      const params = new URLSearchParams({
        page: page.toString(),
        size: '50',
      })
      if (entryTypeFilter) params.append('entryType', entryTypeFilter)
      if (searchTerm) params.append('reference', searchTerm)
      if (fromDate) params.append('from', new Date(fromDate).toISOString())
      if (toDate) params.append('to', new Date(toDate).toISOString())

      const res = await api.get<{ data: LedgerListResponse }>(`/admin/transactions?${params}`)
      return res.data.data
    },
  })

  const entries = data?.content ?? []

  const entryTypeBadgeColor: Record<string, string> = {
    DEBIT: 'bg-red-100 text-red-800',
    CREDIT: 'bg-green-100 text-green-800',
  }

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-NG', {
      style: 'currency',
      currency: 'NGN',
      minimumFractionDigits: 2,
    }).format(amount)
  }

  return (
    <div className="p-6">
      <PageHeader title="Transaction Ledger" subtitle="Cross-tenant paginated ledger with status, type and date filters" />

      {/* Filters */}
      <div className="mt-6 flex gap-4 items-end flex-wrap">
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-2">Reference</label>
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => {
              setSearchTerm(e.target.value)
              setPage(0)
            }}
            placeholder="Search by reference…"
            className="px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-2">Type</label>
          <select
            value={entryTypeFilter}
            onChange={(e) => {
              setEntryTypeFilter(e.target.value)
              setPage(0)
            }}
            className="px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">All types</option>
            <option value="DEBIT">Debit</option>
            <option value="CREDIT">Credit</option>
          </select>
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-2">From Date</label>
          <input
            type="date"
            value={fromDate}
            onChange={(e) => {
              setFromDate(e.target.value)
              setPage(0)
            }}
            className="px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-2">To Date</label>
          <input
            type="date"
            value={toDate}
            onChange={(e) => {
              setToDate(e.target.value)
              setPage(0)
            }}
            className="px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
      </div>

      {/* Table */}
      <div className="mt-6 overflow-x-auto shadow ring-1 ring-black ring-opacity-5 rounded-lg">
        {isLoading ? (
          <div className="p-6 text-center text-slate-500">Loading transactions…</div>
        ) : error ? (
          <div className="p-6 text-center text-red-600">Failed to load transactions</div>
        ) : entries.length === 0 ? (
          <div className="p-6 text-center text-slate-500">No transactions found</div>
        ) : (
          <table className="min-w-full divide-y divide-slate-200">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-700 uppercase tracking-wider">Reference</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-700 uppercase tracking-wider">Type</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-700 uppercase tracking-wider">Amount</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-700 uppercase tracking-wider">Balance</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-700 uppercase tracking-wider">Correlation</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-700 uppercase tracking-wider">Date</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-700 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-slate-200">
              {entries.map((entry) => (
                <tr key={entry.id} className="hover:bg-slate-50">
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-slate-900">{entry.reference}</td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`px-2 py-1 text-xs font-semibold rounded-full ${entryTypeBadgeColor[entry.entryType] || ''}`}>
                      {entry.entryType}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-slate-900">
                    {formatCurrency(entry.amount)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-600">
                    {formatCurrency(entry.runningBalance)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-xs text-slate-500 font-mono">
                    {entry.correlationId.substring(0, 12)}…
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-500">
                    {new Date(entry.createdAt).toLocaleDateString()}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">
                    <button
                      onClick={() => navigate(`/transactions/${entry.id}`)}
                      className="text-blue-600 hover:text-blue-800"
                      title="View details"
                    >
                      <EyeIcon className="w-4 h-4" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="mt-6 flex justify-between items-center">
          <div className="text-sm text-slate-600">
            Page {page + 1} of {data.totalPages}
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => setPage(Math.max(0, page - 1))}
              disabled={page === 0}
              className="px-4 py-2 text-sm font-medium text-slate-700 border border-slate-300 rounded-lg hover:bg-slate-50 disabled:opacity-50"
            >
              Previous
            </button>
            <button
              onClick={() => setPage(page + 1)}
              disabled={page >= data.totalPages - 1}
              className="px-4 py-2 text-sm font-medium text-slate-700 border border-slate-300 rounded-lg hover:bg-slate-50 disabled:opacity-50"
            >
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
