import { BellIcon } from '@heroicons/react/24/outline'
import { useAuthStore } from '@/store/auth.store'
import { Logo } from '@/components/ui/Logo'

export function GlobalHeader() {
  const { user } = useAuthStore()

  return (
    <header className="pt-safe px-4 pb-4 bg-[#F2F2F7] flex items-center justify-between shrink-0">
      <Logo size="sm" />
      <div className="flex-1 min-w-0 mx-3 text-right">
        <h1 className="text-[16px] font-semibold text-gray-900 truncate">{user?.firstName}</h1>
      </div>
      <button className="w-10 h-10 rounded-full bg-white flex items-center justify-center shadow-sm pressable shrink-0">
        <BellIcon className="w-5 h-5 text-gray-500" />
      </button>
    </header>
  )
}
