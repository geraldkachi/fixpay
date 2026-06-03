import { api } from '@/lib/api'
import type { Tenant, ApiKey } from '@/lib/types'

// ── Portal Auth ────────────────────────────────────────────────────────────

export const portalAuthService = {
  register: (payload: { name: string; email: string; password: string; phone: string }) =>
    api.post<{ message: string; tenant_id: string }>('/portal/register', payload),
  login: (payload: { email: string; password: string }) =>
    api.post<{ token: string; tenant: Tenant }>('/auth/login', payload),
}

// ── Portal resources ───────────────────────────────────────────────────────

export function portalService(token: string) {
  const opts = { token }
  return {
    profile: () => api.get<Tenant>('/portal/profile', opts),
    updateProfile: (data: Partial<Tenant>) => api.put<Tenant>('/portal/profile', data, opts),
    requestGoLive: () => api.post<{ message: string }>('/portal/go-live', undefined, opts),
    kyb: (data: Record<string, unknown>) => api.post('/portal/kyb', data, opts),
    apiKeys: () => api.get<ApiKey[]>('/portal/api-keys', opts),
    createApiKey: (name: string, environment: 'sandbox' | 'live') =>
      api.post<ApiKey & { plaintext_key: string }>('/portal/api-keys', { name, environment }, opts),
    revokeApiKey: (id: string) => api.delete<void>(`/portal/api-keys/${id}`, opts),
    webhooks: () => api.get('/portal/webhooks', opts),
    upsertWebhook: (url: string, events: string[]) =>
      api.post('/portal/webhooks', { url, events }, opts),
    settlement: () => api.get('/portal/settlement', opts),
    upsertSettlement: (data: Record<string, unknown>) =>
      api.post('/portal/settlement', data, opts),
  }
}

// ── Admin ──────────────────────────────────────────────────────────────────

export function adminService(token: string) {
  const opts = { token }
  return {
    tenants: (params?: Record<string, string>) => {
      const qs = params ? '?' + new URLSearchParams(params).toString() : ''
      return api.get(`/admin/tenants${qs}`, opts)
    },
    tenant: (id: string) => api.get(`/admin/tenants/${id}`, opts),
    activateTenant: (id: string) => api.post(`/admin/tenants/${id}/activate`, undefined, opts),
    suspendTenant: (id: string) => api.post(`/admin/tenants/${id}/suspend`, undefined, opts),
    paymentRails: () => api.get('/admin/payment-rails', opts),
    createRail: (data: Record<string, unknown>) => api.post('/admin/payment-rails', data, opts),
    updateRail: (id: string, data: Record<string, unknown>) =>
      api.put(`/admin/payment-rails/${id}`, data, opts),
    disputes: (params?: Record<string, string>) => {
      const qs = params ? '?' + new URLSearchParams(params).toString() : ''
      return api.get(`/admin/disputes${qs}`, opts)
    },
    resolveDispute: (id: string, data: { resolution: string; status: string }) =>
      api.post(`/admin/disputes/${id}/resolve`, data, opts),
    complianceUsers: () => api.get('/admin/compliance/users', opts),
    screenUser: (userId: string) => api.post(`/admin/compliance/screen/${userId}`, undefined, opts),
  }
}
