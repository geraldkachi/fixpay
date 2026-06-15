import { api } from '@/lib/api'
import db from '@/lib/db'

export interface AnalyticsData {
  income_total: number
  expense_total: number
  categories_breakdown: Record<string, number>
  disputes_summary: Record<string, number>
  trend_data: Array<{
    date: string
    income: number
    expense: number
  }>
  period: string
  timestamp: string
}

export const analyticsService = {
  getAnalytics: async (period: string = '30d', since?: string): Promise<AnalyticsData | null> => {
    try {
      const headers: Record<string, string> = {}
      if (since) {
        headers['If-Modified-Since'] = since
      }
      const res = await api.get<{ success?: boolean; data?: AnalyticsData } | AnalyticsData>(
        `/analytics?period=${period}`, 
        { headers, validateStatus: (status) => status >= 200 && status < 400 } // Don't throw on 304
      )

      if (res.status === 304) {
        return null // Not modified, use cache
      }

      const payload = res.data as { success?: boolean; data?: AnalyticsData }
      const data = payload.success !== undefined && payload.data ? payload.data : (res.data as AnalyticsData)
      
      // Cache it
      await db.table('analytics_cache').put({ period, data, timestamp: data.timestamp })
      
      return data
    } catch (error) {
      console.warn('Failed to fetch analytics, falling back to cache', error)
      return null
    }
  },

  getCachedAnalytics: async (period: string = '30d'): Promise<{ data: AnalyticsData, timestamp: string } | null> => {
    try {
      const cached = await db.table('analytics_cache').get(period)
      return cached ? { data: cached.data, timestamp: cached.timestamp } : null
    } catch (e) {
      return null
    }
  }
}
