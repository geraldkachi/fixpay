/**
 * DevModeToggle — only rendered in `import.meta.env.DEV` builds.
 *
 * Persists the chosen mode in localStorage under `fixpay_dev_mode`:
 *   'mock' → MSW intercepts all API calls (no backend needed)
 *   'live' → requests pass through the Vite proxy to the real backend
 *
 * Changing the mode triggers a full page reload so that main.tsx can
 * (re-)start the MSW worker before React renders.
 */
export function DevModeToggle() {
  if (!import.meta.env.DEV) return null

  const mode = (localStorage.getItem('fixpay_dev_mode') ?? 'mock') as 'mock' | 'live'
  const isMock = mode === 'mock'

  const toggle = () => {
    localStorage.setItem('fixpay_dev_mode', isMock ? 'live' : 'mock')
    window.location.reload()
  }

  return (
    <button
      onClick={toggle}
      title={isMock ? 'Switch to Live backend' : 'Switch to Demo (mock) mode'}
      className="fixed bottom-5 left-4 z-50 flex items-center gap-1.5 rounded-full px-3 py-1 text-[11px] font-bold uppercase tracking-widest shadow-md active:scale-95 transition-transform select-none"
      style={{
        background: isMock ? '#FF9500' : '#34C759',
        color: '#fff',
        letterSpacing: '0.08em',
      }}
    >
      {/* status dot */}
      <span
        className="h-1.5 w-1.5 rounded-full"
        style={{ background: 'rgba(255,255,255,0.75)' }}
      />
      {isMock ? 'Demo' : 'Live'}
    </button>
  )
}
