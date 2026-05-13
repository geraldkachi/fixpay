import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useQuery } from '@tanstack/react-query'
import { queryClient } from '@/lib/query-client'
import type { ServiceVariation } from '@/types'
import { paymentsService } from '@/lib/services/payments.service'
import { authService } from '@/lib/services/auth.service'
import { PageHeader } from '@/components/layout/PageHeader'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'
import { BottomSheet } from '@/components/ui/BottomSheet'
import { PinPad } from '@/components/ui/PinPad'
import { Spinner } from '@/components/ui/Spinner'

const PRODUCTS = [
  { id: 'ui-insure',                   label: 'Third Party Motor', description: 'TP Motor Insurance',   billerLabel: 'Vehicle Plate Number', billerPlaceholder: 'ABC-123-XY' },
  { id: 'personal-accident-insurance', label: 'Personal Accident', description: 'PA Insurance',         billerLabel: 'Phone Number',         billerPlaceholder: '08012345678' },
  { id: 'home-cover-insurance',        label: 'Home Cover',        description: 'Home Cover Insurance', billerLabel: 'Phone Number',         billerPlaceholder: '08012345678' },
]

const schema = z.object({
  serviceId:     z.string().min(1),
  billersCode:   z.string().min(3, 'Enter required details'),
  variationCode: z.string().min(1, 'Select a plan'),
  phone:         z.string().regex(/^0[789]\d{9}$/, 'Enter a valid phone number'),
})
type FormData = z.infer<typeof schema>

export function InsuranceScreen() {
  const navigate = useNavigate()
  const [showPin, setShowPin] = useState(false)
  const [pin, setPin] = useState('')
  const [pinError, setPinError] = useState('')
  const [pending, setPending] = useState<FormData | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const { register, handleSubmit, setValue, watch, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { serviceId: 'ui-insure', billersCode: '', variationCode: '', phone: '' },
  })
  const serviceId = watch('serviceId')
  const variationCode = watch('variationCode')

  const { data: variations = [], isLoading: varsLoading } = useQuery<ServiceVariation[]>({
    queryKey: ['variations', serviceId],
    queryFn: () => paymentsService.getVariations(serviceId),
  })
  const chosen = variations.find(v => v.variationCode === variationCode)

  const onSubmit = (data: FormData) => { setPending(data); setPin(''); setPinError(''); setShowPin(true) }

  const handlePinChange = async (val: string) => {
    setPin(val); setPinError('')
    if (val.length < 6 || !pending || submitting) return
    setSubmitting(true)
    try {
      await authService.verifyPin(val)
      const res = await paymentsService.insurance({ ...pending, variationCode: pending.variationCode })
      queryClient.invalidateQueries({ queryKey: ['wallet'] })
      queryClient.invalidateQueries({ queryKey: ['transactions'] })
      navigate('/payments/receipt', {
        state: {
          type: 'insurance',
          serviceId: pending.serviceId,
          exam: chosen?.name,
          amount: parseFloat(chosen?.variationAmount ?? '0'),
          requestId: res.requestId,
          date: res.transaction_date,
          purchased_code: res.purchased_code,
        },
      })
    } catch {
      setPinError('Incorrect PIN or payment failed. Try again.')
      setPin('')
      setSubmitting(false)
    }
  }

  return (
    <div className="flex flex-col h-[100dvh] bg-[#F2F2F7]">
      <PageHeader title="Insurance" onBack="default" />
      <div className="flex-1 overflow-y-auto no-scrollbar px-4 pt-4 pb-8 animate-slide-up">

        {/* Product selector */}
        <p className="text-[13px] font-semibold text-gray-500 uppercase tracking-wide mb-2">Insurance Type</p>
        <div className="flex flex-col gap-2 mb-5">
          {PRODUCTS.map(p => (
            <button key={p.id} onClick={() => { setValue('serviceId', p.id); setValue('variationCode', '') }}
              className="flex items-center gap-3 bg-white px-4 py-3 rounded-[14px] border-2 transition-all pressable text-left"
              style={{ borderColor: serviceId === p.id ? 'var(--brand-primary)' : 'transparent' }}>
              <div className="flex flex-col">
                <span className="text-[15px] font-semibold text-gray-800">{p.label}</span>
                <span className="text-[12px] text-gray-500">{p.description}</span>
              </div>
            </button>
          ))}
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <Input
            label={PRODUCTS.find(p => p.id === serviceId)?.billerLabel ?? 'Reference'}
            type="text"
            placeholder={PRODUCTS.find(p => p.id === serviceId)?.billerPlaceholder ?? ''}
            error={errors.billersCode?.message} {...register('billersCode')} />
          <Input label="Phone Number" type="tel" inputMode="tel" placeholder="08012345678"
            error={errors.phone?.message} {...register('phone')} />

          {/* Plan selector */}
          <div>
            <p className="text-[13px] font-semibold text-gray-500 uppercase tracking-wide mb-2">Select Plan</p>
            {varsLoading ? (
              <div className="flex justify-center py-4"><Spinner /></div>
            ) : (
              <div className="flex flex-col gap-2">
                {variations.map(v => (
                  <button key={v.variationCode} type="button" onClick={() => setValue('variationCode', v.variationCode)}
                    className="flex items-center justify-between bg-white rounded-[14px] px-4 py-3 border-2 transition-all pressable"
                    style={{ borderColor: variationCode === v.variationCode ? 'var(--brand-primary)' : 'transparent' }}>
                    <span className="text-[15px] font-medium text-gray-800">{v.name}</span>
                    <span className="text-[15px] font-bold" style={{ color: 'var(--brand-primary)' }}>
                      ₦{parseFloat(v.variationAmount).toLocaleString()}
                    </span>
                  </button>
                ))}
              </div>
            )}
            {errors.variationCode && <p className="text-ios-red text-[13px] mt-1">{errors.variationCode.message}</p>}
          </div>

          <Button type="submit" fullWidth disabled={!chosen}>
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
