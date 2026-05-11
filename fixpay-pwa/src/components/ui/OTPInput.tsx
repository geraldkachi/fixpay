import { useRef, type ClipboardEvent, type KeyboardEvent } from 'react'
import { cn } from '@/lib/utils'

interface OTPInputProps {
  length?: number
  value: string
  onChange: (val: string) => void
  disabled?: boolean
  autoFocus?: boolean
}

export function OTPInput({ length = 6, value, onChange, disabled, autoFocus }: OTPInputProps) {
  const inputs = useRef<(HTMLInputElement | null)[]>([])

  const focus = (i: number) => inputs.current[i]?.focus()

  const handleChange = (i: number, char: string) => {
    const digit = char.replace(/\D/g, '').slice(-1)
    const arr = value.split('')
    arr[i] = digit
    const next = arr.join('').slice(0, length)
    onChange(next.padEnd(length, ' ').trimEnd().replace(/ /g, ''))
    if (digit && i < length - 1) focus(i + 1)
  }

  const handleKey = (i: number, e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace') {
      const arr = value.split('')
      if (arr[i]) { arr[i] = ''; onChange(arr.join('')) }
      else if (i > 0) { arr[i - 1] = ''; onChange(arr.join('')); focus(i - 1) }
    } else if (e.key === 'ArrowLeft' && i > 0) focus(i - 1)
    else if (e.key === 'ArrowRight' && i < length - 1) focus(i + 1)
  }

  const handlePaste = (e: ClipboardEvent<HTMLInputElement>) => {
    e.preventDefault()
    const text = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, length)
    onChange(text)
    focus(Math.min(text.length, length - 1))
  }

  return (
    <div className="flex gap-3 justify-center">
      {Array.from({ length }).map((_, i) => (
        <input
          key={i}
          ref={el => { inputs.current[i] = el }}
          type="text"
          inputMode="numeric"
          pattern="\d*"
          maxLength={1}
          autoFocus={autoFocus && i === 0}
          value={value[i] ?? ''}
          disabled={disabled}
          className={cn(
            'w-12 h-14 rounded-[12px] text-center text-[22px] font-bold border-2 bg-white outline-none transition-all',
            value[i] ? 'border-brand' : 'border-gray-200',
            'focus:border-brand focus:shadow-[0_0_0_3px_rgba(var(--brand-primary-rgb),0.2)]',
            'disabled:opacity-50'
          )}
          style={{ borderColor: value[i] ? 'var(--brand-primary)' : undefined }}
          onChange={e => handleChange(i, e.target.value)}
          onKeyDown={e => handleKey(i, e)}
          onPaste={handlePaste}
        />
      ))}
    </div>
  )
}
