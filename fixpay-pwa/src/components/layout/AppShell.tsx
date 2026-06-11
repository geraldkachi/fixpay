import { Outlet } from 'react-router-dom'
import { BottomNav } from './BottomNav'
import { GlobalHeader } from './GlobalHeader'

export function AppShell() {
  return (
    <div className="relative flex flex-col h-[100dvh] overflow-hidden bg-[#F2F2F7]">
      <GlobalHeader />
      <main className="flex-1 overflow-y-auto no-scrollbar pb-nav">
        <Outlet />
      </main>
      <BottomNav />
    </div>
  )
}
