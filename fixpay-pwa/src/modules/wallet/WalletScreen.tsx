import { useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { formatCurrency } from '@/lib/utils'
import { PageHeader } from '@/components/layout/PageHeader'
import { TransactionItem } from '@/components/feature/TransactionItem'
import { Spinner } from '@/components/ui/Spinner'
import type { Wallet, Transaction } from '@/types'

export function WalletScreen() {
  const { data: wallet } = useQuery<Wallet>({
    queryKey: ['wallet'],
    queryFn: () => api.get<Wallet>('/wallet/me').then(r => r.data),
    staleTime: 30_000,
  })

  const { data: txPage, isLoading } = useQuery({
    queryKey: ['transactions', { page: 0, size: 50 }],
    queryFn: () => api.get<{ content: Transaction[] }>('/wallet/transactions?page=0&size=50').then(r => r.data),
  })

  const txns = txPage?.content ?? []

  return (
    <div className="flex flex-col bg-[#F2F2F7] min-h-[100dvh] pb-nav">
      <PageHeader title="Wallet" />

      {/* Account info card */}
      <div className="mx-4 mt-4 bg-white rounded-[20px] p-5 animate-slide-up">
        <p className="text-[13px] text-gray-400 mb-1">Available Balance</p>
        <p className="text-[30px] font-black text-gray-900">{formatCurrency(wallet?.balanceKobo ?? 0)}</p>
        {wallet?.virtualAccount && (
          <div className="mt-3 pt-3 border-t border-gray-100">
            <p className="text-[12px] text-gray-400">Fund via bank transfer:</p>
            <p className="text-[16px] font-bold text-gray-900 mt-0.5">{wallet.virtualAccount.accountNumber}</p>
            <p className="text-[13px] text-gray-500">{wallet.virtualAccount.bankName}</p>
          </div>
        )}
      </div>

      {/* Transactions */}
      <section className="px-4 mt-5 animate-slide-up">
        <h2 className="text-[13px] font-semibold text-gray-500 uppercase tracking-wide mb-3">All Transactions</h2>
        {isLoading ? (
          <div className="flex justify-center py-8"><Spinner /></div>
        ) : txns.length === 0 ? (
          <div className="bg-white rounded-[16px] p-8 text-center text-gray-400 text-[14px]">No transactions yet</div>
        ) : (
          <div className="bg-white rounded-[16px] overflow-hidden">
            {txns.map(tx => <TransactionItem key={tx.id} tx={tx} />)}
          </div>
        )}
      </section>
    </div>
  )
}
