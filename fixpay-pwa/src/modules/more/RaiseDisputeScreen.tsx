import { useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { PageHeader } from '@/components/layout/PageHeader'
import { Button } from '@/components/ui/Button'
//import { Input } from '@/components/ui/Input'
import api from '@/lib/api'
import { formatDateFull } from '@/lib/utils'

export function RaiseDisputeScreen() {
  const navigate = useNavigate()
  const { state } = useLocation()
  
  // prefillTxId, txDate, type (VTPASS | TRANSFER)
  const txId = state?.prefillTxId || ''
  const txDate = state?.txDate || ''
  const txType = state?.type || 'VTPASS'

  const [category, setCategory] = useState('NOT_DELIVERED')
  const [reason, setReason] = useState('')
  
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!reason) {
      setError('Please fill in the details of your dispute.')
      return
    }
    
    setLoading(true)
    setError('')
    
    try {
      await api.post('/disputes', {
        related_payment_id: txId,
        related_payment_type: txType,
        category: category,
        reason: reason
      })
      navigate('/more/disputes', { replace: true })
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to raise dispute. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex flex-col h-[100dvh] bg-gray-50">
      <PageHeader title="Raise Dispute" onBack={() => navigate(-1)} />
      
      <div className="flex-1 overflow-y-auto px-4 py-6">
        <form onSubmit={handleSubmit} className="flex flex-col gap-5">
          {error && <div className="p-3 bg-red-100 text-red-600 rounded-[12px] text-sm font-medium">{error}</div>}

          {txId && (
            <div className="bg-white p-4 rounded-[16px] border border-gray-100 mb-2">
              <p className="text-xs text-gray-500 mb-1">Disputing Transaction</p>
              <p className="font-mono text-sm font-bold text-gray-900 break-all">{txId}</p>
              {txDate && <p className="text-xs text-gray-500 mt-1">{formatDateFull(txDate)}</p>}
            </div>
          )}

          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-semibold text-gray-700 ml-1">Subject Matter</label>
            <select
              className="w-full bg-white border border-gray-200 rounded-[16px] px-4 py-3.5 text-gray-900 font-medium focus:outline-none focus:ring-2 focus:ring-[var(--brand-primary)] appearance-none"
              value={category}
              onChange={(e) => setCategory(e.target.value)}
            >
              <option value="NOT_DELIVERED">Service not provided</option>
              <option value="DUPLICATE">Debited more than once</option>
              <option value="WRONG_AMOUNT">Wrong amount debited</option>
              <option value="UNAUTHORIZED">Unauthorized transaction</option>
              <option value="OTHER">Other</option>
            </select>
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-semibold text-gray-700 ml-1">Details</label>
            <textarea
              className="w-full bg-white border border-gray-200 rounded-[16px] px-4 py-3.5 text-gray-900 font-medium focus:outline-none focus:ring-2 focus:ring-[var(--brand-primary)] resize-none"
              rows={4}
              placeholder="Please provide details about your dispute..."
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
          </div>

          <div className="mt-4 pb-safe">
            <Button type="submit" fullWidth disabled={loading}>
              {loading ? 'Submitting...' : 'Submit Dispute'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}
