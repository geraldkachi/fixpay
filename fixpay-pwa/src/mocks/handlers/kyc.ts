import { http, HttpResponse, delay } from 'msw'

export const kycHandlers = [
  http.post('/api/kyc/nin', async ({ request }) => {
    await delay(1200)
    const body = await request.json() as Record<string, string>
    if (!body.nin || body.nin.length !== 11) return HttpResponse.json({ success: false, message: 'Invalid NIN' }, { status: 400 })
    return HttpResponse.json({ success: true, message: 'NIN verified - firstName=, lastName=', data: { firstName: '', lastName: '' } })
  }),

  http.post('/api/kyc/bvn', async ({ request }) => {
    await delay(1200)
    const body = await request.json() as Record<string, string>
    if (!body.bvn || body.bvn.length !== 11) return HttpResponse.json({ success: false, message: 'Invalid BVN' }, { status: 400 })
    return HttpResponse.json({ success: true, message: 'BVN verified - bank=', data: { bank: '' } })
  }),

  http.post('/api/kyc/selfie', async () => {
    await delay(2000)
    return HttpResponse.json({ success: true, message: 'Liveness check passed', data: { score: '0.97' } })
  }),

  http.get('/api/kyc/status', async () => {
    await delay(300)
    return HttpResponse.json({ status: 'verified', ninVerified: true, bvnVerified: true, selfiePassed: true })
  }),
]
