import { describe, expect, it, vi, beforeEach } from 'vitest'

const mocks = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  patch: vi.fn(),
  del: vi.fn(),
}))

vi.mock('@/lib/api', () => ({
  api: {
    get: mocks.get,
    post: mocks.post,
    put: mocks.put,
    patch: mocks.patch,
    delete: mocks.del,
  },
}))

import { railApi } from './railApi'

describe('railApi', () => {
  beforeEach(() => {
    Object.values(mocks).forEach(fn => fn.mockClear())
  })

  it('listRails passes tenant filter when provided', async () => {
    mocks.get.mockResolvedValueOnce({ data: [] })

    await railApi.listRails('tenant-123')

    expect(mocks.get).toHaveBeenCalledWith('/admin/rails', { params: { tenantId: 'tenant-123' } })
  })

  it('listRails sends empty params when tenant is missing', async () => {
    mocks.get.mockResolvedValueOnce({ data: [] })

    await railApi.listRails()

    expect(mocks.get).toHaveBeenCalledWith('/admin/rails', { params: {} })
  })

  it('toggleEnabled calls expected endpoint payload', async () => {
    mocks.patch.mockResolvedValueOnce({ data: { enabled: true } })

    await railApi.toggleEnabled('rail-1', true)

    expect(mocks.patch).toHaveBeenCalledWith('/admin/rails/rail-1/enabled', { enabled: true })
  })

  it('addFee posts fee payload and returns data', async () => {
    const fee = { id: 'fee-1', feeType: 'FIXED' }
    mocks.post.mockResolvedValueOnce({ data: fee })

    const result = await railApi.addFee('rail-1', {
      feeType: 'FIXED',
      fixedFeeKobo: 100,
      percentageFee: 0,
      capKobo: null,
      minFeeKobo: 0,
      effectiveFrom: '2026-01-01',
      effectiveTo: null,
    })

    expect(mocks.post).toHaveBeenCalledWith('/admin/rails/rail-1/fees', {
      feeType: 'FIXED',
      fixedFeeKobo: 100,
      percentageFee: 0,
      capKobo: null,
      minFeeKobo: 0,
      effectiveFrom: '2026-01-01',
      effectiveTo: null,
    })
    expect(result).toEqual(fee)
  })

  it('deleteRail calls delete endpoint', async () => {
    mocks.del.mockResolvedValueOnce({ data: null })

    await railApi.deleteRail('rail-1')

    expect(mocks.del).toHaveBeenCalledWith('/admin/rails/rail-1')
  })
})
