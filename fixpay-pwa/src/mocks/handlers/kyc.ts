import { http, HttpResponse, delay } from 'msw'

export const kycHandlers = [
  http.post('/api/kyc/nin', async ({ request }) => {
    await delay(1200)
    const body = await request.json() as Record<string, string>
    if (!body.nin || body.nin.length !== 11) return HttpResponse.json({ message: 'Invalid NIN' }, { status: 400 })
    return HttpResponse.json({ message: 'NIN verified', data: { firstName: 'JOHN', lastName: 'ADEYEMI', middleName: 'CHUKWU', dateOfBirth: '1990-05-11', gender: 'M' } })
  }),

  http.post('/api/kyc/bvn', async ({ request }) => {
    await delay(1200)
    const body = await request.json() as Record<string, string>
    if (!body.bvn || body.bvn.length !== 11) return HttpResponse.json({ message: 'Invalid BVN' }, { status: 400 })
    return HttpResponse.json({ message: 'BVN verified', data: { firstName: 'JOHN', lastName: 'ADEYEMI', dateOfBirth: '1990-05-11', enrollmentBank: 'Guaranty Trust Bank', kycLevel: 2 } })
  }),

  http.post('/api/kyc/selfie', async () => {
    await delay(2000)
    return HttpResponse.json({ message: 'Liveness check passed', score: 0.97 })
  }),

  http.get('/api/kyc/status', async () => {
    await delay(300)
    return HttpResponse.json({ status: 'verified', ninVerified: true, bvnVerified: true, selfiePassed: true })
  }),
]
