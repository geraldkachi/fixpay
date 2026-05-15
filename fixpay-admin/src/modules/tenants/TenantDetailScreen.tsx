import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { PageHeader, Badge } from '@/components/ui'
import { api } from '@/lib/api'
import { ArrowLeftIcon, CheckCircleIcon, XCircleIcon } from '@heroicons/react/24/outline'

// ─── Types ──────────────────────────────────────────────────────────────────

interface TenantDetail {
  id: string
  slug: string
  name: string
  status: 'SANDBOX' | 'ACTIVE' | 'SUSPENDED' | 'OFFBOARDED'
  plan: 'STARTER' | 'GROWTH' | 'ENTERPRISE'
  kybStatus: 'PENDING' | 'IN_REVIEW' | 'APPROVED' | 'REJECTED'
  contactEmail: string | null
  supportEmail: string | null
  supportPhone: string | null
  suspendedAt: string | null
  suspendedReason: string | null
  goLiveRequestedAt: string | null
  wentLiveAt: string | null
  createdAt: string
  updatedAt: string
}

interface KybSubmission {
  id: string
  status: 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED'
  businessName: string | null
  rcNumber: string | null
  tinNumber: string | null
  businessAddress: string | null
  submittedAt: string | null
  reviewedAt: string | null
  reviewNotes: string | null
}

// ─── Status badge helpers ────────────────────────────────────────────────────

const statusVariant: Record<string, 'green' | 'yellow' | 'red' | 'blue' | 'slate'> = {
  SANDBOX:    'blue',
  ACTIVE:     'green',
  SUSPENDED:  'yellow',
  OFFBOARDED: 'red',
}

const kybVariant: Record<string, 'slate' | 'blue' | 'green' | 'red'> = {
  PENDING:   'slate',
  IN_REVIEW: 'blue',
  APPROVED:  'green',
  REJECTED:  'red',
}

// ─── Component ──────────────────────────────────────────────────────────────

