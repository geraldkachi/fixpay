import { PageHeader, Card, Badge } from '@/components/ui'
import { ClipboardDocumentIcon } from '@heroicons/react/24/outline'

function CodeBlock({ code, lang = 'bash' }: { code: string; lang?: string }) {
  return (
    <div className="relative group">
      <pre className="bg-slate-900 text-slate-100 rounded-lg px-4 py-3 text-xs font-mono overflow-x-auto whitespace-pre-wrap">
        <code>{code}</code>
      </pre>
      <button
        onClick={() => void navigator.clipboard.writeText(code)}
        className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity p-1 bg-slate-700 rounded text-slate-300 hover:text-white"
        title="Copy"
      >
        <ClipboardDocumentIcon className="h-4 w-4" />
      </button>
    </div>
  )
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <Card className="mb-5">
      <h2 className="text-sm font-semibold text-slate-700 mb-3">{title}</h2>
      {children}
    </Card>
  )
}

export function IntegrationGuide() {
  return (
    <div className="p-6 max-w-3xl">
      <PageHeader
        title="Integration Guide"
        subtitle="Everything you need to integrate FixPay into your application"
      />

      <Section title="Base URL">
        <p className="text-sm text-slate-600 mb-3">All API requests are made to:</p>
        <CodeBlock code="https://api.fixpay.ng/api/v1" />
        <p className="text-sm text-slate-500 mt-2">Use <code className="text-xs bg-slate-100 px-1 rounded">http://localhost:8081/api/v1</code> for local development.</p>
      </Section>

      <Section title="Authentication">
        <p className="text-sm text-slate-600 mb-3">
          All API requests require your API key in the <code className="text-xs bg-slate-100 px-1 rounded">X-API-Key</code> header:
        </p>
        <CodeBlock lang="bash" code={`curl https://api.fixpay.ng/api/v1/wallets \\
  -H "X-API-Key: fpk_test_your_key_here"`} />
      </Section>

      <Section title="Environments">
        <div className="space-y-2 text-sm text-slate-600">
          <div className="flex items-center gap-2">
            <Badge label="Sandbox" variant="blue" />
            <span>Use <code className="text-xs bg-slate-100 px-1 rounded">fpk_test_</code> keys. Transactions are simulated, no real money moves.</span>
          </div>
          <div className="flex items-center gap-2">
            <Badge label="Live" variant="green" />
            <span>Use <code className="text-xs bg-slate-100 px-1 rounded">fpk_live_</code> keys. Real transactions. Requires Go-Live approval.</span>
          </div>
        </div>
      </Section>

      <Section title="Quick Start — Initiate Airtime Purchase">
        <CodeBlock lang="bash" code={`curl -X POST https://api.fixpay.ng/api/v1/payments/airtime \\
  -H "X-API-Key: fpk_test_your_key_here" \\
  -H "Content-Type: application/json" \\
  -d '{
    "phone": "08012345678",
    "amount": 1000,
    "network": "MTN",
    "reference": "my-unique-ref-001"
  }'`} />
      </Section>

      <Section title="Webhooks — Verifying Signatures">
        <p className="text-sm text-slate-600 mb-3">
          Every webhook delivery includes an <code className="text-xs bg-slate-100 px-1 rounded">X-FixPay-Signature</code> header.
          Verify it with your signing secret:
        </p>
        <CodeBlock lang="javascript" code={`import crypto from 'crypto'

function verifyWebhook(payload, signature, signingSecret) {
  const expected = crypto
    .createHmac('sha256', signingSecret)
    .update(payload)
    .digest('hex')
  return crypto.timingSafeEqual(
    Buffer.from(signature, 'hex'),
    Buffer.from(expected, 'hex')
  )
}`} />
      </Section>

      <Section title="Error Codes">
        <table className="w-full text-sm">
          <thead>
            <tr className="text-xs text-slate-500 border-b border-slate-100">
              <th className="text-left py-2 pr-4">HTTP Status</th>
              <th className="text-left py-2 pr-4">errorCode</th>
              <th className="text-left py-2">Description</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-50">
            {[
              ['401', 'AUTH_001', 'Missing or invalid API key'],
              ['403', 'AUTH_002', 'Insufficient permissions'],
              ['404', 'NOT_FOUND', 'Resource not found'],
              ['409', 'CONFLICT', 'Duplicate resource or state conflict'],
              ['422', 'VALIDATION_ERROR', 'Request body validation failed'],
              ['503', 'SERVICE_UNAVAILABLE', 'Upstream provider unavailable'],
            ].map(([status, code, desc]) => (
              <tr key={code}>
                <td className="py-2 pr-4 font-mono text-slate-700">{status}</td>
                <td className="py-2 pr-4 font-mono text-blue-600">{code}</td>
                <td className="py-2 text-slate-600">{desc}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Section>
    </div>
  )
}
