import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { PageHeader } from '@/components/ui'
import { api } from '@/lib/api'
import { EyeIcon, PencilIcon } from '@heroicons/react/24/outline'

interface Tenant {
  id: string
  slug: string
  name: string
  plan: 'STARTER' | 'GROWTH' | 'ENTERPRISE'
  status: 'ACTIVE' | 'SUSPENDED' | 'OFFBOARDED'
  kyb_status: 'PENDING' | 'IN_REVIEW' | 'APPROVED' | 'REJECTED'
  active: boolean
  created_at: string
}

interface LaravelPaginatedResponse<T> {
  data: T[]
  current_page: number
  last_page: number
  total: number
}

export function TenantListScreen() {
  const navigate = useNavigate()
  const [page, setPage] = useState(1)
  const [statusFilter, setStatusFilter] = useState<string>('')
  const [planFilter, setPlanFilter] = useState<string>('')
  const [searchTerm, setSearchTerm] = useState('')

  const { data, isLoading, error } = useQuery({
    queryKey: ['tenants', page, statusFilter, planFilter, searchTerm],
    queryFn: async () => {
      const params = new URLSearchParams({
        page: page.toString(),
      })
      if (statusFilter) params.append('status', statusFilter)
      if (planFilter) params.append('plan', planFilter)
      if (searchTerm) params.append('search', searchTerm)

      const res = await api.get<LaravelPaginatedResponse<Tenant>>(`/admin/tenants?${params}`)
      return res.data
    },
  })

  const tenants = data?.data ?? []

  const planBadgeColor: Record<string, string> = {
    STARTER: 'bg-blue-100 text-blue-800',
    GROWTH: 'bg-purple-100 text-purple-800',
    ENTERPRISE: 'bg-amber-100 text-amber-800',
  }

  const statusBadgeColor: Record<string, string> = {
    ACTIVE: 'bg-green-100 text-green-800',
    SUSPENDED: 'bg-yellow-100 text-yellow-800',
    OFFBOARDED: 'bg-red-100 text-red-800',
  }

  const kybBadgeColor: Record<string, string> = {
    PENDING: 'bg-slate-100 text-slate-800',
    IN_REVIEW: 'bg-blue-100 text-blue-800',
    APPROVED: 'bg-green-100 text-green-800',
    REJECTED: 'bg-red-100 text-red-800',
  }

  return (
    <div className="p-6 animate-fade-in">
      <PageHeader title="Tenants" subtitle="Manage all registered tenants and their settings" />

      {/* Filters */}
      <div className="mt-6 glass-card p-4 flex gap-4 items-end flex-wrap">
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-2">Search</label>
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => {
              setSearchTerm(e.target.value)
              setPage(1)
            }}
            placeholder="Search by name or slug…"
            className="px-4 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white/50"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-2">Status</label>
          <select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value)
              setPage(1)
            }}
            className="px-4 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white/50"
          >
            <option value="">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="SUSPENDED">Suspended</option>
            <option value="OFFBOARDED">Offboarded</option>
          </select>
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-2">Plan</label>
          <select
            value={planFilter}
            onChange={(e) => {
              setPlanFilter(e.target.value)
              setPage(1)
            }}
            className="px-4 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white/50"
          >
            <option value="">All plans</option>
            <option value="STARTER">Starter</option>
            <option value="GROWTH">Growth</option>
            <option value="ENTERPRISE">Enterprise</option>
          </select>
        </div>
      </div>

      {/* Table */}
      <div className="mt-6 glass-panel overflow-hidden">
        <div className="overflow-x-auto">
          {isLoading ? (
            <div className="p-12 text-center text-slate-500 animate-pulse">Loading tenants…</div>
          ) : error ? (
            <div className="p-12 text-center text-red-600 bg-red-50">Failed to load tenants</div>
          ) : tenants.length === 0 ? (
            <div className="p-12 text-center text-slate-500">No tenants found</div>
          ) : (
            <table className="min-w-full divide-y divide-slate-200/60">
              <thead className="bg-slate-50/50">
                <tr>
                  <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Name</th>
                  <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Plan</th>
                  <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Status</th>
                  <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">KYB</th>
                  <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Created</th>
                  <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200/60">
                {tenants.map((tenant) => (
                  <tr key={tenant.id} className="hover:bg-slate-50/50 transition-colors">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm font-medium text-slate-900">{tenant.name}</div>
                      <div className="text-xs text-slate-500">{tenant.slug}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`px-2 py-1 text-xs font-semibold rounded-full ${planBadgeColor[tenant.plan] || ''}`}>
                        {tenant.plan}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`px-2 py-1 text-xs font-semibold rounded-full ${statusBadgeColor[tenant.status] || ''}`}>
                        {tenant.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`px-2 py-1 text-xs font-semibold rounded-full ${kybBadgeColor[tenant.kyb_status] || kybBadgeColor['PENDING']}`}>
                        {tenant.kyb_status || 'PENDING'}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-500">
                      {tenant.created_at ? new Date(tenant.created_at).toLocaleDateString() : '-'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                      <div className="flex gap-3">
                        <button
                          onClick={() => navigate(`/tenants/${tenant.id}`)}
                          className="text-blue-600 hover:text-blue-800 transition-colors"
                          title="View details"
                        >
                          <EyeIcon className="w-5 h-5" />
                        </button>
                        <button
                          onClick={() => navigate(`/tenants/${tenant.id}`)}
                          className="text-slate-600 hover:text-slate-800 transition-colors"
                          title="Edit"
                        >
                          <PencilIcon className="w-5 h-5" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* Pagination Controls */}
        {data && data.last_page > 1 && (
          <div className="px-6 py-4 border-t border-slate-200/60 bg-slate-50/30 flex justify-between items-center">
            <span className="text-sm text-slate-600 font-medium">
              Page {data.current_page} of {data.last_page}
            </span>
            <div className="flex gap-2">
              <button
                onClick={() => setPage(p => Math.max(1, p - 1))}
                disabled={data.current_page === 1}
                className="px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-200 rounded-lg hover:bg-slate-50 disabled:opacity-50 transition-colors shadow-sm"
              >
                Previous
              </button>
              <button
                onClick={() => setPage(p => p + 1)}
                disabled={data.current_page >= data.last_page}
                className="px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-200 rounded-lg hover:bg-slate-50 disabled:opacity-50 transition-colors shadow-sm"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
