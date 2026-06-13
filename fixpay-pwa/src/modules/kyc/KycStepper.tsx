import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { motion, AnimatePresence } from 'motion/react'
import { CheckCircleIcon } from '@heroicons/react/24/solid'
import { api } from '@/lib/api'
import { useAuthStore } from '@/store/auth.store'
import { PageHeader } from '@/components/layout/PageHeader'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'
import { cn } from '@/lib/utils'

type Step = 0 | 1 | 2 // NIN, BVN, Selfie
const STEPS = ['NIN', 'BVN', 'Selfie'] as const

const ninSchema = z.object({ nin: z.string().length(11, 'NIN must be exactly 11 digits').regex(/^\d+$/, 'Digits only') })
const bvnSchema = z.object({
  bvn: z.string().length(11, 'BVN must be exactly 11 digits').regex(/^\d+$/, 'Digits only'),
  dob: z.string().min(1, 'Date of birth is required')
})

function NinStep({ onDone }: { onDone: () => void }) {
  const [err, setErr] = useState('')
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<{ nin: string }>({ resolver: zodResolver(ninSchema) })
  const onSubmit = async (data: { nin: string }) => {
    setErr('')
    try { await api.post('/kyc/nin', data); onDone() }
    catch { setErr('Could not verify NIN. Check the number and retry.') }
  }
  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-5">
      <div className="text-center mb-2">
        <p className="text-[28px]">🪪</p>
        <h2 className="text-[20px] font-bold text-gray-900 mt-2">National Identity Number</h2>
        <p className="text-[14px] text-gray-500 mt-1">Enter your 11-digit NIN for identity verification.</p>
      </div>
      <Input label="NIN" type="tel" inputMode="numeric" maxLength={11} placeholder="12345678901"
        error={errors.nin?.message} {...register('nin')} />
      {err && <p className="text-ios-red text-[13px] text-center">{err}</p>}
      <Button type="submit" fullWidth loading={isSubmitting}>Verify NIN</Button>
      <p className="text-[12px] text-center text-gray-400">Demo: use any 11-digit number</p>
    </form>
  )
}

function BvnStep({ onDone }: { onDone: () => void }) {
  const [err, setErr] = useState('')
  const [awaiting, setAwaiting] = useState(false)
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<{ bvn: string; dob: string }>({ resolver: zodResolver(bvnSchema) })
  
  const startPolling = async (attempt = 0) => {
    // Delays: 10s, 4m, 10m, 20m, 25m (Total ~60m)
    const delays = [10000, 240000, 600000, 1200000, 1500000]
    if (attempt >= delays.length) {
      setErr('BVN verification timed out. Please try again.')
      setAwaiting(false)
      return
    }
    
    setTimeout(async () => {
      try {
        const res = await api.get('/kyc/status')
        const bvnStatus = res.data.verifications?.find((v: any) => v.type === 'BVN_CONSENT' || v.type === 'BVN')?.status
        if (bvnStatus === 'VERIFIED') {
          onDone()
        } else if (bvnStatus === 'FAILED') {
          setErr('BVN verification failed at NIBSS.')
          setAwaiting(false)
        } else {
          startPolling(attempt + 1)
        }
      } catch (e) {
        startPolling(attempt + 1)
      }
    }, delays[attempt])
  }

  const onSubmit = async (data: { bvn: string; dob: string }) => {
    setErr('')
    try { 
      const res = await api.post('/kyc/bvn/consent/initiate', data) 
      if (res.data.status === 'PENDING') {
        setAwaiting(true)
        if (res.data.consentUrl) {
          window.open(res.data.consentUrl, '_blank')
        }
        startPolling(0)
      } else if (res.data.status === 'VERIFIED') {
        onDone()
      }
    }
    catch { setErr('Could not verify BVN. Check the number and retry.') }
  }

  if (awaiting) {
    return (
      <div className="flex flex-col items-center gap-5 pt-8">
        <p className="text-[28px]">⏳</p>
        <h2 className="text-[20px] font-bold text-gray-900 mt-2">Awaiting NIBSS Response</h2>
        <p className="text-[14px] text-gray-500 mt-1 text-center px-4">
          BVN validation is ongoing with NIBSS. You will be informed as soon as details are received.
        </p>
        <p className="text-[12px] text-gray-400 mt-4 text-center px-4">
          Please complete the authentication on the NIBSS portal that opened in a new tab.
        </p>
        <Button variant="outline" onClick={() => setAwaiting(false)} className="mt-4">Cancel &amp; Retry</Button>
      </div>
    )
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-5">
      <div className="text-center mb-2">
        <p className="text-[28px]">🏦</p>
        <h2 className="text-[20px] font-bold text-gray-900 mt-2">Bank Verification Number</h2>
        <p className="text-[14px] text-gray-500 mt-1">Enter your 11-digit BVN linked to your bank account.</p>
      </div>
      <Input label="Date of Birth" type="date"
        error={errors.dob?.message} {...register('dob')} />
      <Input label="BVN" type="tel" inputMode="numeric" maxLength={11} placeholder="00000000000"
        error={errors.bvn?.message} {...register('bvn')} />
      {err && <p className="text-ios-red text-[13px] text-center">{err}</p>}
      <Button type="submit" fullWidth loading={isSubmitting}>Verify BVN</Button>
      <p className="text-[12px] text-center text-gray-400">Demo: use any 11-digit number</p>
    </form>
  )
}

