import { type ClassValue, clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatCurrency(kobo: number, compact = false): string {
  const naira = kobo / 100
  if (compact && naira >= 1_000_000) return `₦${(naira / 1_000_000).toFixed(1)}M`
  if (compact && naira >= 1_000) return `₦${(naira / 1_000).toFixed(1)}K`
  return new Intl.NumberFormat('en-NG', {
    style: 'currency',
    currency: 'NGN',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(naira)
}

export function formatDateShort(iso: string): string {
  const d = new Date(iso)
  const now = new Date()
  const diffMs = now.getTime() - d.getTime()
  const diffMin = Math.floor(diffMs / 60000)
  const diffH = Math.floor(diffMs / 3600000)
  const diffD = Math.floor(diffMs / 86400000)
  if (diffMin < 1) return 'Just now'
  if (diffMin < 60) return `${diffMin}m ago`
  if (diffH < 24) return `${diffH}h ago`
  if (diffD === 1) return 'Yesterday'
  if (diffD < 7) return `${diffD} days ago`
  return new Intl.DateTimeFormat('en-NG', { day: 'numeric', month: 'short' }).format(d)
}

export function formatDateFull(iso: string): string {
  return new Intl.DateTimeFormat('en-NG', {
    day: 'numeric', month: 'long', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  }).format(new Date(iso))
}

export function maskAccount(acct: string): string {
  return `****${acct.slice(-4)}`
}

export function generateRequestId(): string {
  const d = new Date().toISOString().slice(0, 10).replace(/-/g, '')
  const r = Math.random().toString(36).slice(2, 10)
  return `${d}-${r}`
}

export function sleep(ms: number): Promise<void> {
  return new Promise(r => setTimeout(r, ms))
}

export function vibrate(pattern: number | number[]): void {
  try { navigator.vibrate(pattern) } catch { /* unsupported */ }
}

export function hexToRgb(hex: string): string {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return `${r} ${g} ${b}`
}
