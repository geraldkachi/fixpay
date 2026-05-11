import { http, HttpResponse, delay } from 'msw'
import { mockTenantConfig } from '../data'

export const tenantHandlers = [
  http.get('/api/tenant/config', async () => {
    await delay(200)
    return HttpResponse.json(mockTenantConfig)
  }),
]