export function TenantDetailScreen() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [rejectReason, setRejectReason] = useState('')
  const [showRejectModal, setShowRejectModal] = useState(false)
  const [tab, setTab] = useState<'overview' | 'kyb'>('overview')

  const { data: tenant, isLoading, error } = useQuery<TenantDetail>({
    queryKey: ['tenant', id],
    queryFn: async () => {
      const res = await api.get<{ data: TenantDetail }>(`/admin/tenants/${id}`)
      return res.data.data
    },
    enabled: !!id,
  })

  const { data: kyb } = useQuery<KybSubmission | null>({
    queryKey: ['tenant-kyb', id],
    queryFn: async () => {
      try {
        const res = await api.get<{ data: KybSubmission }>(`/admin/tenants/${id}/kyb`)
        return res.data.data
      } catch {
        return null
      }
    },
    enabled: !!id && tab === 'kyb',
  })

  const approveLive = useMutation({
    mutationFn: () => api.post(`/admin/tenants/${id}/approve-live`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tenant', id] })
      queryClient.invalidateQueries({ queryKey: ['tenants'] })
    },
  })

  const rejectLive = useMutation({
    mutationFn: (reason: string) =>
      api.post(`/admin/tenants/${id}/reject-live`, { reason }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tenant', id] })
      setShowRejectModal(false)
      setRejectReason('')
    },
  })

  if (isLoading) {
    return (
      <div className="p-6">
        <div className="text-slate-400 text-sm">Loading tenant…</div>
      </div>
    )
  }

  if (error || !tenant) {
    return (
      <div className="p-6">
        <div className="text-red-500 text-sm">Failed to load tenant.</div>
      </div>
    )
  }

  const canApproveLive =
    tenant.status === 'SANDBOX' && !!tenant.goLiveRequestedAt

  return (
    <div className="p-6">
      {/* Back + header */}
      <button
        onClick={() => navigate('/tenants')}
        className="flex items-center gap-1 text-sm text-slate-500 hover:text-slate-700 mb-4"
      >
        <ArrowLeftIcon className="h-4 w-4" />
        Back to tenants
      </button>

      <PageHeader
        title={tenant.name}
        subtitle={`@${tenant.slug}`}
        action={
          canApproveLive ? (
            <div className="flex gap-2">
              <button
                onClick={() => approveLive.mutate()}
                disabled={approveLive.isPending}
                className="flex items-center gap-1.5 px-3 py-1.5 bg-emerald-600 text-white text-sm rounded-lg hover:bg-emerald-700 disabled:opacity-50"
              >
                <CheckCircleIcon className="h-4 w-4" />
                {approveLive.isPending ? 'Approving…' : 'Approve Go-Live'}
              </button>
              <button
                onClick={() => setShowRejectModal(true)}
                className="flex items-center gap-1.5 px-3 py-1.5 bg-red-50 text-red-600 text-sm rounded-lg hover:bg-red-100 border border-red-200"
              >
                <XCircleIcon className="h-4 w-4" />
                Reject
              </button>
            </div>
          ) : null
        }
      />

      {/* Status row */}
      <div className="flex items-center gap-3 mb-6">
        <Badge label={tenant.status} variant={statusVariant[tenant.status] ?? 'slate'} />
        <Badge label={`KYB: ${tenant.kybStatus}`} variant={kybVariant[tenant.kybStatus] ?? 'slate'} />
        <Badge label={tenant.plan} variant="purple" />
        {tenant.goLiveRequestedAt && tenant.status !== 'ACTIVE' && (
          <span className="text-xs text-amber-600 bg-amber-50 px-2 py-0.5 rounded-full ring-1 ring-amber-200">
            Go-live requested {new Date(tenant.goLiveRequestedAt).toLocaleDateString()}
          </span>
        )}
      </div>

      {/* Tabs */}
      <div className="flex border-b border-slate-200 mb-6">
        {(['overview', 'kyb'] as const).map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={`px-4 py-2 text-sm font-medium capitalize border-b-2 -mb-px transition-colors ${
              tab === t
                ? 'border-blue-600 text-blue-600'
                : 'border-transparent text-slate-500 hover:text-slate-700'
            }`}
          >
            {t === 'kyb' ? 'KYB Submission' : 'Overview'}
          </button>
        ))}
      </div>

      {tab === 'overview' && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Details card */}
          <div className="bg-white rounded-xl border border-slate-200 p-5">
            <h2 className="text-sm font-semibold text-slate-700 mb-4">Tenant Information</h2>
            <dl className="space-y-3">
              <DetailRow label="Tenant ID" value={tenant.id} mono />
              <DetailRow label="Slug" value={tenant.slug} />
              <DetailRow label="Contact Email" value={tenant.contactEmail ?? '—'} />
              <DetailRow label="Support Email" value={tenant.supportEmail ?? '—'} />
              <DetailRow label="Support Phone" value={tenant.supportPhone ?? '—'} />
              <DetailRow label="Created" value={new Date(tenant.createdAt).toLocaleString()} />
              {tenant.wentLiveAt && (
                <DetailRow label="Went Live" value={new Date(tenant.wentLiveAt).toLocaleString()} />
              )}
              {tenant.suspendedAt && (
                <>
                  <DetailRow label="Suspended At" value={new Date(tenant.suspendedAt).toLocaleString()} />
                  <DetailRow label="Reason" value={tenant.suspendedReason ?? '—'} />
                </>
              )}
            </dl>
          </div>

          {/* Quick actions */}
          <div className="bg-white rounded-xl border border-slate-200 p-5">
            <h2 className="text-sm font-semibold text-slate-700 mb-4">Status Timeline</h2>
            <ol className="relative border-l border-slate-200 ml-3 space-y-4">
              <TimelineItem label="Registered" date={tenant.createdAt} active />
              <TimelineItem
                label="Go-Live Requested"
                date={tenant.goLiveRequestedAt}
                active={!!tenant.goLiveRequestedAt}
              />
              <TimelineItem
                label="Went Live"
                date={tenant.wentLiveAt}
                active={!!tenant.wentLiveAt}
              />
              <TimelineItem
                label="Suspended"
                date={tenant.suspendedAt}
                active={!!tenant.suspendedAt}
                danger
              />
            </ol>
          </div>
        </div>
      )}

      {tab === 'kyb' && (
        <div className="bg-white rounded-xl border border-slate-200 p-5">
          {!kyb ? (
            <p className="text-slate-400 text-sm">No KYB submission found.</p>
          ) : (
            <>
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-sm font-semibold text-slate-700">KYB Submission</h2>
                <Badge label={kyb.status} variant={kybVariant[kyb.status] ?? 'slate'} />
              </div>
              <dl className="space-y-3">
                <DetailRow label="Business Name" value={kyb.businessName ?? '—'} />
                <DetailRow label="RC Number" value={kyb.rcNumber ?? '—'} />
                <DetailRow label="TIN Number" value={kyb.tinNumber ?? '—'} />
                <DetailRow label="Business Address" value={kyb.businessAddress ?? '—'} />
                {kyb.submittedAt && (
                  <DetailRow label="Submitted" value={new Date(kyb.submittedAt).toLocaleString()} />
                )}
                {kyb.reviewedAt && (
                  <DetailRow label="Reviewed" value={new Date(kyb.reviewedAt).toLocaleString()} />
                )}
                {kyb.reviewNotes && (
                  <DetailRow label="Review Notes" value={kyb.reviewNotes} />
                )}
              </dl>
            </>
          )}
        </div>
      )}

      {/* Reject modal */}
      {showRejectModal && (
        <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl p-6 w-full max-w-md shadow-xl">
            <h3 className="text-base font-semibold text-slate-800 mb-3">Reject Go-Live Request</h3>
            <p className="text-sm text-slate-500 mb-4">
              Provide a reason so the tenant can address the issues and re-apply.
            </p>
            <textarea
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              placeholder="e.g. KYB documents incomplete, settlement account not verified…"
              rows={4}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-red-400 resize-none"
            />
            <div className="flex justify-end gap-2 mt-4">
              <button
                onClick={() => { setShowRejectModal(false); setRejectReason('') }}
                className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-lg"
              >
                Cancel
              </button>
              <button
                onClick={() => rejectLive.mutate(rejectReason)}
                disabled={rejectLive.isPending || !rejectReason.trim()}
                className="px-4 py-2 bg-red-600 text-white text-sm rounded-lg hover:bg-red-700 disabled:opacity-50"
              >
                {rejectLive.isPending ? 'Rejecting…' : 'Confirm Reject'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

// ─── Helper sub-components ──────────────────────────────────────────────────

function DetailRow({ label, value, mono = false }: { label: string; value: string; mono?: boolean }) {
  return (
    <div className="flex justify-between gap-4 text-sm">
      <dt className="text-slate-500 shrink-0">{label}</dt>
      <dd className={`text-right text-slate-800 break-all ${mono ? 'font-mono text-xs' : ''}`}>{value}</dd>
    </div>
  )
}

function TimelineItem({
  label,
  date,
  active,
  danger = false,
}: {
  label: string
  date: string | null
  active: boolean
  danger?: boolean
}) {
  return (
    <li className="ml-4 relative">
      <span
        className={`absolute -left-[21px] top-1 h-3 w-3 rounded-full border-2 ${
          active
            ? danger
              ? 'bg-red-400 border-red-200'
              : 'bg-blue-500 border-blue-200'
            : 'bg-slate-200 border-slate-100'
        }`}
      />
      <div className="text-sm font-medium text-slate-700">{label}</div>
      <div className="text-xs text-slate-400">
        {date ? new Date(date).toLocaleString() : 'Not yet'}
      </div>
    </li>
  )
}

