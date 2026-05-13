import { api } from '@/lib/api'

/**
 * MandateResponse — matches the backend MandateController DTO shape exactly.
 * maxAmount is in NGN (not kobo).
 */
export interface MandateResponse {
  mandateReference: string
  providerReference: string | null
  bankCode: string
  accountNumber: string
  maxAmount: number        // NGN
  status: 'pending_auth' | 'active' | 'paused' | 'cancelled' | 'expired'
  startDate: string        // ISO date string (LocalDate)
  endDate: string | null
  providerMessage: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateMandatePayload {
  bankCode: string
  accountNumber: string
  maxAmount: number       // NGN
  startDate: string       // ISO date (YYYY-MM-DD)
  endDate?: string
}

type ApiWrapped<T> = { success: boolean; data: T }

function unwrap<T>(response: ApiWrapped<T> | T): T {
  const r = response as ApiWrapped<T>
  if (r.success !== undefined && r.data !== undefined) return r.data
  return response as T
}

export const mandatesService = {
  list: (): Promise<MandateResponse[]> =>
    api.get<ApiWrapped<MandateResponse[]> | MandateResponse[]>('/mandates').then(r => {
      const payload = unwrap(r.data)
      // ApiResponse<List<MandateResponse>> — data is the array directly
      if (Array.isArray(payload)) return payload
      // MSW mock returns { content: [...] }
      const legacy = payload as unknown as { content?: MandateResponse[] }
      return legacy.content ?? []
    }),

  get: (mandateReference: string): Promise<MandateResponse> =>
    api.get<ApiWrapped<MandateResponse>>(`/mandates/${mandateReference}`).then(r =>
      unwrap(r.data)
    ),

  create: (payload: CreateMandatePayload): Promise<MandateResponse> =>
    api.post<ApiWrapped<MandateResponse>>('/mandates', payload).then(r => unwrap(r.data)),

  sync: (mandateReference: string): Promise<MandateResponse> =>
    api.post<ApiWrapped<MandateResponse>>(`/mandates/${mandateReference}/sync`).then(r =>
      unwrap(r.data)
    ),
}
