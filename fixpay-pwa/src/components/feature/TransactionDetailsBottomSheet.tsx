import { BottomSheet } from '@/components/ui/BottomSheet'
import { Badge, statusBadge } from '@/components/ui/Badge'
import type { Transaction } from '@/types'
import { formatCurrency, formatDateFull, cn } from '@/lib/utils'
import { useState } from 'react'

interface TransactionDetailsBottomSheetProps {
  tx: Transaction | null
  open: boolean
  onClose: () => void
}

export function TransactionDetailsBottomSheet({ tx, open, onClose }: TransactionDetailsBottomSheetProps) {
  const [copied, setCopied] = useState(false)
  if (!tx) return null

  const isCredit = tx.type === 'transfer_in' || tx.type === 'wallet_funding'
  const { label, variant } = statusBadge(tx.status)

  const handleCopyRef = () => {
    navigator.clipboard.writeText(tx.reference)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  const handleCopyCode = (code: string) => {
    navigator.clipboard.writeText(code)
  }

  return (
    <BottomSheet open={open} onClose={onClose} title="Transaction Details">
      <div className="px-4 pt-2 pb-6 flex flex-col items-center">
        {/* Status indicator */}
        <div className="mb-4">
          <Badge variant={variant} className="text-[12px] px-3 py-1 font-semibold">
            {label}
          </Badge>
        </div>

        {/* Amount */}
        <h2 className={cn("text-[32px] font-black text-center mb-6", isCredit ? "text-ios-green" : "text-gray-900")}>
          {isCredit ? "+" : "-"}{formatCurrency(tx.amountKobo)}
        </h2>

        {/* Info list card */}
        <div className="w-full bg-gray-50 rounded-[20px] p-4 flex flex-col gap-3.5 mb-6">
          <div className="flex justify-between items-start">
            <span className="text-[13px] text-gray-500">Description</span>
            <span className="text-[14px] font-semibold text-gray-900 text-right max-w-[65%]">{tx.description}</span>
          </div>

          <div className="flex justify-between items-center">
            <span className="text-[13px] text-gray-500">Reference</span>
            <div className="flex items-center gap-1.5 cursor-pointer" onClick={handleCopyRef}>
              <span className="text-[13px] font-mono text-gray-800 break-all">{tx.reference}</span>
              <span className="text-[11px] text-blue-600 bg-blue-50 px-1.5 py-0.5 rounded font-sans shrink-0">
                {copied ? "Copied" : "Copy"}
              </span>
            </div>
          </div>

          <div className="flex justify-between items-center">
            <span className="text-[13px] text-gray-500">Date & Time</span>
            <span className="text-[13px] font-medium text-gray-900">{formatDateFull(tx.createdAt)}</span>
          </div>

          <div className="flex justify-between items-center">
            <span className="text-[13px] text-gray-500">Transaction Type</span>
            <span className="text-[13px] font-medium text-gray-900 uppercase">
              {tx.type.replace('_', ' ')}
            </span>
          </div>

          {tx.feeKobo > 0 && (
            <div className="flex justify-between items-center">
              <span className="text-[13px] text-gray-500">Network Fee</span>
              <span className="text-[13px] font-medium text-gray-900">{formatCurrency(tx.feeKobo)}</span>
            </div>
          )}

          {tx.counterpartyName && (
            <div className="flex justify-between items-center">
              <span className="text-[13px] text-gray-500">Beneficiary / Sender</span>
              <span className="text-[13px] font-medium text-gray-900">{tx.counterpartyName}</span>
            </div>
          )}

          {/* Tokens / PINs if available */}
          {tx.token && (
            <div className="mt-1 pt-3 border-t border-gray-200 flex flex-col gap-1.5">
              <span className="text-[12px] text-gray-400 font-semibold uppercase">Token / Meter Units</span>
              <div className="flex items-center justify-between bg-yellow-50 border border-yellow-200 rounded-[12px] px-3 py-2">
                <span className="font-mono text-[16px] font-bold text-yellow-800 tracking-wider break-all">{tx.token}</span>
                <button type="button" onClick={() => handleCopyCode(tx.token!)} className="text-[12px] font-semibold text-yellow-700 bg-yellow-100 px-2 py-1 rounded">
                  Copy
                </button>
              </div>
              {tx.units && <span className="text-[12px] text-gray-500 mt-0.5">Units: {tx.units}</span>}
            </div>
          )}

          {tx.Pin && (
            <div className="mt-1 pt-3 border-t border-gray-200 flex flex-col gap-1.5">
              <span className="text-[12px] text-gray-400 font-semibold uppercase">Exam PIN</span>
              <div className="flex items-center justify-between bg-teal-50 border border-teal-200 rounded-[12px] px-3 py-2">
                <span className="font-mono text-[16px] font-bold text-teal-800 tracking-wider break-all">{tx.Pin}</span>
                <button type="button" onClick={() => handleCopyCode(tx.Pin!)} className="text-[12px] font-semibold text-teal-700 bg-teal-100 px-2 py-1 rounded">
                  Copy
                </button>
              </div>
            </div>
          )}

          {tx.purchased_code && !tx.token && !tx.Pin && (
            <div className="mt-1 pt-3 border-t border-gray-200 flex flex-col gap-1.5">
              <span className="text-[12px] text-gray-400 font-semibold uppercase">Purchased PIN/Code</span>
              <div className="flex items-center justify-between bg-gray-100 border border-gray-200 rounded-[12px] px-3 py-2">
                <span className="font-mono text-[16px] font-bold text-gray-800 tracking-wider break-all">{tx.purchased_code}</span>
                <button type="button" onClick={() => handleCopyCode(tx.purchased_code!)} className="text-[12px] font-semibold text-gray-700 bg-gray-200 px-2 py-1 rounded">
                  Copy
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </BottomSheet>
  )
}
