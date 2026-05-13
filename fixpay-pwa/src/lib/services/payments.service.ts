import { api } from '@/lib/api'
import type { BillerVerify, ServiceVariation } from '@/types'

export interface BillPaymentResponse {
  requestId: string
  transaction_date: string
  amount: string
  token?: string
  units?: string
  Pin?: string
  purchased_code?: string
}

export interface AirtimePayload {
  serviceId: string
  phone: string
  amount: number
}

export interface DataPayload {
  serviceId: string
  billersCode: string
  variationCode: string
}

export interface TvPayload {
  serviceId: string
  billersCode: string
  variationCode: string
  subscriptionType: 'renew' | 'change'
  amount?: number
}

export interface ElectricityPayload {
  serviceId: string
  billersCode: string
  variationCode: 'prepaid' | 'postpaid'
  amount: number
}

export interface EducationPayload {
  serviceId: string
  billersCode: string
  variationCode: string
  phone?: string
}

export interface InsurancePayload {
  serviceId: string
  billersCode: string
  variationCode: string
}

export interface VerifyPayload {
  serviceId: string
  billersCode: string
  type?: string
}

// VTpass variations response shape (real backend forwards VTpass JSON after camelCase normalisation)
interface VtpassVariationsResponse {
  responseDescription?: string
  content?: { variations?: ServiceVariation[] }
  // MSW mock returns flat { variations: [] }
  variations?: ServiceVariation[]
}

// VTpass verify response (real backend returns Map<String, Object> from VTpass)
interface VtpassVerifyResponse {
  code?: string
  content?: BillerVerify
  // MSW mock returns BillerVerify directly
  customerName?: string
}

export const paymentsService = {
  getVariations: (serviceId: string): Promise<ServiceVariation[]> =>
    api.get<VtpassVariationsResponse>(`/payments/variations/${serviceId}`).then(r => {
      const d = r.data
      // Real backend: { content: { variations: [...] } }
      if (d.content?.variations) return d.content.variations
      // MSW mock: { variations: [...] }
      if (d.variations) return d.variations
      return []
    }),

  verify: (payload: VerifyPayload): Promise<BillerVerify> =>
    api.post<VtpassVerifyResponse>('/payments/verify', payload).then(r => {
      const d = r.data
      // Real backend: { code, content: { customerName, ... } }
      if (d.content && d.code !== undefined) return d.content as BillerVerify
      // MSW mock: BillerVerify directly
      return d as BillerVerify
    }),

  airtime: (payload: AirtimePayload): Promise<BillPaymentResponse> =>
    api.post<BillPaymentResponse>('/payments/airtime', payload).then(r => r.data),

  data: (payload: DataPayload): Promise<BillPaymentResponse> =>
    api.post<BillPaymentResponse>('/payments/data', payload).then(r => r.data),

  tv: (payload: TvPayload): Promise<BillPaymentResponse> =>
    api.post<BillPaymentResponse>('/payments/tv', payload).then(r => r.data),

  electricity: (payload: ElectricityPayload): Promise<BillPaymentResponse> =>
    api.post<BillPaymentResponse>('/payments/electricity', payload).then(r => r.data),

  education: (payload: EducationPayload): Promise<BillPaymentResponse> =>
    api.post<BillPaymentResponse>('/payments/education', payload).then(r => r.data),

  insurance: (payload: InsurancePayload): Promise<BillPaymentResponse> =>
    api.post<BillPaymentResponse>('/payments/insurance', payload).then(r => r.data),
}
