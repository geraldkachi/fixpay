'use client'

import { useState, Suspense } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { authService } from '@/lib/services'

function VerifyForm() {
  const router = useRouter()
  const params = useSearchParams()
  const identifier = params.get('identifier') ?? ''
  const purpose = params.get('purpose') ?? 'EMAIL_VERIFICATION'

  const [code, setCode] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [resent, setResent] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await authService.verifyOtp({ identifier, purpose, code })
      router.push('/consumer/auth/login')
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Invalid or expired code')
    } finally {
      setLoading(false)
    }
  }

  async function handleResend() {
    setResent(false)
    try {
      await authService.resendOtp({ identifier, purpose })
      setResent(true)
    } catch {
      setError('Could not resend code — try again shortly')
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 px-4">
      <div className="w-full max-w-sm rounded-2xl bg-white p-8 shadow">
        <h1 className="mb-2 text-2xl font-bold text-gray-900">Verify your account</h1>
        <p className="mb-6 text-sm text-gray-500">
          Enter the code sent to <span className="font-medium text-gray-700">{identifier}</span>
        </p>
        <form onSubmit={handleSubmit} className="space-y-4">
          <input
            type="text"
            required
            inputMode="numeric"
            maxLength={6}
            placeholder="6-digit code"
            value={code}
            onChange={e => setCode(e.target.value.replace(/\D/g, ''))}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-center text-xl tracking-widest focus:outline-none focus:ring-2 focus:ring-green-500"
          />
          {error && <p className="text-sm text-red-600">{error}</p>}
          {resent && <p className="text-sm text-green-600">Code resent! Check your email.</p>}
          <button
            type="submit"
            disabled={loading || code.length < 4}
            className="w-full rounded-lg bg-green-600 py-2 text-sm font-semibold text-white hover:bg-green-700 disabled:opacity-50"
          >
            {loading ? 'Verifying…' : 'Verify'}
          </button>
        </form>
        <button
          onClick={handleResend}
          className="mt-3 w-full text-center text-sm text-green-600 hover:underline"
        >
          Resend code
        </button>
      </div>
    </div>
  )
}

export default function VerifyOtpPage() {
  return (
    <Suspense>
      <VerifyForm />
    </Suspense>
  )
}
