import { http, HttpResponse, delay } from 'msw'
import { mockBanks } from '../data'
import { deductBalance } from './wallet'

export const transferHandlers = [
  http.get('/api/transfers/banks', async () => {
    await delay(300)
    return HttpResponse.json(mockBanks)
  }),

  http.post('/api/transfers/verify-account', async ({ request }) => {
    await delay(1000)
    const body = await request.json() as Record<string, string>
    const bank = mockBanks.find(b => b.bankCode === body.bankCode)
    // Simulate name lookup
    const names: Record<string, string> = {
      '0123456789': 'FATIMA BELLO',
      '1234567890': 'CHIDI OKONKWO',
      '9876543210': 'AMINU GARBA',
    }
    const accountName = names[body.accountNumber] ?? 'DEMO ACCOUNT HOLDER'
    return HttpResponse.json({
      accountNumber: body.accountNumber,
      accountName,
      bankCode: body.bankCode,
      bankName: bank?.bankName ?? 'Unknown Bank',
      sessionId: 'SES' + Date.now(),
    })
  }),

  http.post('/api/transfers/bank', async ({ request }) => {
    await delay(1500)
    const body = await request.json() as Record<string, unknown>
    const amountKobo = Number(body.amountKobo)
    deductBalance(amountKobo + 5250) // include ₦52.50 fee
    return HttpResponse.json({
      transactionId: 'TXN' + Date.now(),
      reference: '20260511-TOUT-' + Math.random().toString(36).slice(2, 8),
      status: 'completed',
      message: 'Transfer successful',
    })
  }),

  http.post('/api/transfers/wallet', async ({ request }) => {
    await delay(800)
    const body = await request.json() as Record<string, unknown>
    deductBalance(Number(body.amountKobo))
    return HttpResponse.json({ status: 'completed', message: 'Transfer successful' })
  }),
]
