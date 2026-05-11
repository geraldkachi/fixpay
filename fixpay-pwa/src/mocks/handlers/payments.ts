import { http, HttpResponse, delay } from 'msw'
import { variations } from '../data'
import { generateRequestId } from '@/lib/utils'
import { deductBalance } from './wallet'

export const paymentHandlers = [
  // GET variation codes
  http.get('/api/payments/variations/:serviceId', async ({ params }) => {
    await delay(400)
    const vars = variations[params.serviceId as string]
    if (!vars) return HttpResponse.json({ message: 'Service not found' }, { status: 404 })
    return HttpResponse.json({ serviceId: params.serviceId, variations: vars })
  }),

  // Merchant verify (TV smartcard / electricity meter / JAMB profile)
  http.post('/api/payments/verify', async ({ request }) => {
    await delay(900)
    const body = await request.json() as Record<string, string>
    const { serviceId, billersCode, type } = body

    if (serviceId === 'dstv' || serviceId === 'gotv' || serviceId === 'startimes') {
      if (billersCode === '1212121212') {
        return HttpResponse.json({ code: '020', content: { Customer_Name: 'Emeka Okafor', Status: 'Active', Due_Date: '2026-06-10', Customer_Number: billersCode, Current_Bouquet: serviceId === 'dstv' ? 'DStv Compact' : 'GOtv Jolli', Renewal_Amount: serviceId === 'dstv' ? 9600 : 3300 } })
      }
      return HttpResponse.json({ code: '011', content: { message: 'Customer not found' } }, { status: 400 })
    }

    if (serviceId?.includes('electric')) {
      if (billersCode === '1111111111111' && type === 'prepaid') {
        return HttpResponse.json({ code: '020', content: { Customer_Name: 'Aisha Mohammed', Meter_Number: billersCode, Address: '24 Ikeja Way, Lagos', Customer_District: 'Ikeja', Outstanding: '0', Meter_Type: 'prepaid', Customer_Account_Type: 'NMD' } })
      }
      if (billersCode === '1010101010101' && type === 'postpaid') {
        return HttpResponse.json({ code: '020', content: { Customer_Name: 'Aisha Mohammed', Meter_Number: billersCode, Address: '24 Ikeja Way, Lagos', Customer_District: 'Ikeja', Outstanding: '4500', Meter_Type: 'postpaid', Customer_Account_Type: 'NMD' } })
      }
      return HttpResponse.json({ code: '011', content: { message: 'Meter not found' } }, { status: 400 })
    }

    if (serviceId === 'jamb') {
      if (billersCode === '0123456789') {
        return HttpResponse.json({ code: '000', content: { Customer_Name: 'Chidinma Eze', commission_details: { amount: 10.22, rate: '1.50', rate_type: 'percent' } } })
      }
      return HttpResponse.json({ code: '011', content: { message: 'Profile ID not found' } }, { status: 400 })
    }

    return HttpResponse.json({ code: '030', message: 'Biller not reachable' }, { status: 400 })
  }),

  // Pay endpoint (airtime, data, TV, electricity, education)
  http.post('/api/payments/airtime', async ({ request }) => {
    await delay(1200)
    const body = await request.json() as Record<string, unknown>
    const amount = Number(body.amount) * 100
    deductBalance(amount)
    return HttpResponse.json({
      code: '000',
      content: { transactions: { status: 'delivered', product_name: `${String(body.serviceId).toUpperCase()} Airtime VTU`, unique_element: body.phone, unit_price: body.amount, quantity: 1, commission: 3, total_amount: Number(body.amount) - 3 } },
      response_description: 'TRANSACTION SUCCESSFUL',
      requestId: generateRequestId(),
      amount: body.amount,
      transaction_date: new Date().toISOString(),
      purchased_code: '',
    })
  }),

  http.post('/api/payments/data', async ({ request }) => {
    await delay(1200)
    const body = await request.json() as Record<string, unknown>
    const vars = variations[body.serviceId as string] ?? []
    const chosen = vars.find(v => v.variationCode === body.variationCode)
    const amount = parseFloat(chosen?.variationAmount ?? '0') * 100
    deductBalance(amount)
    return HttpResponse.json({
      code: '000',
      content: { transactions: { status: 'delivered', product_name: chosen?.name ?? 'Data Bundle', unique_element: body.billersCode } },
      response_description: 'TRANSACTION SUCCESSFUL',
      requestId: generateRequestId(),
      amount: chosen?.variationAmount,
      transaction_date: new Date().toISOString(),
    })
  }),

  http.post('/api/payments/tv', async ({ request }) => {
    await delay(1400)
    const body = await request.json() as Record<string, unknown>
    const vars = variations[body.serviceId as string] ?? []
    const chosen = vars.find(v => v.variationCode === body.variationCode)
    const amount = body.subscriptionType === 'renew'
      ? (Number(body.amount) * 100)
      : parseFloat(chosen?.variationAmount ?? '0') * 100
    deductBalance(amount)
    return HttpResponse.json({
      code: '000',
      content: { transactions: { status: 'delivered', product_name: chosen?.name ?? 'TV Subscription', unique_element: body.billersCode } },
      response_description: 'TRANSACTION SUCCESSFUL',
      requestId: generateRequestId(),
      amount: amount / 100,
      transaction_date: new Date().toISOString(),
    })
  }),

  http.post('/api/payments/electricity', async ({ request }) => {
    await delay(1600)
    const body = await request.json() as Record<string, unknown>
    const amount = Number(body.amount) * 100
    deductBalance(amount)
    const isPrepaid = body.variationCode === 'prepaid'
    return HttpResponse.json({
      code: '000',
      content: { transactions: { status: 'delivered', product_name: 'Ikeja Electric Payment Service', unique_element: body.billersCode, unit_price: body.amount } },
      response_description: 'TRANSACTION SUCCESSFUL',
      requestId: generateRequestId(),
      amount: body.amount,
      transaction_date: new Date().toISOString(),
      purchased_code: isPrepaid ? '5024-8167-3921-4856-7301' : '',
      token: isPrepaid ? '5024-8167-3921-4856-7301' : undefined,
      units: isPrepaid ? `${(Number(body.amount) / 47.5).toFixed(1)} kWh` : undefined,
    })
  }),

  http.post('/api/payments/education', async ({ request }) => {
    await delay(1400)
    const body = await request.json() as Record<string, unknown>
    const vars = variations[body.serviceId as string] ?? []
    const chosen = vars.find(v => v.variationCode === body.variationCode)
    const amount = parseFloat(chosen?.variationAmount ?? '0') * 100
    deductBalance(amount)
    const isJamb = body.serviceId === 'jamb'
    return HttpResponse.json({
      code: '000',
      content: { transactions: { status: 'delivered', product_name: chosen?.name, unique_element: body.billersCode ?? body.phone } },
      response_description: 'TRANSACTION SUCCESSFUL',
      requestId: generateRequestId(),
      amount: chosen?.variationAmount,
      transaction_date: new Date().toISOString(),
      purchased_code: isJamb ? 'Pin : 3678251321392432' : 'Token: 0100070365657400875',
      Pin: isJamb ? 'Pin : 3678251321392432' : undefined,
    })
  }),

  http.post('/api/payments/requery/:requestId', async () => {
    await delay(600)
    return HttpResponse.json({ code: '000', content: { transactions: { status: 'delivered' } } })
  }),
]
