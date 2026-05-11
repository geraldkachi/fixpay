import { http, HttpResponse, delay } from 'msw'
import { mockDisputes } from '../data'

let disputes = [...mockDisputes]

export const disputeHandlers = [
  http.get('/api/disputes', async () => {
    await delay(400)
    return HttpResponse.json({ content: disputes, totalElements: disputes.length })
  }),

  http.get('/api/disputes/:id', async ({ params }) => {
    await delay(300)
    const d = disputes.find(x => x.id === params.id)
    if (!d) return HttpResponse.json({ message: 'Not found' }, { status: 404 })
    return HttpResponse.json(d)
  }),

  http.post('/api/disputes', async ({ request }) => {
    await delay(800)
    const body = await request.json() as Record<string, unknown>
    const newDispute = {
      id: 'dsp_' + Date.now(),
      transactionId: body.transactionId as string,
      category: body.category,
      description: body.description as string,
      status: 'open' as const,
      slaDeadline: new Date(Date.now() + 7 * 86_400_000).toISOString(),
      createdAt: new Date().toISOString(),
      transaction: mockDisputes[0].transaction, // stub
    }
    disputes = [newDispute as typeof mockDisputes[0], ...disputes]
    return HttpResponse.json(newDispute, { status: 201 })
  }),

  http.put('/api/disputes/:id/close', async ({ params }) => {
    await delay(500)
    disputes = disputes.map(d => d.id === params.id ? { ...d, status: 'closed' as const } : d)
    return HttpResponse.json({ message: 'Dispute closed' })
  }),
]
