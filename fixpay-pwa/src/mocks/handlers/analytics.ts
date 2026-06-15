import { http, HttpResponse } from 'msw'
const env = { API_URL: "" };

export const analyticsHandlers = [
  http.get(`${env.API_URL}/api/analytics`, ({ request }) => {
    const url = new URL(request.url)
    const period = url.searchParams.get('period') || '30d'

    // Mock trend data
    const today = new Date()
    const trend_data = Array.from({ length: period === '7d' ? 7 : 30 }).map((_, i) => {
      const d = new Date(today)
      d.setDate(d.getDate() - (period === '7d' ? 6 - i : 29 - i))
      return {
        date: d.toISOString(),
        income: Math.floor(Math.random() * 5000000) + 100000,
        expense: Math.floor(Math.random() * 4000000) + 50000,
      }
    })

    const income_total = trend_data.reduce((sum, day) => sum + day.income, 0)
    const expense_total = trend_data.reduce((sum, day) => sum + day.expense, 0)

    return HttpResponse.json({
      success: true,
      data: {
        income_total,
        expense_total,
        categories_breakdown: {
          'airtime': 1500000,
          'data': 2500000,
          'electricity': 5000000,
          'tv': 1200000,
          'Transfer_BANK': 8500000,
          'Transfer_WALLET': 3000000
        },
        payment_methods_breakdown: {
          'WALLET': 18000000,
          'CARD': 2500000,
          'USSD': 1200000,
        },
        disputes_summary: {
          'OPEN': 1,
          'RESOLVED': 4
        },
        trend_data,
        period,
        timestamp: new Date().toISOString()
      }
    })
  })
]
