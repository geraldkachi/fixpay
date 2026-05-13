import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { DocumentDuplicateIcon } from '@heroicons/react/24/outline'
import type { Wallet } from '@/types'
import { walletService } from '@/lib/services/wallet.service'
import { PageHeader } from '@/components/layout/PageHeader'
import { Button } from '@/components/ui/Button'
import { vibrate } from '@/lib/utils'

export function FundWalletScreen() {
  const navigate = useNavigate()
  const { data: wallet } = useQuery<Wallet>({
    queryKey: ['wallet'],
    queryFn: () => walletService.getBalance(),
  })

  const acct = wallet?.virtualAccount?.accountNumber ?? '—'
  const bank = wallet?.virtualAccount?.bankName ?? ''

  const copy = () => {
    navigator.clipboard.writeText(acct)
    vibrate([10])
  }

  return (
    <div className="flex flex-col h-[100dvh] bg-[#F2F2F7]">
      <PageHeader title="Fund Wallet" onBack="default" />
      <div className="flex-1 px-4 pt-4 pb-8 animate-slide-up">

        {/* How it works */}
        <div className="bg-blue-50 rounded-[16px] p-4 mb-6 flex gap-3">
          <span className="text-2xl">ℹ️</span>
          <p className="text-[14px] text-blue-700 leading-relaxed">
            Transfer any amount to the account below. Your wallet is funded <strong>instantly</strong> once we receive your transfer.
          </p>
        </div>

        {/* Virtual account */}
        <div className="bg-white rounded-[20px] p-5">
          <p className="text-[13px] text-gray-400 mb-4 uppercase tracking-wide font-semibold">Your Dedicated Account</p>
          <p className="text-[13px] text-gray-500">Bank Name</p>
          <p className="text-[17px] font-semibold text-gray-900 mb-3">{bank}</p>
          <p className="text-[13px] text-gray-500">Account Number</p>
          <div className="flex items-center gap-3 mt-1">
            <p className="text-[32px] font-black text-gray-900 tracking-widest">{acct}</p>
            <button onClick={copy} className="w-10 h-10 bg-gray-100 rounded-full flex items-center justify-center pressable">
              <DocumentDuplicateIcon className="w-5 h-5 text-gray-500" />
            </button>
          </div>
          <p className="text-[13px] text-gray-400 mt-3">Account Name: <strong className="text-gray-700">FixPay / John Adeyemi</strong></p>
        </div>

        <div className="mt-6 flex flex-col gap-3">
          <Button fullWidth variant="outline" onClick={() => navigate(-1)}>Back to Wallet</Button>
        </div>
      </div>
    </div>
  )
}
