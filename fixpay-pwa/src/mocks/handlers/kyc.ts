import { http, HttpResponse, delay } from 'msw'

export const kycHandlers = [
  http.post('/api/kyc/nin', async ({ request }) => {
    await delay(1200)
    const body = await request.json() as Record<string, string>
    if (!body.nin || body.nin.length !== 11) return HttpResponse.json({ success: false, message: 'Invalid NIN' }, { status: 400 })
    return HttpResponse.json({ success: true, message: 'NIN verified - firstName=, lastName=', data: { firstName: '', lastName: '' } })
  }),

  http.post('/api/kyc/bvn/consent/initiate', async ({ request }) => {
    await delay(1200)
    const body = await request.json() as Record<string, string>
    if (!body.bvn || body.bvn.length !== 11) return HttpResponse.json({ status: 'FAILED', message: 'Invalid BVN' }, { status: 400 })
    return HttpResponse.json({
      status: 'PENDING',
      message: 'Consent initiated successfully.',
      consentUrl: 'https://apitest.nibss-plc.com.ng/api/consent/mock?sessionId=mock_session_from_msw',
      sessionId: 'mock_session_from_msw'
    })
  }),

  http.get('/api/kyc/status', async () => {
    await delay(300)
    // MSW will return VERIFIED for testing, so the frontend unblocks on the first poll
    return HttpResponse.json({
      kyc_status: 'PENDING',
      tier: 1,
      verifications: [
        { type: 'NIN', status: 'VERIFIED' },
        { type: 'BVN', status: 'VERIFIED' }
      ]
    })
  }),
]
