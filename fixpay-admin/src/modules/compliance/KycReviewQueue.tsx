import { PageHeader } from '@/components/ui'

export function KycReviewQueue() {
  return (
    <div className="p-6">
      <PageHeader title="KYC Review Queue" subtitle="Pending identity verifications and document reviews" />
      <p className="text-slate-400 text-sm mt-6">Coming soon — KYC queue with approve/reject and escalation.</p>
    </div>
  )
}
