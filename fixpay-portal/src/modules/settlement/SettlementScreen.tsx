import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { api } from '@/lib/api'
import { PageHeader, Card, Badge, Button, Input } from '@/components/ui'
import { CheckCircleIcon } from '@heroicons/react/24/outline'

interface SettlementAccount {
  id: string
  bankCode: string
  bankName: string
  accountNumber: string
  accountName: string
  currency: string
  verified: boolean
  verifiedAt: string | null
}

type FormData = {
  bankCode: string
  bankName: string
  accountNumber: string
  accountName: string
  currency: string
}

export function SettlementScreen() {
  const qc = useQueryClient()

  const { data: account, isLoading } = useQuery<SettlementAccount | null>({
    queryKey: ['portal-settlement'],
    queryFn: async () => {
      const res = await api.get<{ data?: SettlementAccount | null }>('/portal/settlement')
      return res.data.data ?? null
    },
  })

  const { register, handleSubmit, formState: { isSubmitting, errors } } = useForm<FormData>({
    values: account
      ? {
          bankCode:      account.bankCode,
          bankName:      account.bankName,
          accountNumber: account.accountNumber,
          accountName:   account.accountName,
          currency:      account.currency,
        }
      : { bankCode: '', bankName: '', accountNumber: '', accountName: '', currency: 'NGN' },
  })

  const save = useMutation({
    mutationFn: (d: FormData) => api.put('/portal/settlement', d),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['portal-settlement'] }),
  })

  if (isLoading) return <div className="p-6 text-slate-400 text-sm">Loading…</div>

  return (
    <div className="p-6 max-w-xl">
      <PageHeader
        title="Settlement Account"
        subtitle="Bank account where your payouts will be deposited"
      />

      {account?.verified && (
        <div className="mb-6 flex items-center gap-2 p-3 bg-emerald-50 border border-emerald-200 rounded-xl text-sm text-emerald-700">
          <CheckCircleIcon className="h-5 w-5" />
          Account verified{account.verifiedAt ? ` on ${new Date(account.verifiedAt).toLocaleDateString()}` : ''}.
        </div>
      )}

      {account && !account.verified && (
        <div className="mb-6 p-3 bg-amber-50 border border-amber-200 rounded-xl text-sm text-amber-700">
          Account saved but not yet verified by the FixPay team. Verification happens before first payout.
        </div>
      )}

      <form onSubmit={handleSubmit(d => save.mutate(d))}>
        <Card className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Bank code"
              placeholder="058"
              error={errors.bankCode?.message}
              {...register('bankCode', { required: 'Required' })}
            />
            <Input
              label="Bank name"
              placeholder="Guaranty Trust Bank"
              error={errors.bankName?.message}
              {...register('bankName', { required: 'Required' })}
            />
          </div>
          <Input
            label="Account number"
            placeholder="0123456789"
            maxLength={10}
            error={errors.accountNumber?.message}
            {...register('accountNumber', {
              required: 'Required',
              pattern: { value: /^\d{10}$/, message: '10-digit account number required' },
            })}
          />
          <Input
            label="Account name"
            placeholder="ACME TECHNOLOGIES LTD"
            error={errors.accountName?.message}
            {...register('accountName', { required: 'Required' })}
          />

          <div className="flex items-center justify-between pt-2">
            {account && (
              <div className="flex items-center gap-2">
                <Badge
                  label={account.verified ? 'Verified' : 'Unverified'}
                  variant={account.verified ? 'green' : 'amber'}
                />
              </div>
            )}
            <Button type="submit" loading={isSubmitting || save.isPending} className="ml-auto">
              {account ? 'Update account' : 'Save account'}
            </Button>
          </div>
        </Card>
      </form>
    </div>
  )
}
