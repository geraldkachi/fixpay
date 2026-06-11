import { ArrowDownLeftIcon, ArrowUpRightIcon, BoltIcon, DevicePhoneMobileIcon, TvIcon, AcademicCapIcon, WalletIcon } from '@heroicons/react/24/outline'
import { formatCurrency, formatDateShort } from '@/lib/utils'
import { Badge, statusBadge } from '@/components/ui/Badge'
import type { Transaction } from '@/types'
import { cn } from '@/lib/utils'

interface TransactionItemProps {
  tx: Transaction
  onClick?: () => void
}

export function txIcon(tx: Transaction) {
  if (tx.type === 'transfer_in')    return <div className="w-10 h-10 rounded-full bg-green-100  flex items-center justify-center shrink-0"><ArrowDownLeftIcon  className="w-5 h-5 text-green-600" /></div>
  if (tx.type === 'transfer_out')   return <div className="w-10 h-10 rounded-full bg-red-100    flex items-center justify-center shrink-0"><ArrowUpRightIcon   className="w-5 h-5 text-red-600"   /></div>
  if (tx.type === 'wallet_funding') return <div className="w-10 h-10 rounded-full bg-blue-100   flex items-center justify-center shrink-0"><WalletIcon          className="w-5 h-5 text-blue-600"  /></div>

  // bill_payment — use serviceId
  const sid = tx.serviceId ?? ''
  if (sid.includes('electric'))    return <div className="w-10 h-10 rounded-full bg-yellow-100 flex items-center justify-center shrink-0"><BoltIcon             className="w-5 h-5 text-yellow-600" /></div>
  if (sid.includes('tv') || ['dstv','gotv','startimes'].includes(sid)) return <div className="w-10 h-10 rounded-full bg-purple-100 flex items-center justify-center shrink-0"><TvIcon className="w-5 h-5 text-purple-600" /></div>
  if (['jamb','waec','waec-registration'].includes(sid)) return <div className="w-10 h-10 rounded-full bg-teal-100 flex items-center justify-center shrink-0"><AcademicCapIcon className="w-5 h-5 text-teal-600" /></div>
  return <div className="w-10 h-10 rounded-full bg-orange-100 flex items-center justify-center shrink-0"><DevicePhoneMobileIcon className="w-5 h-5 text-orange-600" /></div>
}

export function TransactionItem({ tx, onClick }: TransactionItemProps) {
  const isCredit = tx.type === 'transfer_in' || tx.type === 'wallet_funding'
  const { label, variant } = statusBadge(tx.status)
  const showBadge = tx.status === 'failed' || tx.status === 'reversed' || tx.status === 'processing'

  return (
    <div className={cn('flex items-center gap-3 px-4 py-3 bg-white border-b border-black/5 last:border-0', onClick && 'pressable cursor-pointer active:bg-gray-50')} onClick={onClick}>
      {txIcon(tx)}
      <div className="flex-1 min-w-0">
        <p className="text-[15px] font-medium text-gray-900 truncate">{tx.description}</p>
        <div className="flex items-center gap-2 mt-0.5">
          <span className="text-[12px] text-gray-400">{formatDateShort(tx.createdAt)}</span>
          {showBadge && <Badge variant={variant} className="text-[10px] px-1.5 py-0 h-4">{label}</Badge>}
        </div>
      </div>
      <div className="text-right shrink-0">
        <p className={cn('text-[15px] font-semibold', isCredit ? 'text-ios-green' : 'text-gray-900')}>
          {isCredit ? '+' : '-'}{formatCurrency(tx.amountKobo)}
        </p>
        {tx.feeKobo > 0 && <p className="text-[11px] text-gray-400">+{formatCurrency(tx.feeKobo)} fee</p>}
      </div>
    </div>
  )
}
