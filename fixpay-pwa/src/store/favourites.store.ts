import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { Transaction } from '@/types'

interface FavouritesState {
  favourites: Transaction[]
  addFavourite: (tx: Transaction) => void
  removeFavourite: (id: string) => void
  isFavourite: (id: string) => boolean
}

export const useFavouritesStore = create<FavouritesState>()(
  persist(
    (set, get) => ({
      favourites: [],
      addFavourite: (tx) =>
        set((state) => {
          if (state.favourites.some((f) => f.id === tx.id)) return state
          return { favourites: [tx, ...state.favourites] }
        }),
      removeFavourite: (id) =>
        set((state) => ({
          favourites: state.favourites.filter((f) => f.id !== id),
        })),
      isFavourite: (id) => get().favourites.some((f) => f.id === id),
    }),
    {
      name: 'fixpay_favourites',
    }
  )
)
