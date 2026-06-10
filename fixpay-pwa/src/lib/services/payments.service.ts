import { api } from '@/lib/api'
import type { BillerVerify, ServiceVariation } from '@/types'

export interface BillPaymentResponse {
  payment_reference: string
  status: string
  amount_kobo: number    // kobo — already deducted from wallet server-side
  fee_kobo: number
  token?: string         // electricity prepaid token
  units?: string         // electricity units
  Pin?: string           // education exam PIN
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

// Helper — builds the unified VTpass payload the backend expects
function vtpassPayload(
  serviceId: string,
  amountNaira: number,
  phone: string,
  billersCode?: string,
  variationCode?: string,
) {
  return {
    service_id:     serviceId,
    amount_kobo:    Math.round(amountNaira * 100),
    phone,
    billers_code:   billersCode  ?? undefined,
    variation_code: variationCode ?? undefined,
  }
}

export const paymentsService = {
  // GET /api/payments/vtpass/services?identifier=<id>
  getVariations: (serviceId: string): Promise<ServiceVariation[]> =>
    api.get<VtpassVariationsResponse>('/payments/vtpass/variations', {
      params: { serviceID: serviceId },
    }).then(r => {
      const d = r.data
      if (d.content?.variations) return d.content.variations
      if (d.variations) return d.variations
      return []
    }),

  // POST /api/payments/vtpass/verify (proxied by backend to VTpass)
  verify: (payload: VerifyPayload): Promise<BillerVerify> =>
    api.post<VtpassVerifyResponse>('/payments/verify', {
      service_id:   payload.serviceId,
      billers_code: payload.billersCode,
      type:         payload.type,
    }).then(r => {
      const d = r.data
      if (d.content && d.code !== undefined) return d.content as BillerVerify
      return d as BillerVerify
    }),

  // All bill payments → POST /api/payments/vtpass
  airtime: (payload: AirtimePayload): Promise<BillPaymentResponse> =>
    api.post<BillPaymentResponse>('/payments/vtpass',
      vtpassPayload(payload.serviceId, payload.amount, payload.phone),
      { headers: { 'X-Idempotency-Key': self.crypto.randomUUID() } }
    ).then(r => r.data),

  data: (payload: DataPayload): Promise<BillPaymentResponse> =>
    api.post<BillPaymentResponse>('/payments/vtpass',
      vtpassPayload(payload.serviceId, 0, payload.billersCode, payload.billersCode, payload.variationCode),
      { headers: { 'X-Idempotency-Key': self.crypto.randomUUID() } }
    ).then(r => r.data),

  tv: (payload: TvPayload): Promise<BillPaymentResponse> =>
    api.post<BillPaymentResponse>('/payments/vtpass',
      vtpassPayload(payload.serviceId, payload.amount ?? 0, payload.billersCode, payload.billersCode, payload.variationCode),
      { headers: { 'X-Idempotency-Key': self.crypto.randomUUID() } }
    ).then(r => r.data),

  electricity: (payload: ElectricityPayload): Promise<BillPaymentResponse> =>
    api.post<BillPaymentResponse>('/payments/vtpass',
      vtpassPayload(payload.serviceId, payload.amount, payload.billersCode, payload.billersCode, payload.variationCode),
      { headers: { 'X-Idempotency-Key': self.crypto.randomUUID() } }
    ).then(r => r.data),

  education: (payload: EducationPayload): Promise<BillPaymentResponse> =>
    api.post<BillPaymentResponse>('/payments/vtpass',
      vtpassPayload(payload.serviceId, 0, payload.phone ?? payload.billersCode, payload.billersCode, payload.variationCode),
      { headers: { 'X-Idempotency-Key': self.crypto.randomUUID() } }
    ).then(r => r.data),

  insurance: (payload: InsurancePayload): Promise<BillPaymentResponse> =>
    api.post<BillPaymentResponse>('/payments/vtpass',
      vtpassPayload(payload.serviceId, 0, payload.billersCode, payload.billersCode, payload.variationCode),
      { headers: { 'X-Idempotency-Key': self.crypto.randomUUID() } }
    ).then(r => r.data),
}
