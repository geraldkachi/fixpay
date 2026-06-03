// Shared domain types — mirrored from fixpay-pwa/src/types/index.ts
// Keep in sync with Laravel API responses.

export interface User {
  id: string
  phone?: string
  email?: string
  first_name: string
  last_name: string
  tier: 1 | 2 | 3
  kyc_status: 'UNVERIFIED' | 'PARTIAL' | 'VERIFIED' | 'REJECTED'
  status: 'PENDING' | 'ACTIVE' | 'SUSPENDED' | 'OFFBOARDED'
  created_at: string
}

export interface Wallet {
  id: string
  balance_kobo: number
  currency: 'NGN'
  status: 'active' | 'frozen' | 'closed'
  virtual_account_number?: string
  virtual_account_bank?: string
}

export type LedgerType =
  | 'CREDIT'
  | 'DEBIT'
  | 'REVERSAL'

export interface LedgerEntry {
  id: string
  wallet_id: string
  type: LedgerType
  amount_kobo: number
  running_balance_kobo: number
  reference: string
  description: string
  created_at: string
}

export interface VtpassPayment {
  id: string
  reference: string
  service_id: string
  amount_kobo: number
  fee_kobo: number
  status: 'PENDING' | 'COMPLETED' | 'FAILED'
  token?: string
  created_at: string
}

export interface Transfer {
  id: string
  reference: string
  direction: 'OUTBOUND' | 'INBOUND'
  transfer_type: 'BANK' | 'WALLET'
  amount_kobo: number
  fee_kobo: number
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'REVERSED'
  recipient_name?: string
  recipient_account?: string
  recipient_bank_name?: string
  narration?: string
  created_at: string
}

export interface Dispute {
  id: string
  category: string
  reason: string
  status: 'OPEN' | 'UNDER_REVIEW' | 'RESOLVED' | 'REJECTED'
  sla_deadline: string
  created_at: string
}

// ── Portal/Tenant types ───────────────────────────────────────────────────

export interface Tenant {
  id: string
  name: string
  slug: string
  email: string
  status: 'SANDBOX' | 'ACTIVE' | 'SUSPENDED' | 'OFFBOARDED'
  plan: 'STARTER' | 'GROWTH' | 'ENTERPRISE'
  kyb_status: 'PENDING' | 'SUBMITTED' | 'APPROVED' | 'REJECTED'
  go_live_requested_at?: string
  activated_at?: string
  created_at: string
}

export interface ApiKey {
  id: string
  name: string
  key_prefix: string
  environment: 'sandbox' | 'live'
  is_revoked: boolean
  last_used_at?: string
  expires_at?: string
  created_at: string
}

// ── Pagination wrapper ────────────────────────────────────────────────────

export interface Paginated<T> {
  data: T[]
  current_page: number
  last_page: number
  per_page: number
  total: number
}

// ── Auth ──────────────────────────────────────────────────────────────────

export interface AuthToken {
  token: string
  type: 'Bearer'
  expires_at?: string
}
