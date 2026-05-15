import { describe, it, expect, vi, beforeEach } from 'vitest'

type RequestHandler = (config: { headers: Record<string, string> }) => Promise<{ headers: Record<string, string> }>
type ResponseErrorHandler = (err: { response?: { status?: number } }) => Promise<never>

let requestHandler: RequestHandler
let responseErrorHandler: ResponseErrorHandler

const mockInstance = {
  interceptors: {
    request: {
      use: vi.fn((handler: RequestHandler) => {
        requestHandler = handler
        return 0
      }),
    },
    response: {
      use: vi.fn((_: unknown, errHandler: ResponseErrorHandler) => {
        responseErrorHandler = errHandler
        return 0
      }),
    },
  },
}

vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => mockInstance),
  },
}))

const keycloakMock = {
  authenticated: false,
  token: 'token-123',
  updateToken: vi.fn(async () => true),
  login: vi.fn(async () => undefined),
}

vi.mock('./keycloak', () => ({
  default: keycloakMock,
}))

describe('api interceptors', () => {
  beforeEach(async () => {
    vi.resetModules()
    keycloakMock.authenticated = false
    keycloakMock.token = 'token-123'
    keycloakMock.updateToken.mockClear()
    keycloakMock.login.mockClear()

    await import('./api')
  })

  it('adds bearer token when authenticated', async () => {
    keycloakMock.authenticated = true

    const config = await requestHandler({ headers: {} })

    expect(keycloakMock.updateToken).toHaveBeenCalledWith(30)
    expect(config.headers.Authorization).toBe('Bearer token-123')
  })

  it('still adds token when refresh throws', async () => {
    keycloakMock.authenticated = true
    keycloakMock.updateToken.mockRejectedValueOnce(new Error('refresh failed'))

    const config = await requestHandler({ headers: {} })

    expect(config.headers.Authorization).toBe('Bearer token-123')
  })

  it('does not add token when unauthenticated', async () => {
    const config = await requestHandler({ headers: {} })

    expect(keycloakMock.updateToken).not.toHaveBeenCalled()
    expect(config.headers.Authorization).toBeUndefined()
  })

  it('triggers keycloak login on 401 response', async () => {
    await expect(responseErrorHandler({ response: { status: 401 } })).rejects.toEqual({ response: { status: 401 } })
    expect(keycloakMock.login).toHaveBeenCalledTimes(1)
  })
})
