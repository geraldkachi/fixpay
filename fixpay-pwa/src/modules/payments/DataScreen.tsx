import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { queryClient } from '@/lib/query-client'
import type { ServiceVariation } from '@/types'
import { PageHeader } from '@/components/layout/PageHeader'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'
import { BottomSheet } from '@/components/ui/BottomSheet'
import { PinPad } from '@/components/ui/PinPad'
import { Spinner } from '@/components/ui/Spinner'

const NETWORKS = [
  { id: 'mtn-data',    label: 'MTN',    color: '#FFCC00', text: '#000' },
  { id: 'airtel-data', label: 'Airtel', color: '#EF3125', text: '#FFF' },
  { id: 'glo-data',    label: 'Glo',    color: '#009900', text: '#FFF' },
]

const schema = z.object({
  serviceId:     z.string().min(1, 'Select a network'),
  billersCode:   z.string().regex(/^0[789]\d{9}$/, 'Enter a valid 11-digit number'),
  variationCode: z.string().min(1, 'Select a bundle'),
})
type FormData = z.infer<typeof schema>

export function DataScreen() {
  const navigate = useNavigate()
  const [showPin, setShowPin] = useState(false)
  const [pin, setPin] = useState('')
  const [pinError, setPinError] = useState('')
  const [pending, setPending] = useState<FormData | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const { register, handleSubmit, setValue, watch, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { serviceId: 'mtn-data', billersCode: '', variationCode: '' },
  })
  const serviceId = watch('serviceId')
  const variationCode = watch('variationCode')

  const { data: varData, isLoading: varsLoading } = useQuery({
    queryKey: ['variations', serviceId],
    queryFn: () => api.get<{ variations: ServiceVariation[] }>(`/payments/variations/${serviceId}`).then(r => r.data),
  })
  const variations = varData?.variations ?? []
  const chosen = variations.find(v => v.variationCode === variationCode)

  const onSubmit = (data: FormData) => { setPending(data); setPin(''); setPinError(''); setShowPin(true) }

  const handlePinChange = async (val: string) => {
    setPin(val); setPinError('')
    if (val.length < 6 || !pending || submitting) return
    setSubmitting(true)
    try {
      await api.post('/auth/pin/verify', { pin: val })
      const res = await api.post<{ requestId: string; transaction_date: string }>('/payments/data', pending)
      queryClient.invalidateQueries({ queryKey: ['wallet'] })
      queryClient.invalidateQueries({ queryKey: ['transactions'] })
      navigate('/payments/receipt', {
        state: { type: 'data', bundle: chosen?.name, phone: pending.billersCode, amount: parseFloat(chosen?.variationAmount ?? '0'), requestId: res.data.requestId, date: res.data.transaction_date },
      })
    } catch {
      setPinError('Incorrect PIN or purchase failed. Try again.')
      setPin('')
      setSubmitting(false)
    }
  }

  return (
    <div className="flex flex-col h-[100dvh] bg-[#F2F2F7]">
      <PageHeader title="Buy Data" onBack="default" />
      <div className="flex-1 overflow-y-auto no-scrollbar px-4 pt-4 pb-8 animate-slide-up">

        {/* Network selector */}
        <p className="text-[13px] font-semibold text-gray-500 uppercase tracking-wide mb-2">Select Network</p>
        <div className="grid grid-cols-3 gap-2 mb-5">
          {NETWORKS.map(n => (
            <button key={n.id} onClick={() => { setValue('serviceId', n.id); setValue('variationCode', '') }}
              className="flex flex-col items-center py-3 rounded-[14px] border-2 transition-all pressable"
              style={{ borderColor: serviceId === n.id ? 'var(--brand-primary)' : 'transparent', background: n.color }}>
              <span className="text-[12px] font-bold" style={{ color: n.text }}>{n.label}</span>
            </button>
          ))}
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <Input label="Phone Number" type="tel" inputMode="tel" placeholder="08012345678"
            error={errors.billersCode?.message} {...register('billersCode')} />

          {/* Bundle list */}
          <div>
            <p className="text-[13px] font-semibold text-gray-500 uppercase tracking-wide mb-2">Select Bundle</p>
            {varsLoading ? <div className="flex justify-center py-4"><Spinner /></div> : (
              <div className="flex flex-col gap-2">
                {variations.map(v => (
                  <button key={v.variationCode} type="button" onClick={() => setValue('variationCode', v.variationCode)}
                    className="flex items-center justify-between bg-white rounded-[14px] px-4 py-3 border-2 transition-all pressable"
                    style={{ borderColor: variationCode === v.variationCode ? 'var(--brand-primary)' : 'transparent' }}>
                    <span className="text-[15px] font-medium text-gray-800">{v.name}</span>
                    <span className="text-[15px] font-bold" style={{ color: 'var(--brand-primary)' }}>₦{parseFloat(v.variationAmount).toLocaleString()}</span>
                  </button>
                ))}
              </div>
            )}
            {errors.variationCode && <p className="text-ios-red text-[13px] mt-1">{errors.variationCode.message}</p>}
          </div>

          <Button type="submit" fullWidth className="mt-2" disabled={!chosen}>
            {chosen ? `Pay ₦${parseFloat(chosen.variationAmount).toLocaleString()}` : 'Continue'}
          </Button>
        </form>
      </div>

      <BottomSheet open={showPin} onClose={() => setShowPin(false)} title="Enter PIN" dismissible={!submitting}>
        <div className="px-2 pt-2 pb-4">
          <PinPad value={pin} onChange={handlePinChange} error={pinError} disabled={submitting} />
        </div>
      </BottomSheet>
    </div>
  )
}
