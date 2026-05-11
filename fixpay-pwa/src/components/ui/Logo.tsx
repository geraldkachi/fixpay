interface LogoMarkProps {
  size?: number
}

export function LogoMark({ size = 28 }: LogoMarkProps) {
  const radius = Math.round(size * 0.27)
  const fontSize = Math.round(size * 0.52)
  return (
    <div
      className="flex items-center justify-center shrink-0 select-none"
      style={{ width: size, height: size, borderRadius: radius, background: 'var(--brand-primary)' }}
    >
      <span className="text-white font-black leading-none" style={{ fontSize }}>F</span>
    </div>
  )
}

interface LogoProps {
  size?: 'xs' | 'sm' | 'md' | 'lg'
  /** Show just the icon mark without the wordmark text */
  markOnly?: boolean
}

const SIZES = {
  xs: { icon: 22, text: '14px', gap: '6px' },
  sm: { icon: 28, text: '18px', gap: '8px' },
  md: { icon: 36, text: '22px', gap: '10px' },
  lg: { icon: 52, text: '30px', gap: '12px' },
}

export function Logo({ size = 'md', markOnly = false }: LogoProps) {
  const s = SIZES[size]
  return (
    <div className="flex items-center select-none" style={{ gap: s.gap }}>
      <LogoMark size={s.icon} />
      {!markOnly && (
        <span className="font-black tracking-tight text-gray-900" style={{ fontSize: s.text, lineHeight: 1 }}>
          Fix<span style={{ color: 'var(--brand-primary)' }}>Pay</span>
        </span>
      )}
    </div>
  )
}
