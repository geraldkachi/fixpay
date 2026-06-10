import { useState, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { railApi, type ConfigFieldSchema, type PaymentRailConfig } from './railApi'

interface Props {
  rail: PaymentRailConfig
  onClose: () => void
}

export function ProcessorConfigWizard({ rail, onClose }: Props) {
  const qc = useQueryClient()
  const [processorId, setProcessorId] = useState(rail.processorId)
  const [formValues, setFormValues] = useState<Record<string, string>>({})
  const [priority, setPriority] = useState(String(rail.priority))
  const [step, setStep] = useState<'processor' | 'config'>('processor')
  const [saveError, setSaveError] = useState<string | null>(null)

  const { data: processorIds = [] } = useQuery({
    queryKey: ['admin', 'processorIds'],
    queryFn: railApi.listProcessorIds,
  })

  const { data: schema, isLoading: schemaLoading } = useQuery({
    queryKey: ['admin', 'schema', processorId],
    queryFn: () => railApi.getProcessorSchema(processorId),
    enabled: !!processorId && step === 'config',
  })

  useEffect(() => {
    if (!schema) return
    try {
      const existing: Record<string, string> = JSON.parse(rail.configJson || '{}')
      const initial: Record<string, string> = {}
      schema.fields.forEach(f => { initial[f.key] = existing[f.key] ?? '' })
      setFormValues(initial)
    } catch { setFormValues({}) }
  }, [schema, rail.configJson])

  const saveMutation = useMutation({
    mutationFn: async () => {
      const configJson = JSON.stringify(formValues)
      const p = parseInt(priority, 10)
      if (processorId !== rail.processorId) await railApi.updateProcessor(rail.id, processorId)
      return railApi.updateConfig(rail.id, configJson, isNaN(p) ? undefined : p)
    },
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['admin', 'rails'] }); onClose() },
    onError: (e: unknown) => setSaveError(e instanceof Error ? e.message : 'Save failed'),
  })

  function renderField(field: ConfigFieldSchema) {
    const val = formValues[field.key] ?? ''
    const base = 'w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500'
    const onChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
      setFormValues(prev => ({ ...prev, [field.key]: e.target.value }))

    if (field.type === 'select' && field.options) {
      return (
        <select value={val} onChange={onChange} className={base}>
          <option value="">— select —</option>
          {field.options.map(opt => <option key={opt} value={opt}>{opt}</option>)}
        </select>
      )
    }
    if (field.type === 'boolean') {
      return (
        <input type="checkbox" id={field.key} checked={val === 'true'}
          onChange={e => setFormValues(prev => ({ ...prev, [field.key]: String(e.target.checked) }))}
          className="h-4 w-4" />
      )
    }
    const inputType = field.type === 'secret' ? 'password' : field.type === 'number' ? 'number' : 'text'
    return <input type={inputType} id={field.key} value={val} placeholder={field.placeholder ?? ''}
      required={field.required} onChange={onChange} className={base} />
  }

  if (step === 'processor') {
    return (
      <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
        <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6 space-y-4">
          <h2 className="text-base font-semibold">Configure Rail — Step 1 of 2</h2>
          <p className="text-sm text-slate-500">
            Choose the payment processor. The configuration form will adapt automatically.
          </p>
          <div className="flex flex-col gap-1">
            <label className="text-xs font-medium text-slate-600">Processor</label>
            <select value={processorId} onChange={e => setProcessorId(e.target.value)}
              className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm bg-white">
              <option value="">— select —</option>
              {(processorIds as string[]).map(id => <option key={id} value={id}>{id}</option>)}
            </select>
          </div>
          <div className="flex flex-col gap-1">
            <label className="text-xs font-medium text-slate-600">Priority (lower = preferred)</label>
            <input type="number" min={1} value={priority} onChange={e => setPriority(e.target.value)}
              className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm" />
          </div>
          <div className="flex gap-2 justify-end pt-2">
            <button onClick={onClose} className="px-4 py-2 text-sm rounded-lg border">Cancel</button>
            <button disabled={!processorId} onClick={() => setStep('config')}
              className="px-4 py-2 text-sm rounded-lg bg-indigo-600 text-white disabled:opacity-50">
              Next: Configure →
            </button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6 space-y-4 max-h-[90vh] overflow-y-auto">
        <button onClick={() => setStep('processor')} className="text-xs text-indigo-600 hover:underline">← Back</button>
        <h2 className="text-base font-semibold">{schema?.displayName ?? processorId}</h2>
        {schema?.description && <p className="text-sm text-slate-500">{schema.description}</p>}
        {schema?.documentationUrl && (
          <a href={schema.documentationUrl} target="_blank" rel="noreferrer"
            className="text-xs text-indigo-600 hover:underline">Documentation ↗</a>
        )}
        {schemaLoading && <p className="text-sm text-slate-500">Loading fields…</p>}
        {schema && (
          <div className="space-y-4">
            {schema.fields.map((field: ConfigFieldSchema) => (
              <div key={field.key} className="space-y-1">
                <label htmlFor={field.key} className="block text-sm font-medium">
                  {field.label}{field.required && <span className="text-red-500 ml-1">*</span>}
                </label>
                {field.description && <p className="text-xs text-slate-500">{field.description}</p>}
                {renderField(field)}
              </div>
            ))}
            {schema.fields.length === 0 && (
              <p className="text-sm text-slate-500 italic">No additional configuration required.</p>
            )}
          </div>
        )}
        {saveError && <p className="text-sm text-red-600">{saveError}</p>}
        <div className="flex gap-2 justify-end pt-2">
          <button onClick={onClose} className="px-4 py-2 text-sm rounded-lg border">Cancel</button>
          <button disabled={saveMutation.isPending || schemaLoading} onClick={() => saveMutation.mutate()}
            className="px-4 py-2 text-sm rounded-lg bg-indigo-600 text-white disabled:opacity-50">
            {saveMutation.isPending ? 'Saving…' : 'Save'}
          </button>
        </div>
      </div>
    </div>
  )
}
