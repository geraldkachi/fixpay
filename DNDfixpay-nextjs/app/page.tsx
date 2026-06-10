import Link from 'next/link'

const OPTIONS = [
  {
    href: '/consumer/auth/login',
    title: 'FixPay App',
    subtitle: 'Personal wallet, transfers, and bill payments',
    accent: 'from-emerald-500 to-green-600',
  },
  {
    href: '/portal/auth/login',
    title: 'Business Portal',
    subtitle: 'API keys, KYB, webhooks, and settlement controls',
    accent: 'from-sky-500 to-blue-600',
  },
  {
    href: '/admin/auth/login',
    title: 'Admin Console',
    subtitle: 'Tenant operations, disputes, and compliance reviews',
    accent: 'from-zinc-700 to-black',
  },
]

export default function Home() {
  return (
    <div className="relative min-h-screen overflow-hidden bg-slate-950 text-slate-100">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_20%_20%,rgba(16,185,129,0.22),transparent_36%),radial-gradient(circle_at_80%_0%,rgba(14,165,233,0.2),transparent_32%),radial-gradient(circle_at_50%_100%,rgba(148,163,184,0.16),transparent_40%)]" />
      <main className="relative mx-auto flex min-h-screen w-full max-w-6xl flex-col px-6 py-14 sm:px-10">
        <p className="mb-4 inline-flex w-fit rounded-full border border-slate-700 bg-slate-900/70 px-3 py-1 text-xs tracking-[0.2em] text-slate-300">
          FIXPAY ACCESS
        </p>
        <h1 className="max-w-3xl text-4xl font-bold tracking-tight sm:text-5xl">
          Choose your workspace
        </h1>
        <p className="mt-4 max-w-2xl text-sm text-slate-300 sm:text-base">
          Select where you want to sign in. Each option includes seeded test accounts on the next screen for quick access.
        </p>

        <div className="mt-10 grid gap-5 md:grid-cols-3">
          {OPTIONS.map(option => (
            <Link
              key={option.href}
              href={option.href}
              className="group rounded-2xl border border-slate-700 bg-slate-900/75 p-5 transition hover:-translate-y-0.5 hover:border-slate-500"
            >
              <div className={`inline-flex rounded-full bg-gradient-to-r px-3 py-1 text-xs font-semibold uppercase tracking-wide text-white ${option.accent}`}>
                Open
              </div>
              <h2 className="mt-4 text-xl font-semibold text-white">{option.title}</h2>
              <p className="mt-2 text-sm text-slate-300">{option.subtitle}</p>
              <span className="mt-5 inline-flex text-sm font-medium text-slate-200 transition group-hover:translate-x-1">
                Continue
              </span>
            </Link>
          ))}
        </div>
      </main>
    </div>
  )
}

