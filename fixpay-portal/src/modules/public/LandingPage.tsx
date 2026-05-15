import { useNavigate } from 'react-router-dom'
import { usePortalAuthStore } from '@/store/auth.store'
import keycloak from '@/lib/keycloak'

export function LandingPage() {
  const navigate = useNavigate()
  const { isAuthenticated } = usePortalAuthStore()

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 to-blue-950 flex flex-col">
      {/* Nav */}
      <header className="flex items-center justify-between px-8 py-5">
        <div className="flex items-center gap-2">
          <span className="text-xl font-bold text-white">FixPay</span>
          <span className="text-xs font-medium text-blue-300 bg-blue-900/60 px-2 py-0.5 rounded">Dev Portal</span>
        </div>
        <div className="flex gap-3">
          <button
            onClick={() => navigate('/register')}
            className="px-4 py-2 text-sm text-white/80 hover:text-white transition-colors"
          >
            Register
          </button>
          <button
            onClick={() => isAuthenticated ? navigate('/dashboard') : keycloak.login()}
            className="px-4 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-500 transition-colors"
          >
            {isAuthenticated ? 'Go to Dashboard' : 'Sign In'}
          </button>
        </div>
      </header>

      {/* Hero */}
      <main className="flex-1 flex flex-col items-center justify-center text-center px-6">
        <h1 className="text-5xl font-bold text-white mb-5 leading-tight max-w-2xl">
          Build payments into your product
        </h1>
        <p className="text-lg text-slate-400 max-w-lg mb-10">
          Access the FixPay API in our free sandbox. Go live when you're ready — no vendor lock-in.
        </p>
        <div className="flex gap-4 flex-wrap justify-center">
          <button
            onClick={() => navigate('/register')}
            className="px-6 py-3 bg-blue-600 text-white rounded-lg text-base font-medium hover:bg-blue-500 transition-colors"
          >
            Get Started for Free
          </button>
          <a
            href="https://docs.fixpay.ng"
            target="_blank"
            rel="noopener noreferrer"
            className="px-6 py-3 border border-slate-600 text-slate-300 rounded-lg text-base hover:border-slate-400 transition-colors"
          >
            View Docs
          </a>
        </div>
      </main>

      {/* Features */}
      <section className="grid grid-cols-1 md:grid-cols-3 gap-5 px-8 pb-16 max-w-4xl mx-auto w-full">
        {[
          { title: 'Instant Sandbox', desc: 'Register and get test API keys in seconds. No approval needed to start building.' },
          { title: 'Webhooks & Events', desc: 'Real-time event delivery with signed payloads, retry logic and delivery logs.' },
          { title: 'Go Live in Minutes', desc: 'Complete KYB, verify your settlement account, and flip to production.' },
        ].map(f => (
          <div key={f.title} className="bg-white/5 border border-white/10 rounded-xl p-5">
            <h3 className="text-white font-medium mb-2">{f.title}</h3>
            <p className="text-slate-400 text-sm">{f.desc}</p>
          </div>
        ))}
      </section>
    </div>
  )
}
