import { forwardRef, type InputHTMLAttributes } from 'react'
import { cn } from '@/lib/utils'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  hint?: string
  prefix?: string
  suffix?: React.ReactNode
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, hint, prefix, suffix, className, ...props }, ref) => (
    <div className="flex flex-col gap-1">
      {label && <label className="text-[13px] font-medium text-gray-500 uppercase tracking-wide px-1">{label}</label>}
      <div className={cn('flex items-center bg-white rounded-[12px] border transition-colors', error ? 'border-ios-red' : 'border-transparent focus-within:border-blue-400', 'shadow-[0_1px_2px_rgba(0,0,0,0.06)]')}>
        {prefix && <span className="pl-4 pr-1 text-gray-500 text-[17px] select-none whitespace-nowrap">{prefix}</span>}
        <input
          ref={ref}
          className={cn('flex-1 h-[52px] px-4 text-[17px] text-gray-900 bg-transparent outline-none placeholder:text-gray-400', prefix && 'pl-1', suffix && 'pr-1', className)}
          {...props}
        />
        {suffix && <span className="pr-3 text-gray-500">{suffix}</span>}
      </div>
      {error && <p className="text-[13px] text-ios-red px-1">{error}</p>}
      {hint && !error && <p className="text-[13px] text-gray-400 px-1">{hint}</p>}
    </div>
  )
)
Input.displayName = 'Input'
