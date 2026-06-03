'use client'

import { useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { AuthProvider, useAuth } from '@/lib/auth-context'

function Guard({ children }: { children: React.ReactNode }) {
  const { user, isLoading } = useAuth()
  const router = useRouter()

  useEffect(() => {
    if (!isLoading && !user) {
      router.replace('/consumer/auth/login')
    }
  }, [user, isLoading, router])

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <span className="h-8 w-8 rounded-full border-4 border-green-500 border-t-transparent animate-spin" />
      </div>
    )
  }
  if (!user) return null
  return <>{children}</>
}

export default function ConsumerLayout({ children }: { children: React.ReactNode }) {
  return (
    <AuthProvider>
      <Guard>
        <main className="min-h-screen bg-gray-50">{children}</main>
      </Guard>
    </AuthProvider>
  )
}
