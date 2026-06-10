import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { api } from '@/lib/api'
import { PageHeader, Card, Badge, Button, Input } from '@/components/ui'
import { CheckCircleIcon } from '@heroicons/react/24/outline'

interface KybData {
  id: string
  status: 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED'
  businessName: string | null
  rcNumber: string | null
  tinNumber: string | null
  businessAddress: string | null
  cacDocumentUrl: string | null
  utilityBillUrl: string | null
  reviewNotes: string | null
  submittedAt: string | null
  reviewedAt: string | null
}

const kybVariant: Record<string, 'slate' | 'blue' | 'green' | 'red' | 'amber'> = {
  DRAFT:     'slate',
  SUBMITTED: 'blue',
  APPROVED:  'green',
  REJECTED:  'red',
}

type FormData = {
  businessName: string
  rcNumber: string
  tinNumber: string
  businessAddress: string
  cacDocumentUrl: string
  utilityBillUrl: string
}

export function KybScreen() {
  const qc = useQueryClient()

  const { data: kyb, isLoading } = useQuery<KybData>({
    queryKey: ['portal-kyb'],
    queryFn: async () => {
      const res = await api.get<{ data: KybData }>('/portal/kyb')
      return res.data.data
    },
  })

  const { register, handleSubmit, formState: { isSubmitting } } = useForm<FormData>({
    values: {
      businessName:   kyb?.businessName    ?? '',
      rcNumber:       kyb?.rcNumber        ?? '',
      tinNumber:      kyb?.tinNumber       ?? '',
      businessAddress: kyb?.businessAddress ?? '',
      cacDocumentUrl: kyb?.cacDocumentUrl  ?? '',
      utilityBillUrl: kyb?.utilityBillUrl  ?? '',
    },
  })

  const saveDraft = useMutation({
    mutationFn: (d: FormData) => api.patch('/portal/kyb', d),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['portal-kyb'] }),
  })

  const submit = useMutation({
    mutationFn: () => api.post('/portal/kyb/submit'),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['portal-kyb'] }),
  })

  if (isLoading || !kyb) return <div className="p-6 text-slate-400 text-sm">Loading…</div>

  const isEditable = kyb.status === 'DRAFT' || kyb.status === 'REJECTED'
  const isApproved = kyb.status === 'APPROVED'

  return (
    <div className="p-6 max-w-2xl">
      <PageHeader
        title="KYB Verification"
        subtitle="Know Your Business — required before going live"
        action={<Badge label={kyb.status} variant={kybVariant[kyb.status] ?? 'slate'} />}
      />

      {isApproved && (
        <div className="mb-6 flex items-center gap-2 p-3 bg-emerald-50 border border-emerald-200 rounded-xl text-sm text-emerald-700">
          <CheckCircleIcon className="h-5 w-5" />
          Your KYB has been approved. You may now request go-live.
        </div>
      )}

      {kyb.status === 'SUBMITTED' && (
        <div className="mb-6 p-3 bg-blue-50 border border-blue-200 rounded-xl text-sm text-blue-700">
          Your KYB submission is under review. We'll notify you at your contact email.
        </div>
      )}

      {kyb.status === 'REJECTED' && kyb.reviewNotes && (
        <div className="mb-6 p-3 bg-red-50 border border-red-200 rounded-xl text-sm text-red-700">
          <strong>Rejection reason:</strong> {kyb.reviewNotes}
        </div>
      )}

      <form
        onSubmit={handleSubmit(d => saveDraft.mutate(d))}
        className="space-y-5"
      >
        <Card>
          <h2 className="text-sm font-semibold text-slate-700 mb-4">Business Details</h2>
          <div className="space-y-4">
            <Input label="Registered business name" {...register('businessName')} disabled={!isEditable} />
            <div className="grid grid-cols-2 gap-4">
              <Input label="RC number (CAC)" placeholder="RC123456" {...register('rcNumber')} disabled={!isEditable} />
              <Input label="TIN number" placeholder="12345678-0001" {...register('tinNumber')} disabled={!isEditable} />
            </div>
            <Input label="Registered address" {...register('businessAddress')} disabled={!isEditable} />
          </div>
        </Card>

        <Card>
          <h2 className="text-sm font-semibold text-slate-700 mb-4">Supporting Documents</h2>
          <div className="space-y-4">
            <Input
              label="CAC Certificate URL"
              placeholder="https://drive.google.com/…"
              {...register('cacDocumentUrl')}
              disabled={!isEditable}
            />
            <Input
              label="Utility Bill URL"
              placeholder="https://drive.google.com/…"
              {...register('utilityBillUrl')}
              disabled={!isEditable}
            />
          </div>
          <p className="text-xs text-slate-400 mt-3">
            Upload documents to Google Drive or any public URL. Documents must be less than 3 months old.
          </p>
        </Card>

        {isEditable && (
          <div className="flex justify-between">
            <Button
              type="submit"
              variant="ghost"
              loading={isSubmitting || saveDraft.isPending}
            >
              Save draft
            </Button>
            <Button
              type="button"
              onClick={() => submit.mutate()}
              loading={submit.isPending}
            >
              Submit for review
            </Button>
          </div>
        )}
      </form>
    </div>
  )
}
