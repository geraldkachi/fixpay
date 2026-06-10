'use client'

import { useState } from 'react'
import { transferService } from '@/lib/services'
import { formatNaira } from '@/lib/format'

type Mode = 'bank' | 'wallet'

export default function TransfersPage() {
  const [mode, setMode] = useState<Mode>('bank')
  const [form, setForm] = useState({
    account_number: '',
    bank_code: '',
    recipient_phone: '',
    amount: '',
    narration: '',
    pin: '',
  })
  const [loading, setLoading] = useState(false)
  const [success, setSuccess] = useState<string | null>(null)
  const [error, setError] = useState('')

  function setField(key: keyof typeof form, value: string) {
    setForm(f => ({ ...f, [key]: value }))
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setSuccess(null)
    setLoading(true)
    const amount_kobo = Math.round(parseFloat(form.amount) * 100)
    try {
      if (mode === 'bank') {
        await transferService.toBank({
          account_number: form.account_number,
          bank_code: form.bank_code,
          amount_kobo,
          narration: form.narration,
          pin: form.pin,
        })
      } else {
        await transferService.toWallet({
          recipient_phone: form.recipient_phone,
          amount_kobo,
          narration: form.narration,
          pin: form.pin,
        })
      }
      setSuccess(`Transfer of ${formatNaira(amount_kobo)} initiated!`)
      setForm({ account_number: '', bank_code: '', recipient_phone: '', amount: '', narration: '', pin: '' })
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Transfer failed')
    } finally {
      setLoading(false)
    }
  }

  const inp = (label: string, key: keyof typeof form, type = 'text', placeholder = '') => (
    <div key={key}>
      <label className="mb-1 block text-sm font-medium text-gray-700">{label}</label>
      <input
        type={type}
        placeholder={placeholder}
        value={form[key]}
        onChange={e => setField(key, e.target.value)}
        className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
      />
    </div>
  )

  return (
    <div className="mx-auto max-w-lg px-4 py-8">
      <h1 className="mb-6 text-xl font-bold text-gray-900">Send Money</h1>

      <div className="mb-6 flex rounded-xl overflow-hidden border">
        {(['bank', 'wallet'] as Mode[]).map(m => (
          <button
            key={m}
            onClick={() => setMode(m)}
            className={
              'flex-1 py-2 text-sm font-medium ' +
              (mode === m ? 'bg-green-600 text-white' : 'bg-white text-gray-700')
            }
          >
            {m === 'bank' ? 'Bank Transfer' : 'FixPay Wallet'}
          </button>
        ))}
      </div>

      <form onSubmit={handleSubmit} className="space-y-4 rounded-2xl bg-white p-6 shadow">
        {mode === 'bank' ? (
          <>
            {inp('Account Number', 'account_number', 'text', '0123456789')}
            {inp('Bank Code', 'bank_code', 'text', '058')}
          </>
        ) : (
          inp('Recipient Phone', 'recipient_phone', 'tel', '+2348000000000')
        )}
        {inp('Amount (NGN)', 'amount', 'number', '500')}
        {inp('Narration (optional)', 'narration')}
        {inp('Transaction PIN', 'pin', 'password', '6-digit PIN')}

        {error && <p className="text-sm text-red-600">{error}</p>}
        {success && <p className="text-sm text-green-600">{success}</p>}

        <button
          type="submit"
          disabled={loading}
          className="w-full rounded-lg bg-green-600 py-2 text-sm font-semibold text-white hover:bg-green-700 disabled:opacity-50"
        >
          {loading ? 'Processing…' : 'Send Money'}
        </button>
      </form>
    </div>
  )
}
