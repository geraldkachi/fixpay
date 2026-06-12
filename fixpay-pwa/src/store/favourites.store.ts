import { create } from 'zustand'
import { favouriteService, type FavouritePayload } from '@/lib/services/favourite.service'
import type { Transaction } from '@/types'

// Map backend Favourite payload to a frontend Transaction-like object for the UI
const mapFavouriteToTransaction = (fav: FavouritePayload): Transaction => ({
  id: fav.id, // Using the backend favourite ID for the UI
  type: fav.type as Transaction['type'],
  amountKobo: fav.amount_kobo || 0,
  feeKobo: 0,
  status: 'completed',
  reference: fav.transaction_reference || fav.id,
  description: fav.description || '',
  serviceId: fav.service_id,
  serviceName: fav.service_name,
  counterpartyName: fav.counterparty_name,
  createdAt: new Date().toISOString(),
})

interface FavouritesState {
  favourites: Transaction[]
  loading: boolean
  fetchFavourites: () => Promise<void>
  addFavourite: (tx: Transaction) => Promise<void>
  removeFavourite: (id: string) => Promise<void>
  isFavourite: (serviceIdOrCounterparty: string) => boolean
}

export const useFavouritesStore = create<FavouritesState>((set, get) => ({
  favourites: [],
  loading: false,

  fetchFavourites: async () => {
    set({ loading: true })
    try {
      const data = await favouriteService.getFavourites()
      set({ favourites: data.map(mapFavouriteToTransaction), loading: false })
    } catch (err) {
      console.error('Failed to fetch favourites', err)
      set({ loading: false })
    }
  },

  addFavourite: async (tx) => {
    // Optimistic UI update
    const tempId = `temp-${Date.now()}`
    const optimisticTx = { ...tx, id: tempId }
    set((state) => ({ favourites: [optimisticTx, ...state.favourites] }))
    
    try {
      const saved = await favouriteService.saveFavourite(tx)
      set((state) => ({
        favourites: state.favourites.map(f => f.id === tempId ? mapFavouriteToTransaction(saved) : f)
      }))
    } catch (err) {
      // Revert optimistic update
      set((state) => ({ favourites: state.favourites.filter(f => f.id !== tempId) }))
      console.error('Failed to save favourite', err)
    }
  },

  removeFavourite: async (id) => {
    const previous = get().favourites
    set((state) => ({ favourites: state.favourites.filter(f => f.id !== id) }))
    
    try {
      await favouriteService.deleteFavourite(id)
    } catch (err) {
      set({ favourites: previous })
      console.error('Failed to remove favourite', err)
    }
  },

  isFavourite: (identifier) => {
    return get().favourites.some(f => 
      f.counterpartyName === identifier || 
      f.serviceId === identifier ||
      f.id === identifier
    )
  },
}))
