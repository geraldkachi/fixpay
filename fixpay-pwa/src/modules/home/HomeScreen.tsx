import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { BellIcon } from '@heroicons/react/24/outline'
import { useAuthStore } from '@/store/auth.store'
import { BalanceCard } from '@/components/feature/BalanceCard'
import { ServiceGrid } from '@/components/feature/ServiceGrid'
import { TransactionItem } from '@/components/feature/TransactionItem'
import { Logo } from '@/components/ui/Logo'
import { walletService } from '@/lib/services/wallet.service'

export function HomeScreen() {
  const { user } = useAuthStore()
  const navigate = useNavigate()

  const { data: txPage } = useQuery({
    queryKey: ['transactions', { page: 0, size: 5 }],
    queryFn: () => walletService.getTransactions(0, 5),
    staleTime: 30_000,
  })

  const txns = txPage?.content ?? []

  return (
    <div className="flex flex-col bg-[#F2F2F7] min-h-[100dvh] pb-nav">
      {/* Header */}
      <div className="pt-safe px-4 pb-4 bg-[#F2F2F7] flex items-center justify-between">
        <Logo size="sm" />
        <div className="flex-1 min-w-0 mx-3">
          <p className="text-[13px] text-gray-400 leading-tight">Welcome back,</p>
          <h1 className="text-[18px] font-bold text-gray-900 truncate">{user?.firstName ?? 'there'} 👋</h1>
        </div>
        <button className="w-10 h-10 rounded-full bg-white flex items-center justify-center shadow-sm pressable shrink-0">
          <BellIcon className="w-5 h-5 text-gray-500" />
        </button>
      </div>

      {/* Balance card */}
      <div className="animate-slide-up">
        <BalanceCard />
      </div>

      {/* Quick services */}
      <section className="px-4 mt-5 animate-slide-up">
        <h2 className="text-[13px] font-semibold text-gray-500 uppercase tracking-wide mb-3">Quick Pay</h2>
        <ServiceGrid compact />
      </section>

      {/* Recent transactions */}
      <section className="px-4 mt-5 animate-slide-up">
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-[13px] font-semibold text-gray-500 uppercase tracking-wide">Recent Transactions</h2>
          <button className="text-[13px] font-semibold" style={{ color: 'var(--brand-primary)' }} onClick={() => navigate('/wallet')}>See All</button>
        </div>
        {txns.length === 0 ? (
          <div className="bg-white rounded-[16px] p-8 text-center text-gray-400 text-[14px]">No transactions yet</div>
        ) : (
          <div className="bg-white rounded-[16px] overflow-hidden">
            {txns.map(tx => <TransactionItem key={tx.id} tx={tx} onClick={() => navigate(`/wallet/transactions/${tx.id}`)} />)}
          </div>
        )}
      </section>
    </div>
  )
}
