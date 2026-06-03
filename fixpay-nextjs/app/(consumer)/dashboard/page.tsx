'use client'

import { useState, useEffect } from 'react'
import Link from 'next/link'
import { walletService } from '@/lib/services'
import { useAuth } from '@/lib/auth-context'
import { formatNaira, formatDate } from '@/lib/format'
import type { Wallet, LedgerEntry, Paginated } from '@/lib/types'

export default function DashboardPage() {
  const { user } = useAuth()
  const [wallet, setWallet] = useState<Wallet | null>(null)
  const [txs, setTxs] = useState<LedgerEntry[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function load() {
      try {
        const [w, t] = await Promise.all([
          walletService.get(),
          walletService.transactions({ per_page: '5' }),
        ])
        setWallet(w)
        setTxs((t as Paginated<LedgerEntry>).data)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <span className="h-8 w-8 rounded-full border-4 border-green-500 border-t-transparent animate-spin" />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-lg px-4 py-8">
      <h1 className="mb-1 text-lg font-semibold text-gray-700">
        Welcome, {user?.first_name}!
      </h1>

      {/* Wallet card */}
      <div className="mt-4 rounded-2xl bg-green-600 p-6 text-white shadow">
        <p className="text-sm opacity-80">Available balance</p>
        <p className="mt-1 text-4xl font-bold">
          {wallet ? formatNaira(wallet.balance_kobo) : '—'}
        </p>
        {wallet?.virtual_account_number && (
          <p className="mt-3 text-sm opacity-70">
            {wallet.virtual_account_bank} · {wallet.virtual_account_number}
          </p>
        )}
      </div>

      {/* Quick actions */}
      <div className="mt-6 grid grid-cols-3 gap-3">
        {[
          { href: '/consumer/transfers', label: 'Send' },
          { href: '/consumer/payments', label: 'Pay Bills' },
          { href: '/consumer/wallet', label: 'History' },
        ].map(a => (
          <Link
            key={a.href}
            href={a.href}
            className="flex h-16 items-center justify-center rounded-xl bg-white text-sm font-medium text-gray-700 shadow hover:bg-gray-50"
          >
            {a.label}
          </Link>
        ))}
      </div>

      {/* Recent transactions */}
      <div className="mt-6">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-sm font-semibold text-gray-700">Recent</h2>
          <Link href="/consumer/wallet" className="text-xs text-green-600 hover:underline">
            See all
          </Link>
        </div>
        <div className="space-y-2">
          {txs.length === 0 && (
            <p className="text-sm text-gray-400">No transactions yet.</p>
          )}
          {txs.map(tx => (
            <div key={tx.id} className="flex items-center justify-between rounded-xl bg-white p-3 shadow-sm">
              <div>
                <p className="text-sm font-medium text-gray-800">{tx.description}</p>
                <p className="text-xs text-gray-400">{formatDate(tx.created_at)}</p>
              </div>
              <span
                className={
                  'text-sm font-semibold ' +
                  (tx.type === 'CREDIT' ? 'text-green-600' : 'text-red-500')
                }
              >
                {tx.type === 'CREDIT' ? '+' : '-'}{formatNaira(tx.amount_kobo)}
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
