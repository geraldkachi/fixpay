import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '@/lib/api'
import { useAuthStore } from '@/store/auth.store'
import { PageHeader } from '@/components/layout/PageHeader'
import { OTPInput } from '@/components/ui/OTPInput'
import { Button } from '@/components/ui/Button'
import type { User } from '@/types'

export function OtpScreen() {
  const navigate = useNavigate()
  const { pendingPhone, pendingEmail, setToken, setUser, pinCreated, kycCompleted } = useAuthStore()
  const [otp, setOtp] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const identifier = pendingPhone ?? pendingEmail ?? 'your number'

  const handleVerify = async () => {
    if (otp.length < 6) { setError('Enter the 6-digit code'); return }
    setError(''); setLoading(true)
    try {
      const res = await api.post<{ accessToken: string; user: User }>('/auth/verify-otp', { otp, identifier: pendingPhone ?? pendingEmail })
      setToken(res.data.accessToken)
      setUser(res.data.user)
      if (!pinCreated) navigate('/auth/pin', { replace: true })
      else if (!kycCompleted) navigate('/kyc', { replace: true })
      else navigate('/home', { replace: true })
    } catch {
      setError('Incorrect OTP. Try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex flex-col h-[100dvh] bg-[#F2F2F7]">
      <PageHeader title="Verify OTP" onBack="default" />
      <div className="flex-1 flex flex-col items-center px-6 pt-8 gap-6 animate-slide-up">
        <p className="text-[15px] text-gray-500 text-center">
          Enter the 6-digit code sent to <strong className="text-gray-900">{identifier}</strong>
        </p>
        <p className="text-[13px] bg-blue-50 text-blue-700 rounded-[10px] px-3 py-2 text-center">
          Demo hint: use <strong>123456</strong>
        </p>
        <OTPInput length={6} value={otp} onChange={setOtp} autoFocus />
        {error && <p className="text-[14px] text-ios-red text-center">{error}</p>}
        <Button fullWidth loading={loading} onClick={handleVerify}>Verify</Button>
        <button className="text-[14px]" style={{ color: 'var(--brand-primary)' }}>Resend code</button>
      </div>
    </div>
  )
}
