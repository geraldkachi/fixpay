'use client'

import { useEffect } from 'react'
import { usePathname, useRouter } from 'next/navigation'
import { AuthProvider, useAuth } from '@/lib/auth-context'

function Guard({ children }: { children: React.ReactNode }) {
  const { user, isLoading } = useAuth()
  const router = useRouter()
  const pathname = usePathname()
  const isAuthRoute = pathname.startsWith('/consumer/auth')

  useEffect(() => {
    if (!isLoading && !user && !isAuthRoute) {
      router.replace('/consumer/auth/login')
    }
  }, [user, isLoading, router, isAuthRoute])

  if (isLoading && !isAuthRoute) {
    return (
      <div className="flex h-screen items-center justify-center">
        <span className="h-8 w-8 rounded-full border-4 border-green-500 border-t-transparent animate-spin" />
      </div>
    )
  }
  if (!user && !isAuthRoute) return null
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
