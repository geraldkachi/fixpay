import { create } from 'zustand'
import { analyticsService, type AnalyticsData } from '@/lib/services/analytics.service'

interface AnalyticsState {
  data: AnalyticsData | null
  loading: boolean
  error: string | null
  lastSyncTimestamp: string | null
  
  fetchAnalytics: (period?: string, force?: boolean) => Promise<void>
  syncAnalytics: () => Promise<void>
}

export const useAnalyticsStore = create<AnalyticsState>((set, get) => ({
  data: null,
  loading: false,
  error: null,
  lastSyncTimestamp: null,

  fetchAnalytics: async (period = '30d', force = false) => {
    set({ loading: true, error: null })
    
    try {
      const since = force ? undefined : get().lastSyncTimestamp || undefined
      const freshData = await analyticsService.getAnalytics(period, since)
      
      if (freshData) {
        // Network succeeded with fresh data
        set({ data: freshData, lastSyncTimestamp: freshData.timestamp, loading: false })
      } else {
        // Network returned 304 Not Modified, so we safely load the cache
        const cached = await analyticsService.getCachedAnalytics(period)
        if (cached && !get().data) {
          set({ data: cached.data, lastSyncTimestamp: cached.timestamp })
        }
        set({ loading: false })
      }
    } catch (err) {
      // Network failed entirely (offline), fallback to cache
      const cached = await analyticsService.getCachedAnalytics(period)
      if (cached && !get().data) {
        set({ data: cached.data, lastSyncTimestamp: cached.timestamp })
      }
      set({ loading: false, error: 'Failed to fetch analytics, displaying offline data' })
    }
  },

  syncAnalytics: async () => {
    // Explicit trigger for when a transaction/dispute is completed
    const period = get().data?.period || '30d'
    const since = get().lastSyncTimestamp || undefined
    const freshData = await analyticsService.getAnalytics(period, since)
    if (freshData) {
      set({ data: freshData, lastSyncTimestamp: freshData.timestamp })
    }
  }
}))
