import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { api } from '@/lib/api'
import { useAuthStore } from '@/store/auth.store'
import { PageHeader } from '@/components/layout/PageHeader'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'

const schema = z.discriminatedUnion('mode', [
  z.object({ mode: z.literal('phone'), identifier: z.string().regex(/^0[789]\d{9}$/, 'Enter a valid 11-digit phone number'), password: z.string().min(6, 'At least 6 characters') }),
  z.object({ mode: z.literal('email'), identifier: z.string().email('Enter a valid email'), password: z.string().min(6, 'At least 6 characters') }),
])
type FormData = z.infer<typeof schema>

export function RegisterScreen() {
  const navigate = useNavigate()
  const { setPending } = useAuthStore()
  const [mode, setMode] = useState<'phone' | 'email'>('phone')
  const [serverError, setServerError] = useState('')

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { mode: 'phone', identifier: '', password: '' },
  })

  const onSubmit = async (data: FormData) => {
    setServerError('')
    try {
      await api.post('/auth/register', { [mode]: data.identifier, password: data.password })
      setPending(mode === 'phone' ? data.identifier : undefined, mode === 'email' ? data.identifier : undefined)
      navigate('/auth/otp')
    } catch {
      setServerError('Registration failed. Try again.')
    }
  }

  return (
    <div className="flex flex-col h-[100dvh] bg-[#F2F2F7]">
      <PageHeader title="Create Account" onBack="default" />
      <div className="flex-1 overflow-y-auto no-scrollbar px-4 pt-4 pb-8 animate-slide-up">
        <p className="text-[15px] text-gray-500 mb-6">Join FixPay to send money and pay bills.</p>

        {/* Mode toggle */}
        <div className="flex bg-white rounded-[12px] p-1 gap-1 mb-6 shadow-sm">
          {(['phone', 'email'] as const).map(m => (
            <button key={m} onClick={() => setMode(m)}
              className="flex-1 py-2 rounded-[10px] text-[14px] font-semibold transition-all duration-200 pressable"
              style={mode === m ? { background: 'var(--brand-primary)', color: 'white' } : { color: '#8E8E93' }}>
              {m === 'phone' ? 'Phone Number' : 'Email'}
            </button>
          ))}
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <input type="hidden" {...register('mode')} value={mode} />
          <Input label={mode === 'phone' ? 'Phone Number' : 'Email Address'} type={mode === 'phone' ? 'tel' : 'email'}
            placeholder={mode === 'phone' ? '08012345678' : 'you@example.com'}
            error={errors.identifier?.message} {...register('identifier')} />
          <Input label="Password" type="password" placeholder="At least 6 characters"
            error={errors.password?.message} {...register('password')} />
          {serverError && <p className="text-[14px] text-ios-red text-center">{serverError}</p>}
          <Button type="submit" fullWidth loading={isSubmitting} className="mt-2">Continue</Button>
        </form>

        <p className="text-center text-[13px] text-gray-400 mt-6">
          Already have an account?{' '}
          <button className="font-semibold" style={{ color: 'var(--brand-primary)' }} onClick={() => navigate('/auth/login')}>Sign In</button>
        </p>
      </div>
    </div>
  )
}
