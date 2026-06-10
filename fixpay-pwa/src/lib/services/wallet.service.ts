import { api } from '@/lib/api'
import { saveTransactions, loadTransactions, loadTransaction } from '@/lib/db'
import type { Wallet, Transaction } from '@/types'

export interface WalletBalanceResponse {
  id: string
  balance_kobo: number      // kobo integer — returned directly by backend
  ledger_balance_kobo: number
  currency: string
  status: string
  virtual_account_number: string | null
  virtual_account_bank: string | null
  virtual_account_bank_code: string | null
}

function toWallet(b: WalletBalanceResponse): Wallet {
  return {
    id: b.id,
    // balance_kobo is already in kobo — no multiplication needed
    balanceKobo: b.balance_kobo,
    currency: b.currency ?? 'NGN',
    status: b.status as Wallet['status'],
    virtualAccount: {
      accountNumber: b.virtual_account_number ?? '',
      bankName:      b.virtual_account_bank ?? '',
      bankCode:      b.virtual_account_bank_code ?? '',
    },
  }
}

export interface TransactionPage {
  content: Transaction[]
  totalElements: number
  totalPages: number
  number: number
}

export const walletService = {
  /**
   * GET /wallet — wrapped in ApiResponse<WalletBalanceResponse>.
   * Backend returns balance_kobo (integer kobo), NOT a float NGN value.
   */
  getBalance: (): Promise<Wallet> =>
    api.get<{ success: boolean; data: WalletBalanceResponse } | WalletBalanceResponse>('/wallet').then(r => {
      const payload = r.data as { success?: boolean; data?: WalletBalanceResponse }
      if (payload.success !== undefined && payload.data) {
        return toWallet(payload.data)
      }
      // direct shape (no ApiResponse wrapper)
      return toWallet(r.data as WalletBalanceResponse)
    }),

  /**
   * GET /wallet/transactions?page&size&type
   *
   * On success  → writes to encrypted IndexedDB then returns fresh data.
   * On network failure → falls back to the locally cached (encrypted) copy.
   */
  getTransactions: async (page = 0, size = 50, type?: string): Promise<TransactionPage> => {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (type) params.set('type', type)

    try {
      const r = await api.get<{ success?: boolean; data?: TransactionPage } | TransactionPage>(
        `/wallet/transactions?${params}`
      )
      const payload = r.data as { success?: boolean; data?: TransactionPage }
      const page_data: TransactionPage =
        payload.success !== undefined && payload.data
          ? payload.data
          : (r.data as TransactionPage)

      // Persist to encrypted IndexedDB for offline access
      if (page_data.content?.length) {
        saveTransactions(page_data.content).catch(() => undefined) // fire-and-forget
      }

      return page_data
    } catch (err) {
      // Network / server failure → serve from encrypted local cache
      const cached = await loadTransactions()
      if (cached.length > 0) {
        const slice = cached.slice(page * size, page * size + size)
        return {
          content: slice,
          totalElements: cached.length,
          totalPages: Math.ceil(cached.length / size),
          number: page,
        }
      }
      throw err // cache empty — surface the original error
    }
  },

  /**
   * GET /wallet/transactions/:id
   *
   * Tries the network first; falls back to the encrypted IndexedDB row.
   */
  getTransaction: async (id: string): Promise<Transaction> => {
    try {
      const r = await api.get<{ success?: boolean; data?: Transaction } | Transaction>(
        `/wallet/transactions/${id}`
      )
      const payload = r.data as { success?: boolean; data?: Transaction }
      const tx: Transaction =
        payload.success !== undefined && payload.data
          ? payload.data
          : (r.data as Transaction)

      // Keep the local cache up-to-date
      saveTransactions([tx]).catch(() => undefined)
      return tx
    } catch (err) {
      const cached = await loadTransaction(id)
      if (cached) return cached
      throw err
    }
  },
}
