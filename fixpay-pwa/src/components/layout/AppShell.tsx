import { Outlet } from 'react-router-dom'
import { BottomNav } from './BottomNav'

export function AppShell() {
  return (
    <div className="relative flex flex-col h-[100dvh] overflow-hidden">
      <main className="flex-1 overflow-y-auto no-scrollbar">
        <Outlet />
      </main>
      <BottomNav />
    </div>
  )
}
