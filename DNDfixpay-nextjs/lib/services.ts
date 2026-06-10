import { api } from '@/lib/api'
import type { User, Wallet, LedgerEntry, Paginated } from '@/lib/types'

// ── Auth ──────────────────────────────────────────────────────────────────

export const authService = {
  getCsrf: () => fetch('http://localhost:8000/sanctum/csrf-cookie', { credentials: 'include' }),

  register: (payload: { phone: string; email: string; first_name: string; last_name: string; password: string }) =>
    api.post<{ message: string; user_id: string }>('/auth/register', payload),

  login: (payload: { identifier: string; password: string }) =>
    api.post<{ access_token: string; user: User }>('/auth/login', payload),

  verifyOtp: (payload: { identifier: string; purpose: string; code: string }) =>
    api.post<{ message: string }>('/auth/verify-otp', payload),

  resendOtp: (payload: { identifier: string; purpose: string }) =>
    api.post<{ message: string }>('/auth/resend-otp', payload),

  logout: () => api.post<void>('/auth/logout'),

  setPin: (pin: string) => api.post<{ message: string }>('/auth/pin/set', { pin }),
  verifyPin: (pin: string) => api.post<{ message: string }>('/auth/pin/verify', { pin }),
  changePin: (old_pin: string, new_pin: string) => api.put<{ message: string }>('/auth/pin/change', { old_pin, new_pin }),
}

// ── User ──────────────────────────────────────────────────────────────────

export const userService = {
  getProfile: () => api.get<User & { wallet: Wallet }>('/user/profile'),
  updateProfile: (data: Partial<User>) => api.put<User>('/user/profile', data),
}

// ── KYC ───────────────────────────────────────────────────────────────────

export const kycService = {
  verifyBvn: (bvn: string, dob: string) => api.post('/kyc/bvn', { bvn, dob }),
  verifyNin: (nin: string) => api.post('/kyc/nin', { nin }),
  getStatus: () => api.get('/kyc/status'),
}

// ── Wallet ────────────────────────────────────────────────────────────────

export const walletService = {
  get: () => api.get<Wallet>('/wallet'),
  transactions: (params?: Record<string, string>) => {
    const qs = params ? '?' + new URLSearchParams(params).toString() : ''
    return api.get<Paginated<LedgerEntry>>(`/wallet/transactions${qs}`)
  },
}

// ── Transfers ─────────────────────────────────────────────────────────────

export const transferService = {
  toBank: (payload: { account_number: string; bank_code: string; amount_kobo: number; narration?: string; pin: string }) =>
    api.post('/transfers/bank', payload),
  toWallet: (payload: { recipient_phone: string; amount_kobo: number; narration?: string; pin: string }) =>
    api.post('/transfers/wallet', payload),
  list: () => api.get('/transfers'),
  status: (ref: string) => api.get(`/transfers/${ref}`),
}

// ── VTPass payments ───────────────────────────────────────────────────────

export const paymentsService = {
  services: (identifier: string) =>
    api.get(`/payments/vtpass/services?serviceID=${encodeURIComponent(identifier)}`),
  variations: (serviceId: string) =>
    api.get(`/payments/vtpass/variations?serviceID=${encodeURIComponent(serviceId)}`),
  pay: (payload: Record<string, unknown>) => api.post('/payments/vtpass', payload),
  status: (ref: string) => api.get(`/payments/vtpass/${ref}`),
}

// ── Disputes ──────────────────────────────────────────────────────────────

export const disputeService = {
  list: () => api.get('/disputes'),
  create: (payload: Record<string, unknown>) => api.post('/disputes', payload),
  get: (id: string) => api.get(`/disputes/${id}`),
}
