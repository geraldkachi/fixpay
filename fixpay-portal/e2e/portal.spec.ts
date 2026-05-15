import { test, expect, request } from '@playwright/test'

// ─── Test account (acme-approve-0515 tenant) ─────────────────────────────────
// Tenant UUID : 674920b3-934b-4271-ae4f-7546ccc5f75f
// Status      : ACTIVE (go-live approved)
// KYB Status  : APPROVED
// LIVE key    : fpk_live_60e6… (created by setup)

const TEST_USER     = 'owner+approve0515@acme-demo.test'
const TEST_PASSWORD = 'Tenant@Dev12345'
const KC_URL        = 'http://localhost:8180'

// ─── Helper: log in through Keycloak ─────────────────────────────────────────

async function loginViaKeycloak(page: import('@playwright/test').Page) {
  // Navigate to portal home; the Sign In button redirects to Keycloak
  await page.goto('/')

  // Wait for the public landing page
  await expect(page.getByRole('button', { name: /sign in/i })).toBeVisible()
  await page.getByRole('button', { name: /sign in/i }).click()

  // Keycloak sign-in page
  await page.waitForURL(`${KC_URL}/**`, { timeout: 15_000 })
  await page.fill('#username', TEST_USER)
  await page.fill('#password', TEST_PASSWORD)
  await page.locator('#kc-login').click()

  // Redirect back to portal
  await page.waitForURL('http://localhost:3001/**', { timeout: 15_000 })
}

// ─── Helper: ensure LIVE API key exists (idempotent) ─────────────────────────

async function ensureLiveApiKey() {
  const api = await request.newContext()

  // Get portal user token
  const tokenRes = await api.post(
    `${KC_URL}/realms/tenants/protocol/openid-connect/token`,
    {
      form: {
        grant_type: 'password',
        client_id:  'fixpay-tenant-portal',
        username:   TEST_USER,
        password:   TEST_PASSWORD,
      },
    }
  )
  const { access_token } = await tokenRes.json()

  // Check existing LIVE keys
  const keysRes = await api.get('http://localhost:8081/api/portal/api-keys?environment=LIVE', {
    headers: { Authorization: `Bearer ${access_token}` },
  })
  const { data: keys } = await keysRes.json()
  const active = (keys as { revokedAt: string | null }[]).filter(k => !k.revokedAt)

  if (active.length === 0) {
    await api.post('http://localhost:8081/api/portal/api-keys', {
      headers: { Authorization: `Bearer ${access_token}`, 'Content-Type': 'application/json' },
      data: { name: 'E2E Live Key', environment: 'LIVE', scopes: [] },
    })
  }
  await api.dispose()
}

// ─── Tests ────────────────────────────────────────────────────────────────────

test.describe('Portal public pages', () => {
  test('landing page shows marketing CTAs', async ({ page }) => {
    await page.goto('/')
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
    await expect(page.getByRole('button', { name: /get started/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /sign in/i })).toBeVisible()
  })

  test('register link navigates to /register', async ({ page }) => {
    await page.goto('/')
    await page.getByRole('button', { name: /get started/i }).click()
    await page.waitForURL('**/register')
    await expect(page.getByRole('heading', { name: /create.*account/i })).toBeVisible()
  })
})

test.describe('Portal authenticated flow', () => {
  test.beforeEach(async ({ page }) => {
    await loginViaKeycloak(page)
  })

  test('redirects to /dashboard after login', async ({ page }) => {
    await page.waitForURL('**/dashboard', { timeout: 10_000 })
    await expect(page.getByText(/overview|dashboard/i).first()).toBeVisible()
  })

  test('KYB screen loads and shows form fields', async ({ page }) => {
    await page.goto('/kyb')
    await expect(page.getByRole('heading', { name: /KYB/i })).toBeVisible()
    // Form fields
    await expect(page.locator('input[name="businessName"], input[placeholder*="name" i]').first()).toBeVisible()
    await expect(page.locator('input[name="rcNumber"], input[placeholder*="RC" i]').first()).toBeVisible()
  })

  test('KYB save draft updates the form without error', async ({ page }) => {
    await page.goto('/kyb')
    await page.waitForLoadState('networkidle')

    const bizNameInput = page.locator('input[name="businessName"]').first()
    await bizNameInput.clear()
    await bizNameInput.fill('E2E Test Company Ltd')

    // Click Save Draft
    const saveBtn = page.getByRole('button', { name: /save draft/i })
    await expect(saveBtn).toBeEnabled()
    await saveBtn.click()

    // No error toast / error state
    await expect(page.getByText(/error|failed/i)).not.toBeVisible({ timeout: 3_000 })
  })

  test('Settlement screen loads', async ({ page }) => {
    await page.goto('/settlement')
    await expect(page.getByRole('heading', { name: /settlement/i })).toBeVisible()
  })

  test('API keys screen lists keys', async ({ page }) => {
    await page.goto('/api-keys')
    await expect(page.getByRole('heading', { name: /api key/i })).toBeVisible()
    await page.waitForLoadState('networkidle')
    // Should show at least one key (created in setup)
    await expect(page.locator('table, [data-testid="key-row"], .key-row').first()).toBeVisible({ timeout: 8_000 })
  })
})

test.describe('Go-live checklist', () => {
  test.beforeAll(async () => {
    await ensureLiveApiKey()
  })

  test.beforeEach(async ({ page }) => {
    await loginViaKeycloak(page)
  })

  test('go-live page shows KYB approved badge', async ({ page }) => {
    await page.goto('/go-live')
    await page.waitForLoadState('networkidle')
    // KYB item should show a ✓ / checked state
    const kybItem = page.locator('text=KYB Approved').first()
    await expect(kybItem).toBeVisible()
    // The enclosing checklist item should NOT contain an ✗ indicator
    await expect(page.locator('text=KYB Approved').locator('..').locator('[aria-label*="fail"], .text-red')).not.toBeVisible()
  })

  test('go-live page shows active live status when tenant is ACTIVE', async ({ page }) => {
    await page.goto('/go-live')
    await page.waitForLoadState('networkidle')
    // Status badge or heading should reflect ACTIVE / live
    const activeBadge = page.locator('text=/live|active/i').first()
    await expect(activeBadge).toBeVisible({ timeout: 8_000 })
  })
})
