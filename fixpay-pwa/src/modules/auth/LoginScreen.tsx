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
import type { User } from '@/types'

const schema = z.object({
  identifier: z.string().min(5, 'Enter phone or email'),
  password:   z.string().min(4, 'Enter your password'),
})
type FormData = z.infer<typeof schema>

export function LoginScreen() {
  const navigate = useNavigate()
  const { setToken, setUser } = useAuthStore()
  const [serverError, setServerError] = useState('')

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { identifier: '08012345678', password: 'password123' },
  })

  const onSubmit = async (data: FormData) => {
    setServerError('')
    try {
      const isPhone = /^0[789]\d{9}$/.test(data.identifier)
      const res = await api.post<{ accessToken: string; user: User }>('/auth/login', {
        [isPhone ? 'phone' : 'email']: data.identifier,
        password: data.password,
      })
      setToken(res.data.accessToken)
      setUser(res.data.user)
      const u = res.data.user
      if (!u.kycStatus || u.kycStatus === 'pending') navigate('/kyc', { replace: true })
      else navigate('/home', { replace: true })
    } catch {
      setServerError('Invalid credentials. Try again.')
    }
  }

  return (
    <div className="flex flex-col h-[100dvh] bg-[#F2F2F7]">
      <PageHeader title="Sign In" onBack="default" />
      <div className="flex-1 overflow-y-auto no-scrollbar px-4 pt-4 pb-8 animate-slide-up">
        <p className="text-[15px] text-gray-500 mb-6">Enter your phone or email to continue.</p>
        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <Input label="Phone or Email" type="text" placeholder="08012345678"
            error={errors.identifier?.message} {...register('identifier')} />
          <Input label="Password" type="password" placeholder="Your password"
            error={errors.password?.message} {...register('password')} />
          {serverError && <p className="text-[14px] text-ios-red text-center">{serverError}</p>}
          <Button type="submit" fullWidth loading={isSubmitting} className="mt-2">Sign In</Button>
        </form>

        <p className="text-center text-[13px] text-gray-400 mt-6">
          Don&apos;t have an account?{' '}
          <button className="font-semibold" style={{ color: 'var(--brand-primary)' }} onClick={() => navigate('/auth/register')}>Create One</button>
        </p>
      </div>
    </div>
  )
}
