import { NavLink, Outlet } from 'react-router-dom'
import { usePortalAuthStore } from '@/store/auth.store'
import {
  HomeIcon,
  KeyIcon,
  GlobeAltIcon,
  ShieldCheckIcon,
  BookOpenIcon,
  PaintBrushIcon,
  IdentificationIcon,
  BanknotesIcon,
  RocketLaunchIcon,
  ArrowRightStartOnRectangleIcon,
} from '@heroicons/react/24/outline'

const navItems = [
  { to: '/dashboard',    label: 'Overview',         icon: HomeIcon },
  { to: '/api-keys',     label: 'API Keys',          icon: KeyIcon },
  { to: '/webhooks',     label: 'Webhooks',          icon: GlobeAltIcon },
  { to: '/ip-whitelist', label: 'IP Whitelist',      icon: ShieldCheckIcon },
  { to: '/integration',  label: 'Integration Guide', icon: BookOpenIcon },
  { to: '/branding',     label: 'Branding',          icon: PaintBrushIcon },
  { to: '/kyb',          label: 'KYB Verification',  icon: IdentificationIcon },
  { to: '/settlement',   label: 'Settlement',        icon: BanknotesIcon },
  { to: '/go-live',      label: 'Go Live',           icon: RocketLaunchIcon },
]

export function PortalShell() {
  const { username, logout } = usePortalAuthStore()

  return (
    <div className="flex h-screen bg-slate-50">
      {/* Sidebar */}
      <aside className="w-60 bg-white border-r border-slate-200 flex flex-col">
        <div className="px-5 py-4 border-b border-slate-100">
          <span className="text-lg font-bold text-slate-900">FixPay</span>
          <span className="ml-1 text-xs font-medium text-blue-600 bg-blue-50 px-1.5 py-0.5 rounded">Dev Portal</span>
        </div>

        <nav className="flex-1 px-3 py-4 space-y-0.5 overflow-y-auto">
          {navItems.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                `flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm transition-colors ${
                  isActive
                    ? 'bg-blue-50 text-blue-700 font-medium'
                    : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                }`
              }
            >
              <Icon className="h-4 w-4 shrink-0" />
              {label}
            </NavLink>
          ))}
        </nav>

        {/* User footer */}
        <div className="px-3 py-3 border-t border-slate-100">
          <div className="flex items-center gap-2 px-3">
            <div className="h-7 w-7 rounded-full bg-blue-600 flex items-center justify-center text-white text-xs font-semibold">
              {username.slice(0, 2).toUpperCase()}
            </div>
            <span className="text-xs text-slate-600 truncate flex-1">{username}</span>
            <button
              onClick={logout}
              className="text-slate-400 hover:text-red-500 transition-colors"
              title="Sign out"
            >
              <ArrowRightStartOnRectangleIcon className="h-4 w-4" />
            </button>
          </div>
        </div>
      </aside>

      {/* Main content */}
      <main className="flex-1 overflow-y-auto">
        <Outlet />
      </main>
    </div>
  )
}
