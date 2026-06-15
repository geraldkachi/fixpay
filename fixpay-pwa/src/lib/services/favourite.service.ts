import { api } from '@/lib/api'
import type { Transaction } from '@/types'

export interface FavouritePayload {
  id: string
  type: string
  service_id?: string
  service_name?: string
  counterparty_name: string
  description?: string
  amount_kobo?: number
  transaction_reference?: string
}

export const favouriteService = {
  getFavourites: async (): Promise<FavouritePayload[]> => {
    const res = await api.get<{ data: FavouritePayload[] }>('/favourites')
    return res.data.data
  },

  saveFavourite: async (tx: Transaction): Promise<FavouritePayload> => {
    const payload = {
      type: tx.type,
      service_id: tx.serviceId,
      service_name: tx.serviceName,
      counterparty_name: tx.counterpartyName || 'Unknown',
      description: tx.description,
      amount_kobo: tx.amountKobo,
      transaction_reference: tx.reference,
    }
    const res = await api.post<{ data: FavouritePayload }>('/favourites', payload)
    return res.data.data
  },

  deleteFavourite: async (id: string): Promise<void> => {
    await api.delete(`/favourites/${id}`)
  }
}
