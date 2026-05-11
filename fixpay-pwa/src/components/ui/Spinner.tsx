import { cn } from '@/lib/utils'

interface SpinnerProps {
  size?: 'sm' | 'md' | 'lg'
  className?: string
  color?: string
}
export function Spinner({ size = 'md', className, color }: SpinnerProps) {
  const sizes = { sm: 'w-4 h-4 border-2', md: 'w-7 h-7 border-2', lg: 'w-10 h-10 border-3' }
  return (
    <span
      className={cn('rounded-full border-gray-200 animate-spin-fast', sizes[size], className)}
      style={{ borderTopColor: color ?? 'var(--brand-primary)' }}
    />
  )
}

export function FullPageSpinner() {
  return (
    <div className="fixed inset-0 flex items-center justify-center bg-white/80 z-50">
      <Spinner size="lg" />
    </div>
  )
}
