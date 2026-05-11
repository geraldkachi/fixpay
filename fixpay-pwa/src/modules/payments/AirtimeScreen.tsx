import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { api } from '@/lib/api'
import { queryClient } from '@/lib/query-client'
import { PageHeader } from '@/components/layout/PageHeader'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'
import { BottomSheet } from '@/components/ui/BottomSheet'
import { PinPad } from '@/components/ui/PinPad'

const NETWORKS = [
  { id: 'mtn',     label: 'MTN',     color: '#FFCC00', text: '#000' },
  { id: 'airtel',  label: 'Airtel',  color: '#EF3125', text: '#FFF' },
  { id: 'glo',     label: 'Glo',     color: '#009900', text: '#FFF' },
  { id: '9mobile', label: '9mobile', color: '#006600', text: '#FFF' },
]

const AMOUNTS = [100, 200, 500, 1000, 2000, 5000]

const schema = z.object({
  phone:     z.string().regex(/^0[789]\d{9}$/, 'Enter a valid 11-digit number'),
  amount:    z.coerce.number().min(50, 'Minimum ₦50').max(50000, 'Maximum ₦50,000'),
  serviceId: z.string().min(1, 'Select a network'),
})
type FormData = z.infer<typeof schema>

export function AirtimeScreen() {
  const navigate = useNavigate()
  const [showPin, setShowPin] = useState(false)
  const [pin, setPin] = useState('')
  const [pinError, setPinError] = useState('')
  const [pending, setPending] = useState<FormData | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const { register, handleSubmit, setValue, watch, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema) as Resolver<FormData>,
    defaultValues: { serviceId: 'mtn', phone: '', amount: 0 },
  })
  const selectedNet = watch('serviceId')

  const onSubmit = (data: FormData) => { setPending(data); setPin(''); setPinError(''); setShowPin(true) }

  const handlePinChange = async (val: string) => {
    setPin(val); setPinError('')
    if (val.length < 6 || !pending || submitting) return
    setSubmitting(true)
    try {
      await api.post('/auth/pin/verify', { pin: val })
      const res = await api.post<{ requestId: string; transaction_date: string }>('/payments/airtime', {
        serviceId: pending.serviceId, phone: pending.phone, amount: pending.amount,
      })
      queryClient.invalidateQueries({ queryKey: ['wallet'] })
      queryClient.invalidateQueries({ queryKey: ['transactions'] })
      navigate('/payments/receipt', {
        state: { type: 'airtime', network: pending.serviceId, phone: pending.phone, amount: pending.amount, requestId: res.data.requestId, date: res.data.transaction_date },
      })
    } catch {
      setPinError('Incorrect PIN or payment failed. Try again.')
      setPin('')
      setSubmitting(false)
    }
  }

  return (
    <div className="flex flex-col h-[100dvh] bg-[#F2F2F7]">
      <PageHeader title="Buy Airtime" onBack="default" />
      <div className="flex-1 overflow-y-auto no-scrollbar px-4 pt-4 pb-8 animate-slide-up">

        {/* Network selector */}
        <p className="text-[13px] font-semibold text-gray-500 uppercase tracking-wide mb-2">Select Network</p>
        <div className="grid grid-cols-4 gap-2 mb-5">
          {NETWORKS.map(n => (
            <button key={n.id} onClick={() => setValue('serviceId', n.id)}
              className="flex flex-col items-center gap-1.5 py-3 rounded-[14px] border-2 transition-all pressable"
              style={{ borderColor: selectedNet === n.id ? 'var(--brand-primary)' : 'transparent', background: n.color }}>
              <span className="text-[12px] font-bold" style={{ color: n.text }}>{n.label}</span>
            </button>
          ))}
        </div>
        {errors.serviceId && <p className="text-ios-red text-[13px] mb-3">{errors.serviceId.message}</p>}

        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <Input label="Phone Number" type="tel" inputMode="tel" placeholder="08012345678"
            error={errors.phone?.message} {...register('phone')} />

          {/* Quick amounts */}
          <div>
            <p className="text-[13px] font-semibold text-gray-500 uppercase tracking-wide mb-2">Amount</p>
            <div className="grid grid-cols-3 gap-2 mb-3">
              {AMOUNTS.map(a => (
                <button key={a} type="button" onClick={() => setValue('amount', a)}
                  className="py-2.5 bg-white rounded-[12px] text-[15px] font-semibold text-gray-700 pressable active:bg-gray-50 shadow-sm">
                  ₦{a.toLocaleString()}
                </button>
              ))}
            </div>
            <Input label="" type="number" inputMode="numeric" placeholder="Or enter amount" prefix="₦"
              error={errors.amount?.message} {...register('amount')} />
          </div>

          <Button type="submit" fullWidth className="mt-2">Continue</Button>
        </form>
      </div>

      <BottomSheet open={showPin} onClose={() => setShowPin(false)} title="Enter PIN" dismissible={!submitting}>
        <div className="px-2 pt-2 pb-4">
          <PinPad value={pin} onChange={handlePinChange} label="" error={pinError} disabled={submitting} />
        </div>
      </BottomSheet>
    </div>
  )
}
