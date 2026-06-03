'use client'

import { useState, useEffect } from 'react'
import { walletService } from '@/lib/services'
import { formatNaira, formatDate } from '@/lib/format'
import type { LedgerEntry, Paginated } from '@/lib/types'

export default function WalletPage() {
  const [txs, setTxs] = useState<LedgerEntry[]>([])
  const [page, setPage] = useState(1)
  const [lastPage, setLastPage] = useState(1)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    walletService
      .transactions({ page: String(page), per_page: '20' })
      .then(res => {
        const p = res as Paginated<LedgerEntry>
        setTxs(p.data)
        setLastPage(p.last_page)
      })
      .finally(() => setLoading(false))
  }, [page])

  return (
    <div className="mx-auto max-w-lg px-4 py-8">
      <h1 className="mb-6 text-xl font-bold text-gray-900">Transaction History</h1>

      {loading ? (
        <div className="flex h-40 items-center justify-center">
          <span className="h-8 w-8 rounded-full border-4 border-green-500 border-t-transparent animate-spin" />
        </div>
      ) : (
        <div className="space-y-2">
          {txs.length === 0 && <p className="text-sm text-gray-400">No transactions.</p>}
          {txs.map(tx => (
            <div key={tx.id} className="flex items-center justify-between rounded-xl bg-white p-3 shadow-sm">
              <div>
                <p className="text-sm font-medium text-gray-800">{tx.description}</p>
                <p className="text-xs text-gray-400">{tx.reference}</p>
                <p className="text-xs text-gray-400">{formatDate(tx.created_at)}</p>
              </div>
              <div className="text-right">
                <span
                  className={
                    'block text-sm font-semibold ' +
                    (tx.type === 'CREDIT' ? 'text-green-600' : 'text-red-500')
                  }
                >
                  {tx.type === 'CREDIT' ? '+' : '-'}{formatNaira(tx.amount_kobo)}
                </span>
                <span className="text-xs text-gray-400">
                  Bal: {formatNaira(tx.running_balance_kobo)}
                </span>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Pagination */}
      <div className="mt-6 flex justify-between">
        <button
          onClick={() => setPage(p => Math.max(1, p - 1))}
          disabled={page === 1}
          className="rounded-lg border px-4 py-1 text-sm disabled:opacity-40"
        >
          Prev
        </button>
        <span className="text-sm text-gray-500">{page} / {lastPage}</span>
        <button
          onClick={() => setPage(p => Math.min(lastPage, p + 1))}
          disabled={page === lastPage}
          className="rounded-lg border px-4 py-1 text-sm disabled:opacity-40"
        >
          Next
        </button>
      </div>
    </div>
  )
}
