import { create } from 'zustand'

interface TransactionState {
  /** True when a payment or transfer is currently executing */
  isProcessing: boolean
  startProcessing: () => void
  stopProcessing: () => void
}

export const useTransactionStore = create<TransactionState>()((set) => ({
  isProcessing: false,
  startProcessing: () => set({ isProcessing: true }),
  stopProcessing: () => set({ isProcessing: false }),
}))
