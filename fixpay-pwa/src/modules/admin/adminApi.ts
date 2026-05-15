// Admin-specific API helpers

import { api } from '@/lib/api'

// ─── Types ───────────────────────────────────────────────────────────────────

export type FeeType = 'FIXED' | 'PERCENTAGE' | 'TIERED'

export interface ConfigFieldSchema {
  key: string
  label: string
  type: 'text' | 'secret' | 'url' | 'number' | 'boolean' | 'select'
  required: boolean
  placeholder?: string
  description?: string
  options?: string[]
}

export interface ConfigSchema {
  processorId: string
  displayName: string
  category: string
  description: string
  documentationUrl?: string
  fields: ConfigFieldSchema[]
}

export interface PaymentRailConfig {
  id: string
  tenantId: string
  paymentMethod: string
  processorId: string
  priority: number
  enabled: boolean
  maintenance: boolean
  configJson: string
  configSchema: ConfigSchema | null
  createdAt: string
  updatedAt: string
}

export interface ProcessorHealthStatus {
  processorId: string
  cbState: string
  failureRate: number
  totalCalls: number
  isPlugin: boolean
}

export interface FeeSchedule {
  id: string
  feeType: FeeType
  fixedFeeKobo: number
  percentageFee: number
  capKobo: number | null
  minFeeKobo: number
  effectiveFrom: string
  effectiveTo: string | null
  createdAt: string
}

export interface AuditLogEntry {
  id: string
  adminUserId: string
  action: string
  entityType: string
  entityId: string | null
  beforeStateJson: string | null
  afterStateJson: string | null
  ipAddress: string | null
  createdAt: string
}

export interface PluginInfo {
  pluginId: string
  version: string
  state: string
}

export interface SettlementReport {
  implemented: boolean
  message: string
  totalTransactions: number
  totalAmountKobo: number
  totalProcessorFeesKobo: number
  platformRevenueKobo: number
}

// ─── Rail config endpoints ────────────────────────────────────────────────────

export const adminApi = {
  listRails: (tenantId?: string) =>
    api.get<PaymentRailConfig[]>('/admin/rails', { params: tenantId ? { tenantId } : {} })
       .then(r => r.data),

  getRail: (id: string) =>
    api.get<PaymentRailConfig>(`/admin/rails/${id}`).then(r => r.data),

  createRail: (body: {
    tenantId: string
    paymentMethod: string
    processorId: string
    priority: number
    configJson: string
  }) => api.post<PaymentRailConfig>('/admin/rails', body).then(r => r.data),

  updateConfig: (id: string, configJson: string, priority?: number) =>
    api.put<PaymentRailConfig>(`/admin/rails/${id}/config`, { configJson, priority })
       .then(r => r.data),

  updateProcessor: (id: string, processorId: string) =>
    api.put<PaymentRailConfig>(`/admin/rails/${id}/processor`, { processorId })
       .then(r => r.data),

  toggleEnabled: (id: string, enabled: boolean) =>
    api.patch<PaymentRailConfig>(`/admin/rails/${id}/enabled`, { enabled })
       .then(r => r.data),

  setMaintenance: (id: string, maintenance: boolean) =>
    api.patch<PaymentRailConfig>(`/admin/rails/${id}/maintenance`, { maintenance })
       .then(r => r.data),

  deleteRail: (id: string) =>
    api.delete(`/admin/rails/${id}`),

  // ─── Discovery ─────────────────────────────────────────────────────────────

  listProcessorIds: () =>
    api.get<string[]>('/admin/rails/processors').then(r => r.data),

  getProcessorSchema: (processorId: string) =>
    api.get<ConfigSchema>(`/admin/rails/processors/${processorId}/schema`).then(r => r.data),

  getHealth: () =>
    api.get<ProcessorHealthStatus[]>('/admin/rails/health').then(r => r.data),

  // ─── Fee schedule ──────────────────────────────────────────────────────────

  listFees: (railId: string) =>
    api.get<FeeSchedule[]>(`/admin/rails/${railId}/fees`).then(r => r.data),

  addFee: (railId: string, body: {
    feeType: FeeType
    fixedFeeKobo: number
    percentageFee: number
    capKobo?: number | null
    minFeeKobo: number
    effectiveFrom: string
    effectiveTo?: string | null
  }) => api.post<FeeSchedule>(`/admin/rails/${railId}/fees`, body).then(r => r.data),

  deleteFee: (railId: string, feeId: string) =>
    api.delete(`/admin/rails/${railId}/fees/${feeId}`),

  // ─── Plugins ───────────────────────────────────────────────────────────────

  listPlugins: () =>
    api.get<PluginInfo[]>('/admin/plugins').then(r => r.data),

  unloadPlugin: (pluginId: string) =>
    api.delete(`/admin/plugins/${pluginId}`),

  reloadPlugins: () =>
    api.post<{ loaded: number }>('/admin/plugins/reload').then(r => r.data),

  // ─── Audit ─────────────────────────────────────────────────────────────────

  getAuditLog: (page = 0, size = 20) =>
    api.get<{ content: AuditLogEntry[]; totalElements: number }>('/admin/rails/audit', {
      params: { page, size },
    }).then(r => r.data),

  getEntityAuditLog: (railId: string, page = 0, size = 20) =>
    api.get<{ content: AuditLogEntry[]; totalElements: number }>(`/admin/rails/${railId}/audit`, {
      params: { page, size },
    }).then(r => r.data),

  // ─── Settlement ────────────────────────────────────────────────────────────

  getSettlementReport: (tenantId: string, from: string, to: string) =>
    api.get<SettlementReport>('/admin/settlement/report', {
      params: { tenantId, from, to },
    }).then(r => r.data),
}
