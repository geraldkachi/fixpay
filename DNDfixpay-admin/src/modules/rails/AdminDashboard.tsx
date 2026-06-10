import { useState } from 'react'
import { RailsAdminScreen } from './RailsAdminScreen'
import { HealthDashboard }  from './HealthDashboard'
import { AuditLogView }     from './AuditLogView'
import { PluginsPanel }     from './PluginsPanel'
import { PageHeader } from '@/components/ui'
import { cn } from '@/lib/utils'

type Tab = 'rails' | 'health' | 'plugins' | 'audit'

const TABS: { id: Tab; label: string }[] = [
  { id: 'rails',   label: 'Payment Rails' },
  { id: 'health',  label: 'Health' },
  { id: 'plugins', label: 'Plugins' },
  { id: 'audit',   label: 'Audit Log' },
]

export function AdminDashboard() {
  const [tab, setTab] = useState<Tab>('rails')

  return (
    <div className="p-6">
      <PageHeader
        title="Payment Rail Management"
        subtitle="Configure processors, circuit breakers, fees, and hot-loadable plugins"
      />

      {/* Tab bar */}
      <div className="flex gap-0.5 border-b border-slate-200 mb-6">
        {TABS.map(t => (
          <button
            key={t.id}
            onClick={() => setTab(t.id)}
            className={cn(
              'px-4 py-2.5 text-sm font-medium border-b-2 transition-colors -mb-px',
              tab === t.id
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-slate-600 hover:text-slate-900',
            )}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'rails'   && <RailsAdminScreen />}
      {tab === 'health'  && <HealthDashboard />}
      {tab === 'plugins' && <PluginsPanel />}
      {tab === 'audit'   && <AuditLogView />}
    </div>
  )
}
