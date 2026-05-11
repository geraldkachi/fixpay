import { useNavigate, useLocation } from 'react-router-dom'
import { CheckCircleIcon } from '@heroicons/react/24/solid'
import { ShareIcon, HomeIcon } from '@heroicons/react/24/outline'
import { formatCurrency, formatDateFull } from '@/lib/utils'
import { Button } from '@/components/ui/Button'

interface ReceiptState {
  type: string
  amount: number
  date: string
  requestId?: string
  // Electricity
  token?: string
  units?: string
  meterType?: string
  meter?: string
  customerName?: string
  provider?: string
  // Education
  pin?: string
  exam?: string
  serviceId?: string
  // Airtime/Data
  phone?: string
  network?: string
  bundle?: string
  // TV
  smartcard?: string
  package?: string
}

export function ReceiptScreen() {
  const navigate = useNavigate()
  const { state } = useLocation()
  const r = state as ReceiptState

  if (!r) { navigate('/home', { replace: true }); return null }

  const isPrepaidElectricity = r.type === 'electricity' && r.meterType === 'prepaid'

  return (
    <div className="flex flex-col h-[100dvh] bg-[#F2F2F7]">
      <div className="flex-1 overflow-y-auto no-scrollbar px-4 pt-safe pb-8">
        {/* Success icon */}
        <div className="flex flex-col items-center pt-10 pb-6 animate-scale-in">
          <div className="w-20 h-20 rounded-full bg-green-100 flex items-center justify-center mb-4">
            <CheckCircleIcon className="w-12 h-12 text-ios-green" />
          </div>
          <h1 className="text-[22px] font-black text-gray-900">Payment Successful</h1>
          <p className="text-[14px] text-gray-500 mt-1">{formatDateFull(r.date)}</p>
        </div>

        {/* Amount */}
        <div className="bg-white rounded-[20px] p-5 mb-4 animate-slide-up text-center">
          <p className="text-[13px] text-gray-400 uppercase tracking-wide mb-1">Amount Paid</p>
          <p className="text-[36px] font-black text-gray-900">{formatCurrency(r.amount * 100)}</p>
        </div>

        {/* ELECTRICITY TOKEN — prominent box */}
        {isPrepaidElectricity && r.token && (
          <div className="rounded-[20px] p-5 mb-4 text-center" style={{ background: 'var(--brand-primary)' }}>
            <p className="text-white/70 text-[12px] uppercase tracking-widest mb-2">⚡ Electricity Token</p>
            <p className="text-white text-[26px] font-black tracking-[4px] font-mono">{r.token}</p>
            {r.units && <p className="text-white/70 text-[13px] mt-2">{r.units}</p>}
            <p className="text-white/50 text-[11px] mt-3">Enter this token on your meter to load your units</p>
          </div>
        )}

        {/* Education PIN */}
        {r.type === 'education' && r.pin && (
          <div className="rounded-[20px] p-5 mb-4 text-center border-2" style={{ borderColor: 'var(--brand-primary)', background: 'rgba(0,122,255,0.04)' }}>
            <p className="text-[12px] uppercase tracking-widest mb-2 font-semibold" style={{ color: 'var(--brand-primary)' }}>🎓 {r.exam ?? 'Exam'} PIN</p>
            <p className="text-[20px] font-black tracking-wide font-mono text-gray-900">{r.pin}</p>
          </div>
        )}

        {/* Details */}
        <div className="bg-white rounded-[20px] overflow-hidden mb-4 animate-slide-up">
          {r.requestId && <Row label="Reference" value={r.requestId} />}
          {r.type === 'airtime' && <>
            <Row label="Network" value={r.network?.toUpperCase() ?? ''} />
            <Row label="Phone" value={r.phone ?? ''} />
          </>}
          {r.type === 'data' && <>
            <Row label="Network" value={r.network?.toUpperCase() ?? ''} />
            <Row label="Bundle" value={r.bundle ?? ''} />
            <Row label="Phone" value={r.phone ?? ''} />
          </>}
          {r.type === 'tv' && <>
            <Row label="Provider" value={(r.provider ?? '').toUpperCase()} />
            <Row label="Customer" value={r.customerName ?? ''} />
            <Row label="Smartcard" value={r.smartcard ?? ''} />
            <Row label="Package" value={r.package ?? ''} />
          </>}
          {r.type === 'electricity' && <>
            <Row label="Provider" value={(r.provider ?? '').replace(/-/g, ' ')} />
            <Row label="Customer" value={r.customerName ?? ''} />
            <Row label="Meter" value={r.meter ?? ''} />
            <Row label="Type" value={r.meterType ?? ''} last={!r.token} />
            {!isPrepaidElectricity && <Row label="Status" value="Payment Posted" last />}
          </>}
          {r.type === 'education' && <>
            <Row label="Exam" value={r.exam ?? ''} last />
          </>}
        </div>
      </div>

      {/* Actions */}
      <div className="px-4 pb-safe flex flex-col gap-3 pb-6 shrink-0">
        <Button variant="outline" fullWidth onClick={() => {}} >
          <ShareIcon className="w-4 h-4" /> Share Receipt
        </Button>
        <Button fullWidth onClick={() => navigate('/home', { replace: true })}>
          <HomeIcon className="w-4 h-4" /> Back to Home
        </Button>
      </div>
    </div>
  )
}

function Row({ label, value, last }: { label: string; value: string; last?: boolean }) {
  return (
    <div className={`flex items-center justify-between px-4 py-3 ${!last ? 'border-b border-black/5' : ''}`}>
      <span className="text-[14px] text-gray-500">{label}</span>
      <span className="text-[14px] font-semibold text-gray-900 text-right max-w-[60%] break-all">{value}</span>
    </div>
  )
}
