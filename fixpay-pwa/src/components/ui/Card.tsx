import { cn } from '@/lib/utils'

interface CardProps {
  children: React.ReactNode
  className?: string
  onClick?: () => void
  padding?: boolean
}

export function Card({ children, className, onClick, padding = true }: CardProps) {
  return (
    <div
      className={cn('card', padding && 'p-4', onClick && 'pressable cursor-pointer', className)}
      onClick={onClick}
    >
      {children}
    </div>
  )
}

export function CardSection({ children, className }: { children: React.ReactNode; className?: string }) {
  return <div className={cn('card overflow-hidden', className)}>{children}</div>
}

export function ListItem({ children, onClick, className, last = false }: { children: React.ReactNode; onClick?: () => void; className?: string; last?: boolean }) {
  return (
    <div
      className={cn(
        'ios-sep flex items-center gap-3 bg-white px-4 py-3',
        !last && 'border-b border-black/5',
        onClick && 'pressable cursor-pointer active:bg-gray-50',
        className
      )}
      onClick={onClick}
    >
      {children}
    </div>
  )
}
