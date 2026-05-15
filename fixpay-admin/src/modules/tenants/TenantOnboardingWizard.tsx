import { PageHeader } from '@/components/ui'

export function TenantOnboardingWizard() {
  return (
    <div className="p-6">
      <PageHeader title="Onboard New Tenant" subtitle="Multi-step: business info → KYB → plan → feature flags → confirm" />
      <p className="text-slate-400 text-sm mt-6">Coming soon — guided onboarding wizard.</p>
    </div>
  )
}
