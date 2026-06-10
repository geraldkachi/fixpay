import { cn } from '@/lib/utils'

interface PageHeaderProps {
  title: string
  subtitle?: string
  action?: React.ReactNode
}
export function PageHeader({ title, subtitle, action }: PageHeaderProps) {
  return (
    <div className="flex items-start justify-between mb-6">
      <div>
        <h1 className="text-xl font-semibold text-slate-900">{title}</h1>
        {subtitle && <p className="text-sm text-slate-500 mt-0.5">{subtitle}</p>}
      </div>
      {action && <div className="shrink-0 ml-4">{action}</div>}
    </div>
  )
}

interface BadgeProps {
  label: string
  variant?: 'green' | 'red' | 'yellow' | 'blue' | 'slate' | 'purple' | 'amber'
}
export function Badge({ label, variant = 'slate' }: BadgeProps) {
  const cls: Record<string, string> = {
    green:  'bg-emerald-50 text-emerald-700 ring-emerald-200',
    red:    'bg-red-50 text-red-700 ring-red-200',
    yellow: 'bg-amber-50 text-amber-700 ring-amber-200',
    amber:  'bg-amber-50 text-amber-700 ring-amber-200',
    blue:   'bg-blue-50 text-blue-700 ring-blue-200',
    slate:  'bg-slate-100 text-slate-600 ring-slate-200',
    purple: 'bg-purple-50 text-purple-700 ring-purple-200',
  }
  return (
    <span className={cn('inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ring-1', cls[variant])}>
      {label}
    </span>
  )
}

interface CardProps { children: React.ReactNode; className?: string }
export function Card({ children, className }: CardProps) {
  return (
    <div className={cn('bg-white rounded-xl border border-slate-200 p-5', className)}>
      {children}
    </div>
  )
}

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
}
export function Input({ label, error, className, ...props }: InputProps) {
  return (
    <div>
      {label && <label className="block text-sm font-medium text-slate-700 mb-1.5">{label}</label>}
      <input
        className={cn(
          'w-full px-3 py-2 border rounded-lg text-sm focus:outline-none focus:ring-2',
          error ? 'border-red-300 focus:ring-red-300' : 'border-slate-300 focus:ring-blue-500',
          className,
        )}
        {...props}
      />
      {error && <p className="mt-1 text-xs text-red-500">{error}</p>}
    </div>
  )
}

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'danger' | 'ghost'
  loading?: boolean
}
export function Button({ variant = 'primary', loading, children, className, disabled, ...props }: ButtonProps) {
  const cls: Record<string, string> = {
    primary: 'bg-blue-600 text-white hover:bg-blue-700 disabled:opacity-50',
    danger:  'bg-red-600 text-white hover:bg-red-700 disabled:opacity-50',
    ghost:   'text-slate-600 hover:bg-slate-100 disabled:opacity-50',
  }
  return (
    <button
      disabled={disabled || loading}
      className={cn('px-4 py-2 rounded-lg text-sm font-medium transition-colors', cls[variant], className)}
      {...props}
    >
      {loading ? 'Loading…' : children}
    </button>
  )
}
