import { lazy, Suspense, useEffect } from 'react'
import { createBrowserRouter, RouterProvider, Navigate, Outlet } from 'react-router-dom'
import { usePortalAuthStore } from '@/store/auth.store'
import { PortalShell } from '@/components/layout/PortalShell'

// Public pages
const LandingPage  = lazy(() => import('@/modules/public/LandingPage').then(m => ({ default: m.LandingPage })))
const RegisterPage = lazy(() => import('@/modules/public/RegisterPage').then(m => ({ default: m.RegisterPage })))

// Portal pages (authenticated)
const OverviewScreen    = lazy(() => import('@/modules/overview/OverviewScreen').then(m => ({ default: m.OverviewScreen })))
const ApiKeysScreen     = lazy(() => import('@/modules/api-keys/ApiKeysScreen').then(m => ({ default: m.ApiKeysScreen })))
const WebhooksScreen    = lazy(() => import('@/modules/webhooks/WebhooksScreen').then(m => ({ default: m.WebhooksScreen })))
const IpWhitelistScreen = lazy(() => import('@/modules/ip-whitelist/IpWhitelistScreen').then(m => ({ default: m.IpWhitelistScreen })))
const IntegrationGuide  = lazy(() => import('@/modules/integration/IntegrationGuide').then(m => ({ default: m.IntegrationGuide })))
const BrandingScreen    = lazy(() => import('@/modules/branding/BrandingScreen').then(m => ({ default: m.BrandingScreen })))
const KybScreen         = lazy(() => import('@/modules/kyb/KybScreen').then(m => ({ default: m.KybScreen })))
const SettlementScreen  = lazy(() => import('@/modules/settlement/SettlementScreen').then(m => ({ default: m.SettlementScreen })))
const GoLiveScreen      = lazy(() => import('@/modules/go-live/GoLiveScreen').then(m => ({ default: m.GoLiveScreen })))

// ─── Route guard ─────────────────────────────────────────────────────────────

function RequireAuth() {
  const { isAuthenticated, isInitialised } = usePortalAuthStore()
  if (!isInitialised) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="text-slate-400 text-sm">Loading…</div>
      </div>
    )
  }
  if (!isAuthenticated) {
    return <Navigate to="/" replace />
  }
  return <Outlet />
}

// ─── Router ──────────────────────────────────────────────────────────────────

const router = createBrowserRouter([
  // Public routes
  { path: '/',         element: <LandingPage /> },
  { path: '/register', element: <RegisterPage /> },

  // Protected routes (inside PortalShell)
  {
    element: <RequireAuth />,
    children: [
      {
        element: <PortalShell />,
        children: [
          { path: '/dashboard',    element: <OverviewScreen /> },
          { path: '/api-keys',     element: <ApiKeysScreen /> },
          { path: '/webhooks',     element: <WebhooksScreen /> },
          { path: '/ip-whitelist', element: <IpWhitelistScreen /> },
          { path: '/integration',  element: <IntegrationGuide /> },
          { path: '/branding',     element: <BrandingScreen /> },
          { path: '/kyb',          element: <KybScreen /> },
          { path: '/settlement',   element: <SettlementScreen /> },
          { path: '/go-live',      element: <GoLiveScreen /> },
          // Catch-all inside shell
          { path: '*',             element: <Navigate to="/dashboard" replace /> },
        ],
      },
    ],
  },
])

// ─── App root ─────────────────────────────────────────────────────────────────

const PageLoader = () => (
  <div className="flex items-center justify-center h-screen">
    <div className="w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full animate-spin" />
  </div>
)

export default function App() {
  const init = usePortalAuthStore(s => s.init)
  useEffect(() => { init() }, [init])
  return (
    <Suspense fallback={<PageLoader />}>
      <RouterProvider router={router} />
    </Suspense>
  )
}
