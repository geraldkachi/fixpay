import { api } from '@/lib/api'

export interface RegisterPayload {
  tenantId: string
  phone: string
  password: string
  email?: string
}

export interface RegisterResponse {
  userId: string
  walletId: string
  phone: string
  message: string
}

type ApiWrapped<T> = { success: boolean; message: string | null; data: T }

export const authService = {
  /**
   * POST /auth/register
   * Public endpoint — does not require a JWT.
   */
  register: (payload: RegisterPayload): Promise<RegisterResponse> =>
    api.post<ApiWrapped<RegisterResponse>>('/auth/register', payload).then(r => {
      const d = r.data as ApiWrapped<RegisterResponse>
      // Real backend wraps in ApiResponse
      if (d.data && d.success !== undefined) return d.data
      // MSW mock may return shape directly
      return r.data as unknown as RegisterResponse
    }),

  /**
   * POST /auth/pin/verify
   * Used by payment screens to confirm PIN before debit.
   */
  verifyPin: (pin: string): Promise<void> =>
    api.post('/auth/pin/verify', { pin }).then(() => undefined),

  /**
   * POST /auth/pin/create
   */
  createPin: (pin: string): Promise<void> =>
    api.post('/auth/pin/create', { pin }).then(() => undefined),
}
