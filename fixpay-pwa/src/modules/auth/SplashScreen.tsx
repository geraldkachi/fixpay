import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'motion/react'
import { useAuthStore } from '@/store/auth.store'
import { sleep } from '@/lib/utils'

const spring = { type: 'spring' as const, damping: 14, stiffness: 220 }
const fade   = (delay: number) => ({ initial: { opacity: 0, y: 12 }, animate: { opacity: 1, y: 0 }, transition: { delay, duration: 0.45, ease: [0.22, 1, 0.36, 1] as [number, number, number, number] } })

export function SplashScreen() {
  const navigate = useNavigate()
  const { isAuthenticated, kycCompleted, pinCreated } = useAuthStore()

  useEffect(() => {
    sleep(2800).then(() => {
      sessionStorage.setItem('splash_shown', '1')
      if (isAuthenticated && kycCompleted && pinCreated) navigate('/home', { replace: true })
      else if (isAuthenticated) navigate('/kyc', { replace: true })
      else navigate('/welcome', { replace: true })
    })
  }, [isAuthenticated, kycCompleted, pinCreated, navigate])

  return (
    <div className="h-[100dvh] flex flex-col items-center justify-center overflow-hidden select-none"
      style={{ background: 'var(--brand-primary)' }}>

      {/* Pulsing ring behind icon */}
      <motion.div
        className="absolute rounded-full"
        style={{ width: 120, height: 120, border: '2px solid rgba(255,255,255,0.25)' }}
        initial={{ scale: 0.8, opacity: 0 }}
        animate={{ scale: [1, 1.6, 1], opacity: [0.5, 0, 0.5] }}
        transition={{ delay: 0.6, duration: 2.2, repeat: Infinity, ease: 'easeInOut' }}
      />
      <motion.div
        className="absolute rounded-full"
        style={{ width: 120, height: 120, border: '2px solid rgba(255,255,255,0.15)' }}
        initial={{ scale: 0.8, opacity: 0 }}
        animate={{ scale: [1, 2.1, 1], opacity: [0.4, 0, 0.4] }}
        transition={{ delay: 1.0, duration: 2.2, repeat: Infinity, ease: 'easeInOut' }}
      />

      {/* Icon box */}
      <motion.div
        className="w-24 h-24 bg-white rounded-[28px] flex items-center justify-center shadow-2xl z-10"
        initial={{ scale: 0, rotate: -12, opacity: 0 }}
        animate={{ scale: 1, rotate: 0, opacity: 1 }}
        transition={{ ...spring, delay: 0.1 }}
      >
        <span className="text-[46px] font-black leading-none" style={{ color: 'var(--brand-primary)' }}>F</span>
      </motion.div>

      {/* Wordmark: "Fix" + "Pay" slide in from opposite sides */}
      <div className="flex items-baseline mt-5 overflow-hidden z-10">
        <motion.span
          className="text-[38px] font-black text-white tracking-tight leading-none"
          initial={{ x: -40, opacity: 0 }}
          animate={{ x: 0, opacity: 1 }}
          transition={{ delay: 0.55, duration: 0.45, ease: [0.22, 1, 0.36, 1] }}
        >Fix</motion.span>
        <motion.span
          className="text-[38px] font-black tracking-tight leading-none"
          style={{ color: 'rgba(255,255,255,0.65)' }}
          initial={{ x: 40, opacity: 0 }}
          animate={{ x: 0, opacity: 1 }}
          transition={{ delay: 0.65, duration: 0.45, ease: [0.22, 1, 0.36, 1] }}
        >Pay</motion.span>
      </div>

      {/* Tagline */}
      <motion.p className="text-white/55 text-[14px] mt-2 z-10" {...fade(1.0)}>
        Payments. Simplified.
      </motion.p>

      {/* Loading dots */}
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 1.3 }}
        className="absolute bottom-16 flex gap-2">
        {[0, 0.18, 0.36].map((d, i) => (
          <motion.div key={i}
            animate={{ opacity: [0.25, 1, 0.25], scale: [0.8, 1.15, 0.8] }}
            transition={{ duration: 1.1, delay: d, repeat: Infinity }}
            className="w-2 h-2 rounded-full bg-white/60"
          />
        ))}
      </motion.div>
    </div>
  )
}
