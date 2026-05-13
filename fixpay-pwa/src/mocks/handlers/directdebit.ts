import { http, HttpResponse, delay } from 'msw'
import type { MandateResponse } from '@/types'
import { mockMandates, mockMandateResponses } from '../data'

let mandates = [...mockMandates]
let mandateResponses: MandateResponse[] = [...mockMandateResponses]

export const directDebitHandlers = [
  // ── Legacy paths (kept for backward compat) ───────────────────────────────
  http.get('/api/direct-debit/mandates', async () => {
    await delay(400)
    return HttpResponse.json({ content: mandates, totalElements: mandates.length })
  }),

  http.get('/api/direct-debit/mandates/:id', async ({ params }) => {
    await delay(300)
    const m = mandates.find(x => x.id === params.id)
    if (!m) return HttpResponse.json({ message: 'Not found' }, { status: 404 })
    return HttpResponse.json(m)
  }),

  http.post('/api/direct-debit/mandates', async ({ request }) => {
    await delay(1000)
    const body = await request.json() as Record<string, unknown>
    const newMandate = {
      id: 'mnd_' + Date.now(),
      ...body,
      status: 'pending_auth' as const,
      authorizationUrl: 'https://sandbox.gtbank.com/mandate-auth?ref=MND' + Date.now(),
      createdAt: new Date().toISOString(),
    }
    mandates = [newMandate as typeof mockMandates[0], ...mandates]
    return HttpResponse.json(newMandate, { status: 201 })
  }),

  http.delete('/api/direct-debit/mandates/:id', async ({ params }) => {
    await delay(600)
    mandates = mandates.map(m => m.id === params.id ? { ...m, status: 'cancelled' as const } : m)
    return HttpResponse.json({ message: 'Mandate cancelled' })
  }),

  // ── Real backend paths (/api/mandates) ────────────────────────────────────
  http.get('/api/mandates', async () => {
    await delay(400)
    return HttpResponse.json({
      success: true,
      message: null,
      errorCode: null,
      timestamp: new Date().toISOString(),
      data: mandateResponses,
    })
  }),

  http.get('/api/mandates/:ref', async ({ params }) => {
    await delay(300)
    const m = mandateResponses.find(x => x.mandateReference === params.ref)
    if (!m) return HttpResponse.json({ success: false, message: 'Not found', data: null, errorCode: 'NOT_FOUND', timestamp: new Date().toISOString() }, { status: 404 })
    return HttpResponse.json({ success: true, message: null, errorCode: null, timestamp: new Date().toISOString(), data: m })
  }),

  http.post('/api/mandates', async ({ request }) => {
    await delay(1000)
    const body = await request.json() as Omit<MandateResponse, 'mandateReference' | 'providerReference' | 'status' | 'providerMessage' | 'createdAt' | 'updatedAt'>
    const newMandate: MandateResponse = {
      mandateReference: 'MND-' + body.bankCode.toUpperCase() + '-' + Date.now(),
      providerReference: null,
      bankCode: body.bankCode,
      accountNumber: body.accountNumber,
      maxAmount: body.maxAmount,
      status: 'pending_auth',
      startDate: body.startDate,
      endDate: (body as Record<string, unknown>).endDate as string | null ?? null,
      providerMessage: 'Pending authorization',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }
    mandateResponses = [newMandate, ...mandateResponses]
    return HttpResponse.json({
      success: true,
      message: 'Mandate created successfully',
      errorCode: null,
      timestamp: new Date().toISOString(),
      data: newMandate,
    }, { status: 201 })
  }),

  http.post('/api/mandates/:ref/sync', async ({ params }) => {
    await delay(600)
    const m = mandateResponses.find(x => x.mandateReference === params.ref)
    if (!m) return HttpResponse.json({ success: false, message: 'Not found', data: null, errorCode: 'NOT_FOUND', timestamp: new Date().toISOString() }, { status: 404 })
    // Simulate moving from pending_auth → active on sync
    const updated: MandateResponse = m.status === 'pending_auth'
      ? { ...m, status: 'active', providerReference: 'NIBSS_' + Date.now(), providerMessage: 'Mandate activated', updatedAt: new Date().toISOString() }
      : { ...m, updatedAt: new Date().toISOString() }
    mandateResponses = mandateResponses.map(x => x.mandateReference === params.ref ? updated : x)
    return HttpResponse.json({ success: true, message: null, errorCode: null, timestamp: new Date().toISOString(), data: updated })
  }),
]
