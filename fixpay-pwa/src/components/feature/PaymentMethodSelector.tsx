import React from 'react'

export type PaymentMethod = 'wallet' | 'payfixy' | 'bank_mandate'

interface PaymentMethodSelectorProps {
  value: PaymentMethod
  onChange: (method: PaymentMethod) => void
  disabled?: boolean
}

const METHODS: { id: PaymentMethod, label: string, icon: string }[] = [
  { id: 'wallet', label: 'Wallet', icon: '💳' },
  { id: 'payfixy', label: 'PayFixy', icon: '🌐' },
  { id: 'bank_mandate', label: 'Bank Mandate', icon: '🏦' },
]

export function PaymentMethodSelector({ value, onChange, disabled }: PaymentMethodSelectorProps) {
  return (
    <div className="mb-4">
      <p className="text-[13px] font-semibold text-gray-500 uppercase tracking-wide mb-2">Payment Method</p>
      <div className="flex flex-col gap-2">
        {METHODS.map(method => {
          const isSelected = value === method.id
          return (
            <button
              key={method.id}
              type="button"
              disabled={disabled}
              onClick={() => onChange(method.id)}
              className={`flex items-center justify-between p-3 rounded-[12px] border-2 transition-all ${disabled ? 'opacity-50 cursor-not-allowed' : 'pressable'}`}
              style={{
                borderColor: isSelected ? 'var(--brand-primary)' : '#E5E5EA',
                backgroundColor: isSelected ? 'rgba(var(--brand-primary-rgb), 0.05)' : '#FFF',
              }}
            >
              <div className="flex items-center gap-3">
                <span className="text-xl">{method.icon}</span>
                <span className="text-[15px] font-semibold text-gray-800">{method.label}</span>
              </div>
              <div className={`w-5 h-5 rounded-full border-2 flex items-center justify-center ${isSelected ? 'border-[var(--brand-primary)]' : 'border-gray-300'}`}>
                {isSelected && <div className="w-2.5 h-2.5 rounded-full bg-[var(--brand-primary)]" />}
              </div>
            </button>
          )
        })}
      </div>
    </div>
  )
}
