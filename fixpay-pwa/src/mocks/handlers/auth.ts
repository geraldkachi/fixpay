import { http, HttpResponse, delay } from 'msw'
import { mockUser } from '../data'

let accessToken = 'mock-access-token-' + Date.now()

export const authHandlers = [
  http.post('/api/auth/register', async ({ request }) => {
    await delay(800)
    const body = await request.json() as Record<string, string>
    return HttpResponse.json({ message: 'OTP sent', identifier: body.phone || body.email }, { status: 200 })
  }),

  http.post('/api/auth/verify-otp', async ({ request }) => {
    await delay(600)
    const body = await request.json() as Record<string, string>
    if (body.otp !== '1234') return HttpResponse.json({ message: 'Invalid OTP' }, { status: 400 })
    accessToken = 'mock-access-token-' + Date.now()
    return HttpResponse.json({ accessToken, user: mockUser }, { status: 200 })
  }),

  http.post('/api/auth/login', async () => {
    await delay(800)
    accessToken = 'mock-access-token-' + Date.now()
    return HttpResponse.json({ accessToken, user: mockUser }, { status: 200 })
  }),

  http.post('/api/auth/refresh', async () => {
    await delay(300)
    accessToken = 'mock-access-token-' + Date.now()
    return HttpResponse.json({ accessToken }, { status: 200 })
  }),

  http.post('/api/auth/logout', async () => {
    await delay(200)
    return HttpResponse.json({ message: 'Logged out' }, { status: 200 })
  }),

  http.post('/api/auth/pin/set', async () => {
    await delay(400)
    return HttpResponse.json({ message: 'PIN created' }, { status: 200 })
  }),

  http.post('/api/auth/pin/verify', async () => {
    await delay(400)
    return HttpResponse.json({ message: 'PIN verified' }, { status: 200 })
  }),

  http.get('/api/users/me', async () => {
    await delay(300)
    return HttpResponse.json(mockUser)
  }),
]
