import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { api } from '@/lib/api'
import { PageHeader, Card, Button, Input } from '@/components/ui'

interface BrandingData {
  primaryColor: string | null
  secondaryColor: string | null
  accentColor: string | null
  logoUrl: string | null
  faviconUrl: string | null
  supportEmail: string | null
  supportPhone: string | null
}

export function BrandingScreen() {
  const qc = useQueryClient()

  const { data, isLoading } = useQuery<BrandingData>({
    queryKey: ['portal-branding'],
    queryFn: async () => {
      const res = await api.get<{ data: BrandingData }>('/portal/me')
      return res.data.data
    },
  })

  const { register, handleSubmit, formState: { isSubmitting } } = useForm<BrandingData>({
    values: data ?? {
      primaryColor: '#2563EB',
      secondaryColor: '#1E40AF',
      accentColor: '#3B82F6',
      logoUrl: null,
      faviconUrl: null,
      supportEmail: null,
      supportPhone: null,
    },
  })

  const save = useMutation({
    mutationFn: (d: BrandingData) => api.patch('/portal/me/branding', d),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['portal-branding'] }),
  })

  if (isLoading) return <div className="p-6 text-slate-400 text-sm">Loading…</div>

  return (
    <div className="p-6 max-w-2xl">
      <PageHeader
        title="Branding"
        subtitle="Customise your tenant's look and feel for hosted pages"
      />

      <form onSubmit={handleSubmit(d => save.mutate(d))} className="space-y-6">
        <Card>
          <h2 className="text-sm font-semibold text-slate-700 mb-4">Brand Colours</h2>
          <div className="grid grid-cols-3 gap-4">
            <ColorInput label="Primary" name="primaryColor" register={register} />
            <ColorInput label="Secondary" name="secondaryColor" register={register} />
            <ColorInput label="Accent" name="accentColor" register={register} />
          </div>
        </Card>

        <Card>
          <h2 className="text-sm font-semibold text-slate-700 mb-4">Logos</h2>
          <div className="space-y-4">
            <Input label="Logo URL" placeholder="https://cdn.yoursite.com/logo.png" {...register('logoUrl')} />
            <Input label="Favicon URL" placeholder="https://cdn.yoursite.com/favicon.ico" {...register('faviconUrl')} />
          </div>
        </Card>

        <Card>
          <h2 className="text-sm font-semibold text-slate-700 mb-4">Support Contact</h2>
          <div className="space-y-4">
            <Input type="email" label="Support email" placeholder="support@yourcompany.com" {...register('supportEmail')} />
            <Input label="Support phone" placeholder="+2348000000000" {...register('supportPhone')} />
          </div>
        </Card>

        <div className="flex justify-end">
          <Button type="submit" loading={isSubmitting || save.isPending}>Save changes</Button>
        </div>
      </form>
    </div>
  )
}

function ColorInput({
  label,
  name,
  register,
}: {
  label: string
  name: keyof BrandingData
  register: ReturnType<typeof useForm<BrandingData>>['register']
}) {
  return (
    <div>
      <label className="block text-sm font-medium text-slate-700 mb-1.5">{label}</label>
      <div className="flex items-center gap-2">
        <input
          type="color"
          {...register(name)}
          className="h-9 w-9 rounded-lg border border-slate-300 cursor-pointer p-0.5"
        />
        <input
          type="text"
          {...register(name)}
          className="flex-1 px-3 py-2 border border-slate-300 rounded-lg text-sm font-mono focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>
    </div>
  )
}
