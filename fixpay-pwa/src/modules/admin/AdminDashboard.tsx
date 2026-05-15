import { useState } from 'react'
import { RailsAdminScreen } from './RailsAdminScreen'
import { HealthDashboard }  from './HealthDashboard'
import { AuditLogView }     from './AuditLogView'
import { PluginsPanel }     from './PluginsPanel'

type Tab = 'rails' | 'health' | 'audit' | 'plugins'

const TABS: { id: Tab; label: string }[] = [
  { id: 'rails',   label: 'Rails' },
  { id: 'health',  label: 'Health' },
  { id: 'plugins', label: 'Plugins' },
  { id: 'audit',   label: 'Audit Log' },
]

export function AdminDashboard() {
  const [tab, setTab] = useState<Tab>('rails')

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white border-b px-4 py-3">
        <h1 className="text-base font-semibold">Platform Admin</h1>
        <p className="text-xs text-gray-500">Payment Rail Management</p>
      </header>

      {/* Tab bar */}
      <nav className="bg-white border-b flex">
        {TABS.map(t => (
          <button
            key={t.id}
            onClick={() => setTab(t.id)}
            className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
              tab === t.id
                ? 'border-blue-600 text-blue-600'
                : 'border-transparent text-gray-600 hover:text-gray-900'
            }`}
          >
            {t.label}
          </button>
        ))}
      </nav>

      {/* Content */}
      <main className="max-w-3xl mx-auto">
        {tab === 'rails'   && <RailsAdminScreen />}
        {tab === 'health'  && <HealthDashboard />}
        {tab === 'plugins' && <PluginsPanel />}
        {tab === 'audit'   && (
          <div className="p-4">
            <AuditLogView />
          </div>
        )}
      </main>
    </div>
  )
}
