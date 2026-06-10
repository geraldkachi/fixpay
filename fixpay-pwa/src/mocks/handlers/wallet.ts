import { http, HttpResponse, delay } from 'msw'
import { mockWallet, mockTransactions } from '../data'

// Mutable in-memory wallet balance for the session
let balanceKobo = mockWallet.balanceKobo

export function deductBalance(kobo: number) { balanceKobo = Math.max(0, balanceKobo - kobo) }
export function addBalance(kobo: number) { balanceKobo += kobo }
export function getBalance() { return balanceKobo }

export const walletHandlers = [
  // Laravel backend path for wallet details
  http.get('/api/wallet', async () => {
    await delay(300)
    return HttpResponse.json({
      id: mockWallet.id,
      balance_kobo: balanceKobo,
      ledger_balance_kobo: balanceKobo,
      currency: mockWallet.currency,
      status: mockWallet.status,
      virtual_account_number: mockWallet.virtualAccount?.accountNumber ?? null,
      virtual_account_bank: mockWallet.virtualAccount?.bankName ?? null,
      virtual_account_bank_code: mockWallet.virtualAccount?.bankCode ?? null,
    })
  }),

  // Legacy path — kept for backward compat (FundWalletScreen still uses it)
  http.get('/api/wallet/me', async () => {
    await delay(400)
    return HttpResponse.json({ ...mockWallet, balanceKobo })
  }),

  // Real backend path — returns ApiResponse<WalletBalanceResponse> shape
  http.get('/api/wallet/balance', async () => {
    await delay(400)
    return HttpResponse.json({
      success: true,
      message: null,
      errorCode: null,
      timestamp: new Date().toISOString(),
      data: {
        walletId: mockWallet.id,
        currency: mockWallet.currency,
        availableBalance: balanceKobo / 100,
        ledgerBalance: balanceKobo / 100,
        status: mockWallet.status,
        virtualAccount: mockWallet.virtualAccount,
      },
    })
  }),

  http.get('/api/wallet/transactions', async ({ request }) => {
    await delay(500)
    const url = new URL(request.url)
    const page = parseInt(url.searchParams.get('page') ?? '0')
    const size = parseInt(url.searchParams.get('size') ?? '20')
    const type = url.searchParams.get('type')
    const filtered = type ? mockTransactions.filter(t => t.type === type) : mockTransactions
    const slice = filtered.slice(page * size, (page + 1) * size)
    return HttpResponse.json({ content: slice, totalElements: filtered.length, totalPages: Math.ceil(filtered.length / size), number: page })
  }),

  http.get('/api/wallet/transactions/:id', async ({ params }) => {
    await delay(300)
    const tx = mockTransactions.find(t => t.id === params.id)
    if (!tx) return HttpResponse.json({ message: 'Not found' }, { status: 404 })
    return HttpResponse.json(tx)
  }),
]
