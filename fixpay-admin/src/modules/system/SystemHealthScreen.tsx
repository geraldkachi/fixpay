import { PageHeader } from '@/components/ui'

export function SystemHealthScreen() {
  return (
    <div className="p-6">
      <PageHeader title="System Health" subtitle="Service availability, infrastructure metrics and incident feed" />
      <p className="text-slate-400 text-sm mt-6">Coming soon — service status grid and metrics dashboard.</p>
    </div>
  )
}
