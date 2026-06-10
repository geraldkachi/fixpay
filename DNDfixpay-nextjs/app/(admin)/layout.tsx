'use client'

import { useEffect } from 'react'
import Link from 'next/link'
import { useRouter, usePathname } from 'next/navigation'
import { AdminAuthProvider, useAdminAuth } from '@/lib/admin-auth-context'

const NAV = [
  { href: '/admin/tenants', label: 'Tenants' },
  { href: '/admin/payment-rails', label: 'Payment Rails' },
  { href: '/admin/disputes', label: 'Disputes' },
  { href: '/admin/compliance', label: 'Compliance' },
]

function AdminShell({ children }: { children: React.ReactNode }) {
  const { admin, isLoading } = useAdminAuth()
  const router = useRouter()
  const pathname = usePathname()
  const isAuthRoute = pathname.startsWith('/admin/auth')

  useEffect(() => {
    if (!isLoading && !admin && !isAuthRoute) router.replace('/admin/auth/login')
  }, [admin, isLoading, router, isAuthRoute])

  if (isLoading && !isAuthRoute) return (
    <div className="flex h-screen items-center justify-center">
      <span className="h-8 w-8 rounded-full border-4 border-gray-800 border-t-transparent animate-spin" />
    </div>
  )
  if (!admin && !isAuthRoute) return null

  if (isAuthRoute) {
    return <main className="min-h-screen bg-gray-100">{children}</main>
  }

  const currentAdmin = admin

  if (!currentAdmin) return null

  return (
    <div className="flex min-h-screen">
      <aside className="w-56 shrink-0 border-r bg-gray-900 py-6 text-white">
        <div className="px-4 mb-6">
          <span className="text-lg font-bold">FixPay Admin</span>
          <p className="mt-1 text-xs text-gray-400">{currentAdmin.email}</p>
        </div>
        <nav className="space-y-1 px-3">
          {NAV.map(n => (
            <Link
              key={n.href}
              href={n.href}
              className={`block rounded-lg px-3 py-2 text-sm font-medium ${
                pathname.startsWith(n.href) ? 'bg-gray-700 text-white' : 'text-gray-300 hover:bg-gray-800'
              }`}
            >
              {n.label}
            </Link>
          ))}
        </nav>
      </aside>
      <main className="flex-1 bg-gray-50 p-8">{children}</main>
    </div>
  )
}

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return (
    <AdminAuthProvider>
      <AdminShell>{children}</AdminShell>
    </AdminAuthProvider>
  )
}
