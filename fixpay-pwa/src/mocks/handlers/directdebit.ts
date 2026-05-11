import { http, HttpResponse, delay } from 'msw'
import { mockMandates } from '../data'

let mandates = [...mockMandates]

export const directDebitHandlers = [
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
]
