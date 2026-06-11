import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { BellIcon } from '@heroicons/react/24/outline'
import { useAuthStore } from '@/store/auth.store'
import { BalanceCard } from '@/components/feature/BalanceCard'
import { ServiceGrid } from '@/components/feature/ServiceGrid'
import { TransactionItem, txIcon } from '@/components/feature/TransactionItem'
import { Logo } from '@/components/ui/Logo'
import { walletService } from '@/lib/services/wallet.service'
import { TransactionDetailsBottomSheet } from '@/components/feature/TransactionDetailsBottomSheet'
import { RepeatPaymentBottomSheet } from '@/components/feature/RepeatPaymentBottomSheet'
import type { Transaction } from '@/types'
import { useFavouritesStore } from '@/store/favourites.store'
import { formatCurrency, formatDateShort } from '@/lib/utils'
import { Badge, statusBadge } from '@/components/ui/Badge'
import { HeartIcon as HeartSolid } from '@heroicons/react/24/solid'

export function HomeScreen() {
  const { user } = useAuthStore()
  const navigate = useNavigate()
  const [selectedTx, setSelectedTx] = useState<Transaction | null>(null)
  const [repeatTx, setRepeatTx] = useState<Transaction | null>(null)
  const { favourites, removeFavourite } = useFavouritesStore()

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

      {/* Favourites */}
      <section className="mt-5 animate-slide-up">
        <div className="flex items-center justify-between mb-3 px-4">
          <h2 className="text-[13px] font-semibold text-gray-500 uppercase tracking-wide">Favourites</h2>
          <button className="text-[13px] font-semibold" style={{ color: 'var(--brand-primary)' }} onClick={() => navigate('/wallet')}>History</button>
        </div>
        {favourites.length === 0 ? (
          <div className="mx-4 bg-white rounded-[16px] p-8 text-center text-gray-400 text-[14px]">No saved favourites</div>
        ) : (
          <div className="flex overflow-x-auto no-scrollbar px-4 gap-3 pb-2">
            {favourites.map(tx => {
              const { label, variant } = statusBadge(tx.status)
              return (
                <div 
                  key={tx.id} 
                  className="bg-white rounded-[16px] p-4 min-w-[200px] max-w-[200px] shrink-0 cursor-pointer active:scale-95 transition-transform pressable flex flex-col justify-between relative"
                  style={{ boxShadow: '0 4px 20px rgba(0,0,0,0.03)' }}
                  onClick={() => setRepeatTx(tx)}
                >
                  <button 
                    className="absolute top-3 right-3 p-1 rounded-full bg-red-50 hover:bg-red-100 z-10"
                    onClick={(e) => { e.stopPropagation(); removeFavourite(tx.id) }}
                  >
                    <HeartSolid className="w-4 h-4 text-red-500" />
                  </button>
                  <div className="flex items-start gap-3 mb-4 pr-6">
                    {txIcon(tx)}
                    <div className="flex-1 min-w-0">
                      <p className="text-[14px] font-semibold text-gray-900 truncate">{tx.counterpartyName || tx.serviceName || 'Payment'}</p>
                      <p className="text-[12px] text-gray-500 truncate">{tx.description}</p>
                    </div>
                  </div>
                  <div className="flex items-end justify-between mt-auto">
                    <div>
                      <p className="text-[11px] text-gray-400 mb-1">{formatDateShort(tx.createdAt)}</p>
                      <Badge variant={variant} className="text-[9px] px-1.5 py-0 h-[18px]">{label}</Badge>
                    </div>
                    <p className="text-[15px] font-bold text-gray-900">{formatCurrency(tx.amountKobo)}</p>
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </section>

      <TransactionDetailsBottomSheet tx={selectedTx} open={selectedTx !== null} onClose={() => setSelectedTx(null)} />
      <RepeatPaymentBottomSheet tx={repeatTx} open={repeatTx !== null} onClose={() => setRepeatTx(null)} />
    </div>
  )
}
