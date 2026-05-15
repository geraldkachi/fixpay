import { Link } from 'react-router-dom'

const tenantPortalUrl = import.meta.env.VITE_TENANT_PORTAL_URL ?? 'http://localhost:3001'
const adminPortalUrl = import.meta.env.VITE_ADMIN_PORTAL_URL ?? 'http://localhost:5174'

export function PortalLaunchpadScreen() {
  return (
    <div className="min-h-dvh bg-gradient-to-b from-slate-950 via-slate-900 to-slate-800 px-5 py-7 text-white">
      <div className="mx-auto max-w-md">
        <p className="inline-flex rounded-full border border-emerald-400/30 bg-emerald-500/10 px-3 py-1 text-xs font-medium text-emerald-200">
          Development Mode
        </p>

        <h1 className="mt-5 text-3xl font-semibold tracking-tight">FixPay Launchpad</h1>
        <p className="mt-2 text-sm text-slate-300">
          Select which portal to open for local testing.
        </p>

        <div className="mt-7 space-y-3">
          <Link
            to="/splash"
            className="block rounded-2xl border border-blue-400/25 bg-blue-500/15 p-4 transition hover:bg-blue-500/20"
          >
            <div className="text-base font-semibold">Customer PWA</div>
            <div className="mt-1 text-xs text-blue-100/80">Continue to mobile customer app</div>
          </Link>

          <a
            href={tenantPortalUrl}
            className="block rounded-2xl border border-cyan-400/25 bg-cyan-500/15 p-4 transition hover:bg-cyan-500/20"
          >
            <div className="text-base font-semibold">Tenant Developer Portal</div>
            <div className="mt-1 text-xs text-cyan-100/80">Open tenant onboarding and integrations</div>
          </a>

          <a
            href={adminPortalUrl}
            className="block rounded-2xl border border-violet-400/25 bg-violet-500/15 p-4 transition hover:bg-violet-500/20"
          >
            <div className="text-base font-semibold">Platform Admin Portal</div>
            <div className="mt-1 text-xs text-violet-100/80">Open ops and approval dashboard</div>
          </a>
        </div>

        <p className="mt-8 text-xs text-slate-400">
          You can override URLs with Vite env vars: VITE_TENANT_PORTAL_URL and VITE_ADMIN_PORTAL_URL.
        </p>
      </div>
    </div>
  )
}
