import { create } from 'zustand'

interface DuplicatePaymentState {
  isOpen: boolean
  message: string
  resolvePromise: ((proceed: boolean) => void) | null
  showWarning: (message: string) => Promise<boolean>
  proceed: () => void
  cancel: () => void
}

export const useDuplicatePaymentStore = create<DuplicatePaymentState>((set) => ({
  isOpen: false,
  message: '',
  resolvePromise: null,
  showWarning: (message) => {
    return new Promise<boolean>((resolve) => {
      set({ isOpen: true, message, resolvePromise: resolve })
    })
  },
  proceed: () => set((state) => {
    state.resolvePromise?.(true)
    return { isOpen: false, resolvePromise: null }
  }),
  cancel: () => set((state) => {
    state.resolvePromise?.(false)
    return { isOpen: false, resolvePromise: null }
  })
}))
