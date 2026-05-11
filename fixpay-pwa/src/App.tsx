import { createBrowserRouter, RouterProvider, Navigate, Outlet, useLocation } from 'react-router-dom'
import { useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { useTenantStore } from '@/store/tenant.store'
import { useAuthStore } from '@/store/auth.store'
import type { TenantConfig } from '@/types'

// Layout
import { AppShell } from '@/components/layout/AppShell'

// Auth
import { SplashScreen }      from '@/modules/auth/SplashScreen'
import { WelcomeScreen }     from '@/modules/auth/WelcomeScreen'
import { RegisterScreen }    from '@/modules/auth/RegisterScreen'
import { LoginScreen }       from '@/modules/auth/LoginScreen'
import { OtpScreen }         from '@/modules/auth/OtpScreen'
import { CreatePinScreen }   from '@/modules/auth/CreatePinScreen'

// KYC
import { KycStepper }        from '@/modules/kyc/KycStepper'

// Home
import { HomeScreen }        from '@/modules/home/HomeScreen'

// Payments
import { PaymentsScreen }    from '@/modules/payments/PaymentsScreen'
import { AirtimeScreen }     from '@/modules/payments/AirtimeScreen'
import { DataScreen }        from '@/modules/payments/DataScreen'
import { TvScreen }          from '@/modules/payments/TvScreen'
import { ElectricityScreen } from '@/modules/payments/ElectricityScreen'
import { EducationScreen }   from '@/modules/payments/EducationScreen'
import { ReceiptScreen }     from '@/modules/payments/ReceiptScreen'

// Send
import { SendScreen }        from '@/modules/send/SendScreen'

// Wallet
import { WalletScreen }      from '@/modules/wallet/WalletScreen'
import { FundWalletScreen }  from '@/modules/wallet/FundWalletScreen'

// More
import { MoreScreen }           from '@/modules/more/MoreScreen'
import { ProfileScreen }        from '@/modules/more/ProfileScreen'
import { MandatesScreen }       from '@/modules/more/MandatesScreen'
import { DisputesScreen }       from '@/modules/more/DisputesScreen'
import { DisputeDetailScreen }  from '@/modules/more/DisputeDetailScreen'

// ─── Guards ────────────────────────────────────────────────────────────────

function RequireAuth() {
  const { isAuthenticated, kycCompleted, pinCreated } = useAuthStore()
  const { pathname } = useLocation()
  if (!isAuthenticated)                        return <Navigate to="/auth/login" replace />
  if (!pinCreated)                             return <Navigate to="/auth/pin"   replace />
  if (!kycCompleted && pathname !== '/kyc')    return <Navigate to="/kyc"        replace />
  return <Outlet />
}

// ─── Tenant theme applier ──────────────────────────────────────────────────

function TenantLoader() {
  const { setConfig } = useTenantStore()
  const { data } = useQuery<TenantConfig>({
    queryKey: ['tenant-config'],
    queryFn: () => api.get<TenantConfig>('/tenant/config').then(r => r.data),
    staleTime: Infinity,
  })
  useEffect(() => { if (data) setConfig(data) }, [data, setConfig])
  return null
}

// ─── Router ───────────────────────────────────────────────────────────────

const router = createBrowserRouter([
  { path: '/splash',         element: <SplashScreen /> },
  { path: '/welcome',        element: <WelcomeScreen /> },
  { path: '/auth/register',  element: <RegisterScreen /> },
  { path: '/auth/login',     element: <LoginScreen /> },
  { path: '/auth/otp',       element: <OtpScreen /> },
  { path: '/auth/pin',       element: <CreatePinScreen /> },
  {
    path: '/',
    element: <RequireAuth />,
    children: [
      // Protected full-screen (no bottom nav)
      { path: 'kyc',                   element: <KycStepper /> },
      { path: 'payments/airtime',      element: <AirtimeScreen /> },
      { path: 'payments/data',         element: <DataScreen /> },
      { path: 'payments/tv',           element: <TvScreen /> },
      { path: 'payments/electricity',  element: <ElectricityScreen /> },
      { path: 'payments/education',    element: <EducationScreen /> },
      { path: 'payments/receipt',      element: <ReceiptScreen /> },
      { path: 'wallet/fund',           element: <FundWalletScreen /> },
      { path: 'more/profile',          element: <ProfileScreen /> },
      { path: 'more/mandates',         element: <MandatesScreen /> },
      { path: 'more/disputes',         element: <DisputesScreen /> },
      { path: 'more/disputes/:id',     element: <DisputeDetailScreen /> },
      // Protected with bottom nav (AppShell)
      {
        element: <AppShell />,
        children: [
          { index: true,           element: <Navigate to="/home" replace /> },
          { path: 'home',          element: <HomeScreen /> },
          { path: 'payments',      element: <PaymentsScreen /> },
          { path: 'send',          element: <SendScreen /> },
          { path: 'wallet',        element: <WalletScreen /> },
          { path: 'more',          element: <MoreScreen /> },
        ],
      },
    ],
  },
  { path: '*', element: <Navigate to="/splash" replace /> },
])

// ─── App ──────────────────────────────────────────────────────────────────

export default function App() {
  return (
    <>
      <TenantLoader />
      <RouterProvider router={router} />
    </>
  )
}
