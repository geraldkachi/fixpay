import React from 'react'
import ReactDOM from 'react-dom/client'
import { QueryClientProvider } from '@tanstack/react-query'
import { queryClient } from '@/lib/query-client'
import App from './App'
import './index.css'

// Reset the splash guard on every fresh page load so the splash always plays.
// sessionStorage is cleared on tab-close / reload, but we do it explicitly here
// so even reloads within the same tab show the splash.
sessionStorage.removeItem('splash_shown')

// ── One-time migration ────────────────────────────────────────────────────
// If the user already has a verified account in localStorage but kycCompleted
// is false (stale state from before the login fix), auto-correct it so they
// don't get stuck in the KYC flow on every app start.
try {
  const raw = localStorage.getItem('fixpay-auth')
  if (raw) {
    const stored = JSON.parse(raw)
    const state = stored?.state
    if (state?.isAuthenticated && state?.user?.kycStatus === 'VERIFIED') {
      let dirty = false
      if (!state.kycCompleted) { state.kycCompleted = true; dirty = true }
      // pin_hash cannot be inferred here without a server round-trip, but
      // if the user landed on Home successfully before, pinCreated must be true.
      if (state.isAuthenticated && !state.pinCreated) { state.pinCreated = true; dirty = true }
      if (dirty) localStorage.setItem('fixpay-auth', JSON.stringify(stored))
    }
  }
} catch { /* ignore — corrupt storage will be wiped on next login */ }


async function prepare() {
  if (import.meta.env.DEV && 'serviceWorker' in navigator) {
    // Remove any stale Workbox/VitePWA service workers so they don't block MSW.
    // devOptions: { enabled: false } stops Vite from serving the Workbox SW, but
    // previously-registered SWs keep controlling the page until explicitly removed.
    const regs = await navigator.serviceWorker.getRegistrations()
    const stale = regs.filter(r => {
      const url = r.active?.scriptURL ?? r.installing?.scriptURL ?? r.waiting?.scriptURL ?? ''
      return !url.includes('mockServiceWorker')
    })
    if (stale.length > 0) {
      await Promise.all(stale.map(r => r.unregister()))
      window.location.reload()
      await new Promise(() => {}) // keep pending — page reloads before React mounts
    }

    const mode = localStorage.getItem('fixpay_dev_mode') ?? 'mock'
    if (mode === 'mock') {
      const { worker } = await import('./mocks/browser')
      await worker.start({ onUnhandledRequest: 'bypass' })
    }
  }
}

prepare().then(() => {
  ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
      <QueryClientProvider client={queryClient}>
        <App />
      </QueryClientProvider>
    </React.StrictMode>
  )
})
