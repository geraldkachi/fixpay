import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion, AnimatePresence } from 'motion/react'
import { Button } from '@/components/ui/Button'
import { Logo } from '@/components/ui/Logo'

const slides = [
  { emoji: '💸', title: 'Instant Transfers', body: 'Send money to any Nigerian bank in seconds, day or night.' },
  { emoji: '⚡', title: 'Pay Bills Easily', body: 'Top up airtime, buy data, pay DSTV and electricity from one app.' },
  { emoji: '🔒', title: 'Bank-Grade Security', body: 'Two-factor auth, 6-digit PIN, and biometric protection for every transaction.' },
]

export function WelcomeScreen() {
  const navigate = useNavigate()
  const [slide, setSlide] = useState(0)

  return (
    <div className="h-[100dvh] flex flex-col bg-white">
      {/* Slides */}
      <div className="flex-1 flex flex-col items-center justify-center px-8 pt-safe">
        <div className="mb-8">
          <Logo size="md" />
        </div>
        <AnimatePresence mode="wait">
          <motion.div key={slide} initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -20 }}
            transition={{ duration: 0.3 }} className="flex flex-col items-center text-center gap-6">
            <div className="text-[80px]">{slides[slide].emoji}</div>
            <h2 className="text-[28px] font-black text-gray-900">{slides[slide].title}</h2>
            <p className="text-[16px] text-gray-500 leading-relaxed">{slides[slide].body}</p>
          </motion.div>
        </AnimatePresence>

        {/* Dots */}
        <div className="flex gap-2 mt-10">
          {slides.map((_, i) => (
            <button key={i} onClick={() => setSlide(i)}
              className="h-2 rounded-full transition-all duration-300 pressable"
              style={{ width: i === slide ? 24 : 8, background: i === slide ? 'var(--brand-primary)' : '#D1D5DB' }} />
          ))}
        </div>
      </div>

      {/* CTAs */}
      <div className="px-6 pb-safe flex flex-col gap-3 pb-8">
        <Button fullWidth onClick={() => navigate('/auth/register')}>Get Started</Button>
        <Button fullWidth variant="outline" onClick={() => navigate('/auth/login')}>Sign In</Button>
      </div>
    </div>
  )
}
