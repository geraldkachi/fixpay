import { useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
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

const SERVICES = [
  { id: 'jamb',              label: 'JAMB' },
  { id: 'waec',              label: 'WAEC Result' },
  { id: 'waec-registration', label: 'WAEC Reg.' },
]

const schema = z.object({
  serviceId:     z.string().min(1),
  billersCode:   z.string().min(5, 'Enter exam number / profile ID'),
  variationCode: z.string().min(1, 'Select a service'),
  phone:         z.string().regex(/^0[789]\d{9}$/, 'Enter a valid phone number'),
})
type FormData = z.infer<typeof schema>

export function EducationScreen() {
  const navigate = useNavigate()
  const location = useLocation()
  const [showPin, setShowPin] = useState(false)
  const [pin, setPin] = useState('')
  const [pinError, setPinError] = useState('')
  const [pending, setPending] = useState<FormData | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const initialService = (location.state as { serviceId?: string } | null)?.serviceId ?? 'jamb'

  const { register, handleSubmit, setValue, watch, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { serviceId: initialService, billersCode: '', variationCode: '', phone: '' },
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
      const res = await paymentsService.education(pending)
      queryClient.invalidateQueries({ queryKey: ['wallet'] })
      queryClient.invalidateQueries({ queryKey: ['transactions'] })
      navigate('/payments/receipt', {
        state: {
          type: 'education', serviceId, exam: chosen?.name, amount: parseFloat(chosen?.variationAmount ?? '0'),
          pin: res.Pin ?? res.purchased_code, requestId: res.requestId, date: res.transaction_date,
        },
      })
    } catch {
      setPinError('Incorrect PIN or purchase failed.')
      setPin(''); setSubmitting(false)
    }
  }

  return (
    <div className="flex flex-col h-[100dvh] bg-[#F2F2F7]">
      <PageHeader title="Education" onBack="default" />
      <div className="flex-1 overflow-y-auto no-scrollbar px-4 pt-4 pb-8 animate-slide-up">

        {/* Service selector */}
        <div className="grid grid-cols-3 gap-2 mb-5">
          {SERVICES.map(s => (
            <button key={s.id} onClick={() => { setValue('serviceId', s.id); setValue('variationCode', '') }}
              className="py-3 bg-white rounded-[14px] border-2 text-[13px] font-bold text-gray-700 transition-all pressable"
              style={{ borderColor: serviceId === s.id ? 'var(--brand-primary)' : 'transparent' }}>
              {s.label}
            </button>
          ))}
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <Input label="Profile / Registration Number" type="text" inputMode="numeric"
            placeholder={serviceId === 'jamb' ? 'JAMB Profile ID' : 'Exam Number'}
            error={errors.billersCode?.message} {...register('billersCode')} />
          <Input label="Phone Number" type="tel" inputMode="tel" placeholder="08012345678"
            error={errors.phone?.message} {...register('phone')} />

          <div>
            <p className="text-[13px] font-semibold text-gray-500 uppercase tracking-wide mb-2">Select Package</p>
            {varsLoading ? <Spinner /> : (
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
