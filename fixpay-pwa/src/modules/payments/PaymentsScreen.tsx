import { PageHeader } from '@/components/layout/PageHeader'
import { ServiceGrid } from '@/components/feature/ServiceGrid'

export function PaymentsScreen() {
  return (
    <div className="flex flex-col bg-[#F2F2F7] min-h-[100dvh] pb-nav">
      <PageHeader title="Pay Bills" />
      <div className="px-4 pt-4 animate-slide-up">
        <p className="text-[14px] text-gray-500 mb-5">Choose a service to pay for</p>
        <ServiceGrid />
      </div>
    </div>
  )
}
