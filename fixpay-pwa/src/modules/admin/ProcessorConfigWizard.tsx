import { useState, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { adminApi, type ConfigFieldSchema, type PaymentRailConfig } from './adminApi'

interface Props {
  rail: PaymentRailConfig
  onClose: () => void
}

/**
 * No-code wizard that:
 * 1. Fetches the ConfigSchema for the current processor
 * 2. Pre-fills fields from the existing configJson
 * 3. Lets the admin switch to a different processor (schema reloads automatically)
 * 4. On save: serialises the form values back to JSON and PUTs to the backend
 */
export function ProcessorConfigWizard({ rail, onClose }: Props) {
  const qc = useQueryClient()

  // Which processor is selected in the wizard (may differ from saved rail)
  const [processorId, setProcessorId] = useState(rail.processorId)
  const [formValues, setFormValues] = useState<Record<string, string>>({})
  const [priority, setPriority] = useState(String(rail.priority))
  const [step, setStep] = useState<'processor' | 'config'>('processor')
  const [saveError, setSaveError] = useState<string | null>(null)

  // All known processor IDs for the dropdown
  const { data: processorIds = [] } = useQuery({
    queryKey: ['admin', 'processorIds'],
    queryFn: adminApi.listProcessorIds,
  })

  // Schema for the currently-selected processor
  const { data: schema, isLoading: schemaLoading } = useQuery({
    queryKey: ['admin', 'schema', processorId],
    queryFn: () => adminApi.getProcessorSchema(processorId),
    enabled: !!processorId && step === 'config',
  })

  // Populate form when schema or existing configJson changes
  useEffect(() => {
    if (!schema) return
    try {
      const existing: Record<string, string> = JSON.parse(rail.configJson || '{}')
      const initial: Record<string, string> = {}
      schema.fields.forEach(f => {
        initial[f.key] = existing[f.key] ?? ''
      })
      setFormValues(initial)
    } catch {
      setFormValues({})
    }
  }, [schema, rail.configJson])

  const saveMutation = useMutation({
    mutationFn: async () => {
      const configJson = JSON.stringify(formValues)
      const p = parseInt(priority, 10)
      // If processor changed, update processor first
      if (processorId !== rail.processorId) {
        await adminApi.updateProcessor(rail.id, processorId)
      }
      return adminApi.updateConfig(rail.id, configJson, isNaN(p) ? undefined : p)
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin', 'rails'] })
      onClose()
    },
    onError: (e: unknown) => {
      setSaveError(e instanceof Error ? e.message : 'Save failed')
    },
  })

  function renderField(field: ConfigFieldSchema) {
    const val = formValues[field.key] ?? ''
    const common = {
      id: field.key,
      value: val,
      placeholder: field.placeholder ?? '',
      required: field.required,
      onChange: (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
        setFormValues(prev => ({ ...prev, [field.key]: e.target.value })),
      className: 'w-full rounded border px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500',
    }

    if (field.type === 'select' && field.options) {
      return (
        <select {...common}>
          <option value="">— select —</option>
          {field.options.map(opt => (
            <option key={opt} value={opt}>{opt}</option>
          ))}
        </select>
      )
    }

    const inputType =
      field.type === 'secret'  ? 'password' :
      field.type === 'number'  ? 'number' :
      field.type === 'boolean' ? 'checkbox' :
      'text'

    if (field.type === 'boolean') {
      return (
        <input
          type="checkbox"
          id={field.key}
          checked={val === 'true'}
          onChange={e => setFormValues(prev => ({ ...prev, [field.key]: String(e.target.checked) }))}
          className="h-4 w-4"
        />
      )
    }

    return <input type={inputType} {...common} />
  }

  // ─── Step 1: choose processor ──────────────────────────────────────────────

  if (step === 'processor') {
    return (
      <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
        <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6 space-y-4">
          <h2 className="text-lg font-semibold">Configure Rail</h2>
          <p className="text-sm text-gray-500">
            Choose the payment processor for this rail. The form will adapt to the
            processor's required settings automatically.
          </p>

          <div className="space-y-1">
            <label className="block text-sm font-medium" htmlFor="proc-select">
              Processor
            </label>
            <select
              id="proc-select"
              value={processorId}
              onChange={e => setProcessorId(e.target.value)}
              className="w-full rounded border px-3 py-2 text-sm"
            >
              <option value="">— select —</option>
              {processorIds.map((id: string) => (
                <option key={id} value={id}>{id}</option>
              ))}
            </select>
          </div>

          <div className="space-y-1">
            <label className="block text-sm font-medium" htmlFor="priority-input">
              Priority (lower = preferred)
            </label>
            <input
              id="priority-input"
              type="number"
              min={1}
              value={priority}
              onChange={e => setPriority(e.target.value)}
              className="w-full rounded border px-3 py-2 text-sm"
            />
          </div>

          <div className="flex gap-2 justify-end pt-2">
            <button onClick={onClose} className="px-4 py-2 text-sm rounded border">Cancel</button>
            <button
              disabled={!processorId}
              onClick={() => setStep('config')}
              className="px-4 py-2 text-sm rounded bg-blue-600 text-white disabled:opacity-50"
            >
              Next: Configure →
            </button>
          </div>
        </div>
      </div>
    )
  }

  // ─── Step 2: fill config fields ────────────────────────────────────────────

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6 space-y-4 max-h-[90vh] overflow-y-auto">
        <button onClick={() => setStep('processor')} className="text-xs text-blue-600 hover:underline">
          ← Back
        </button>
        <h2 className="text-lg font-semibold">{schema?.displayName ?? processorId}</h2>
        {schema?.description && (
          <p className="text-sm text-gray-500">{schema.description}</p>
        )}
        {schema?.documentationUrl && (
          <a
            href={schema.documentationUrl}
            target="_blank"
            rel="noreferrer"
            className="text-xs text-blue-600 hover:underline"
          >
            Documentation ↗
          </a>
        )}

        {schemaLoading && <p className="text-sm text-gray-500">Loading fields…</p>}

        {schema && (
          <div className="space-y-4">
            {schema.fields.map(field => (
              <div key={field.key} className="space-y-1">
                <label htmlFor={field.key} className="block text-sm font-medium">
                  {field.label}
                  {field.required && <span className="text-red-500 ml-1">*</span>}
                </label>
                {field.description && (
                  <p className="text-xs text-gray-500">{field.description}</p>
                )}
                {renderField(field)}
              </div>
            ))}

            {schema.fields.length === 0 && (
              <p className="text-sm text-gray-500 italic">
                This processor requires no additional configuration.
              </p>
            )}
          </div>
        )}

        {saveError && (
          <p className="text-sm text-red-600">{saveError}</p>
        )}

        <div className="flex gap-2 justify-end pt-2">
          <button onClick={onClose} className="px-4 py-2 text-sm rounded border">Cancel</button>
          <button
            disabled={saveMutation.isPending || schemaLoading}
            onClick={() => saveMutation.mutate()}
            className="px-4 py-2 text-sm rounded bg-blue-600 text-white disabled:opacity-50"
          >
            {saveMutation.isPending ? 'Saving…' : 'Save'}
          </button>
        </div>
      </div>
    </div>
  )
}
