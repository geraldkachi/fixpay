'use client'

import { useEffect } from 'react'
import Link from 'next/link'
import { useRouter, usePathname } from 'next/navigation'
import { PortalAuthProvider, usePortalAuth } from '@/lib/portal-auth-context'

const NAV = [
  { href: '/portal/dashboard', label: 'Dashboard' },
  { href: '/portal/api-keys', label: 'API Keys' },
  { href: '/portal/kyb', label: 'KYB' },
  { href: '/portal/webhooks', label: 'Webhooks' },
  { href: '/portal/settlement', label: 'Settlement' },
]

function PortalShell({ children }: { children: React.ReactNode }) {
  const { tenant, isLoading } = usePortalAuth()
  const router = useRouter()
  const pathname = usePathname()
  const isAuthRoute = pathname.startsWith('/portal/auth')

  useEffect(() => {
    if (!isLoading && !tenant && !isAuthRoute) router.replace('/portal/auth/login')
  }, [tenant, isLoading, router, isAuthRoute])

  if (isLoading && !isAuthRoute) return (
    <div className="flex h-screen items-center justify-center">
      <span className="h-8 w-8 rounded-full border-4 border-blue-500 border-t-transparent animate-spin" />
    </div>
  )
  if (!tenant && !isAuthRoute) return null

  if (isAuthRoute) {
    return <main className="min-h-screen bg-gray-50">{children}</main>
  }

  const currentTenant = tenant

  if (!currentTenant) return null

  return (
    <div className="flex min-h-screen">
      <aside className="w-56 shrink-0 border-r bg-white py-6">
        <div className="px-4 mb-6">
          <span className="text-lg font-bold text-blue-600">FixPay Portal</span>
          <p className="mt-1 text-xs text-gray-500 truncate">{currentTenant.name}</p>
          <span className={`mt-1 inline-block rounded px-2 py-0.5 text-xs font-medium ${
            currentTenant.status === 'ACTIVE' ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'
          }`}>{currentTenant.status}</span>
        </div>
        <nav className="space-y-1 px-3">
          {NAV.map(n => (
            <Link
              key={n.href}
              href={n.href}
              className={`block rounded-lg px-3 py-2 text-sm font-medium ${
                pathname === n.href ? 'bg-blue-50 text-blue-700' : 'text-gray-700 hover:bg-gray-50'
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

export default function PortalLayout({ children }: { children: React.ReactNode }) {
  return (
    <PortalAuthProvider>
      <PortalShell>{children}</PortalShell>
    </PortalAuthProvider>
  )
}