function SelfieStep({ onDone, loading }: { onDone: () => void; loading: boolean }) {
  return (
    <div className="flex flex-col items-center gap-5">
      <div className="text-center">
        <p className="text-[28px]">🤳</p>
        <h2 className="text-[20px] font-bold text-gray-900 mt-2">Selfie Verification</h2>
        <p className="text-[14px] text-gray-500 mt-1">Take a quick selfie to complete your identity check.</p>
      </div>
      <div className="w-40 h-40 rounded-full bg-gray-100 border-4 border-dashed border-gray-200 flex items-center justify-center">
        <span className="text-[60px]">📸</span>
      </div>
      <p className="text-[13px] text-gray-400 text-center">(Simulated in demo — tap button to proceed)</p>
      <Button fullWidth loading={loading} onClick={onDone}>Take Selfie &amp; Continue</Button>
    </div>
  )
}

export function KycStepper() {
  const navigate = useNavigate()
  const { setKycCompleted } = useAuthStore()
  const [step, setStep] = useState<Step>(0)
  const [selfieLoading, setSelfieLoading] = useState(false)
  const [done, setDone] = useState(false)

  const handleSelfie = async () => {
    setSelfieLoading(true)
    try {
      // await api.post('/kyc/selfie', {})
      await new Promise(r => setTimeout(r, 800)) // simulate network delay
      setDone(true)
      setKycCompleted(true)
      setTimeout(() => navigate('/home', { replace: true }), 1800)
    } catch { /* ignore */ }
    finally { setSelfieLoading(false) }
  }

  if (done) return (
    <div className="h-[100dvh] flex flex-col items-center justify-center gap-4 animate-scale-in">
      <CheckCircleIcon className="w-20 h-20 text-ios-green" />
      <h2 className="text-[24px] font-bold text-gray-900">KYC Complete!</h2>
      <p className="text-gray-500">Redirecting to your dashboard…</p>
    </div>
  )

  return (
    <div className="flex flex-col h-[100dvh] bg-[#F2F2F7]">
      <PageHeader title="Identity Verification" />

      {/* Progress bar */}
      <div className="flex gap-2 px-4 pb-4 shrink-0">
        {STEPS.map((s, i) => (
          <div key={s} className="flex-1 flex flex-col items-center gap-1">
            <div className={cn('h-1 w-full rounded-full transition-all duration-500', i <= step ? 'opacity-100' : 'bg-gray-200')}
              style={i <= step ? { background: 'var(--brand-primary)' } : undefined} />
            <span className={cn('text-[11px] font-medium', i <= step ? 'text-brand' : 'text-gray-400')}
              style={i <= step ? { color: 'var(--brand-primary)' } : undefined}>{s}</span>
          </div>
        ))}
      </div>

      <div className="flex-1 overflow-y-auto no-scrollbar px-4 pb-8">
        <AnimatePresence mode="wait">
          <motion.div key={step} initial={{ opacity: 0, x: 40 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -40 }}
            transition={{ duration: 0.25 }}>
            {step === 0 && <NinStep onDone={() => setStep(1)} />}
            {step === 1 && <BvnStep onDone={() => setStep(2)} />}
            {step === 2 && <SelfieStep onDone={handleSelfie} loading={selfieLoading} />}
          </motion.div>
        </AnimatePresence>
      </div>
    </div>
  )
}
