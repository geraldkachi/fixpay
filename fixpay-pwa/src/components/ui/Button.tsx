import { forwardRef, type ButtonHTMLAttributes } from 'react'
import { cn } from '@/lib/utils'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'ghost' | 'destructive' | 'outline'
  size?: 'sm' | 'md' | 'lg'
  loading?: boolean
  fullWidth?: boolean
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ variant = 'primary', size = 'md', loading = false, fullWidth = false, className, children, disabled, style, ...props }, ref) => {
    const base = 'font-semibold inline-flex items-center justify-center gap-2 rounded-[14px] transition-all duration-150 active:scale-[0.97] active:opacity-85 select-none outline-none focus-visible:ring-2 focus-visible:ring-offset-2 disabled:opacity-50 disabled:pointer-events-none'
    const variants = {
      primary:     '',
      secondary:   'bg-ios-gray5 text-gray-900 hover:bg-ios-gray4',
      ghost:       'bg-transparent hover:bg-gray-100',
      destructive: 'bg-ios-red text-white hover:opacity-90',
      outline:     'bg-transparent border border-gray-200 text-gray-800 hover:bg-gray-50',
    }
    const sizes = { sm: 'h-9 px-4 text-[14px]', md: 'h-[52px] px-6 text-[17px]', lg: 'h-14 px-8 text-[17px]' }
    const brandStyle = variant === 'primary' ? { background: 'var(--brand-primary)', color: 'white', ...style } : style

    return (
      <button ref={ref} disabled={disabled || loading} className={cn(base, variants[variant], sizes[size], fullWidth && 'w-full', className)} style={brandStyle} {...props}>
        {loading ? <span className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin-fast" /> : children}
      </button>
    )
  }
)
Button.displayName = 'Button'
