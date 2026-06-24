import { create } from 'zustand'
import type { TenantConfig } from '@/types'

const DEFAULTS: TenantConfig = {
  tenantId: 'default',
  slug: 'fixpay',
  appName: 'FixPay',
  primaryColor: '#A51D21',
  secondaryColor: '#34C759',
  accentColor: '#FF9500',
  logoUrl: null,
  faviconUrl: null,
  supportEmail: 'support@fixpay.com',
  supportPhone: '07000000000',
  features: {
    billPayments: true, directDebit: true, walletTransfers: true,
    internationalAirtime: false, disputeManagement: true, nibssTransfers: true,
  },
}

function applyBrand(cfg: TenantConfig) {
  const el = document.documentElement
  el.style.setProperty('--brand-primary', cfg.primaryColor)
  el.style.setProperty('--brand-secondary', cfg.secondaryColor)
  el.style.setProperty('--brand-accent', cfg.accentColor)
  if (cfg.appName) document.title = cfg.appName
}

// Apply defaults immediately so first render never flashes wrong color
applyBrand(DEFAULTS)

interface TenantState {
  config: TenantConfig
  isLoaded: boolean
  setConfig: (cfg: TenantConfig) => void
}

export const useTenantStore = create<TenantState>()((set) => ({
  config: DEFAULTS,
  isLoaded: false,
  setConfig: (cfg) => { applyBrand(cfg); set({ config: cfg, isLoaded: true }) },
}))

export function useTenant(): TenantConfig {
  return useTenantStore((s) => s.config)
}
