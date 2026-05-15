import { PageHeader } from '@/components/ui'

export function FraudCasesScreen() {
  return (
    <div className="p-6">
      <PageHeader title="Fraud Cases" subtitle="Active investigations, escalations and resolution history" />
      <p className="text-slate-400 text-sm mt-6">Coming soon — fraud case queue with investigation workflow.</p>
    </div>
  )
}
