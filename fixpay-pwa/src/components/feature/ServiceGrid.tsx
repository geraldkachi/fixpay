import { useNavigate } from 'react-router-dom'
import { DevicePhoneMobileIcon, SignalIcon, TvIcon, BoltIcon, AcademicCapIcon, ShieldCheckIcon } from '@heroicons/react/24/outline'
import { cn } from '@/lib/utils'

const services = [
  { id: 'airtime',     label: 'Airtime',      Icon: DevicePhoneMobileIcon, path: '/payments/airtime',    color: 'text-orange-500 bg-orange-50' },
  { id: 'data',        label: 'Data',         Icon: SignalIcon,            path: '/payments/data',        color: 'text-blue-500 bg-blue-50' },
  { id: 'tv',          label: 'TV / Cable',   Icon: TvIcon,               path: '/payments/tv',          color: 'text-purple-500 bg-purple-50' },
  { id: 'electricity', label: 'Electricity',  Icon: BoltIcon,             path: '/payments/electricity', color: 'text-yellow-500 bg-yellow-50' },
  { id: 'education',   label: 'Education',    Icon: AcademicCapIcon,      path: '/payments/education',   color: 'text-teal-500 bg-teal-50' },
  { id: 'insurance',   label: 'Insurance',    Icon: ShieldCheckIcon,      path: '/payments/insurance',   color: 'text-green-600 bg-green-50' },
]

interface ServiceGridProps {
  compact?: boolean
}

export function ServiceGrid({ compact }: ServiceGridProps) {
  const navigate = useNavigate()
  return (
    <div className={cn('grid grid-cols-3 gap-3', compact && 'gap-2')}>
      {services.map(({ id, label, Icon, path, color }) => (
        <button
          key={id}
          onClick={() => navigate(path)}
          className="flex flex-col items-center gap-2 p-3 bg-white rounded-[16px] pressable active:bg-gray-50 shadow-sm"
        >
          <div className={cn('w-12 h-12 rounded-full flex items-center justify-center', color)}>
            <Icon className="w-6 h-6" />
          </div>
          <span className="text-[12px] font-medium text-gray-700 text-center leading-tight">{label}</span>
        </button>
      ))}
    </div>
  )
}
