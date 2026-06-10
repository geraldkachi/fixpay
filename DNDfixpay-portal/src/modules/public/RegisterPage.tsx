import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod/v4'
import { api } from '@/lib/api'
import keycloak from '@/lib/keycloak'
import { Input, Button } from '@/components/ui'

const schema = z.object({
  businessName: z.string().min(2, 'Business name is required'),
  slug: z
    .string()
    .min(3, 'Min 3 characters')
    .max(30, 'Max 30 characters')
    .regex(/^[a-z0-9-]+$/, 'Only lowercase letters, numbers and hyphens'),
  email:    z.email('Valid email required'),
  password: z.string().min(8, 'Min 8 characters'),
})

type FormData = z.infer<typeof schema>

export function RegisterPage() {
  const navigate = useNavigate()
  const [serverError, setServerError] = useState('')
  const [slugAvailable, setSlugAvailable] = useState<boolean | null>(null)
  const [checkingSlug, setCheckingSlug] = useState(false)

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) })

  const slug = watch('slug')

  async function checkSlug() {
    if (!slug || slug.length < 3) return
    setCheckingSlug(true)
    try {
      const res = await api.post<{ data: { available: boolean } }>('/portal/check-slug', { slug })
      setSlugAvailable(res.data.data.available)
    } catch {
      setSlugAvailable(null)
    } finally {
      setCheckingSlug(false)
    }
  }

  async function onSubmit(data: FormData) {
    setServerError('')
    try {
      await api.post('/portal/register', {
        businessName: data.businessName,
        slug:         data.slug,
        email:        data.email,
        password:     data.password,
      })
      // Auto-login after registration
      await keycloak.login({ loginHint: data.email })
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })
        ?.response?.data?.message
      setServerError(msg ?? 'Registration failed. Please try again.')
    }
  }

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200 w-full max-w-md p-8">
        {/* Logo */}
        <div className="mb-6">
          <Link to="/" className="text-lg font-bold text-slate-900">FixPay</Link>
          <h1 className="text-xl font-semibold text-slate-800 mt-3">Create your developer account</h1>
          <p className="text-sm text-slate-500 mt-1">Start with a free sandbox — no credit card needed.</p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <Input
            label="Business name"
            placeholder="Acme Ltd"
            error={errors.businessName?.message}
            {...register('businessName')}
          />

          <div>
            <Input
              label="Portal slug"
              placeholder="acme"
              error={errors.slug?.message}
              {...register('slug', { onBlur: checkSlug })}
            />
            <div className="mt-1 flex items-center gap-2 text-xs">
              {checkingSlug && <span className="text-slate-400">Checking…</span>}
              {!checkingSlug && slugAvailable === true  && <span className="text-emerald-600">✓ Available</span>}
              {!checkingSlug && slugAvailable === false && <span className="text-red-500">✗ Already taken</span>}
            </div>
          </div>

          <Input
            type="email"
            label="Work email"
            placeholder="you@company.com"
            error={errors.email?.message}
            {...register('email')}
          />

          <Input
            type="password"
            label="Password"
            placeholder="Min 8 characters"
            error={errors.password?.message}
            {...register('password')}
          />

          {serverError && (
            <div className="text-sm text-red-500 bg-red-50 border border-red-200 rounded-lg px-3 py-2">
              {serverError}
            </div>
          )}

          <Button
            type="submit"
            loading={isSubmitting}
            className="w-full"
          >
            Create account
          </Button>
        </form>

        <p className="mt-5 text-sm text-center text-slate-500">
          Already have an account?{' '}
          <button
            onClick={() => keycloak.login()}
            className="text-blue-600 hover:underline"
          >
            Sign in
          </button>
        </p>
      </div>
    </div>
  )
}
