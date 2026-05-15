import { test, expect, request } from '@playwright/test'

const TEST_PHONE    = '08011111111'
const TEST_EMAIL    = 'e2e-test@fixpay.local'
const TEST_PASSWORD = 'TestPass123!'
const E164_PHONE    = '+2348011111111'

// ─── Helpers ──────────────────────────────────────────────────────────────────

async function cleanTestUser() {
  // 1. Delete from Postgres via docker exec
  const { execSync } = await import('child_process')
  try {
    execSync(
      `docker exec fixpay-postgres psql -U fixpay -d fixpay -c ` +
      `"DELETE FROM wallets WHERE user_id = (SELECT id FROM users WHERE email = '${TEST_EMAIL}'); ` +
      `DELETE FROM users WHERE email = '${TEST_EMAIL}';"`,
      { stdio: 'pipe' }
    )
  } catch { /* user may not exist yet */ }

  // 2. Delete from Keycloak
  try {
    const api = await request.newContext()
    const tokenRes = await api.post('http://localhost:8180/realms/master/protocol/openid-connect/token', {
      form: {
        grant_type: 'password', client_id: 'admin-cli',
        username: 'admin', password: 'admin_dev_secret',
      },
    })
    const { access_token } = await tokenRes.json()

    const usersRes = await api.get(
      `http://localhost:8180/admin/realms/customers/users?email=${encodeURIComponent(TEST_EMAIL)}&exact=true`,
      { headers: { Authorization: `Bearer ${access_token}` } }
    )
    const users: { id: string }[] = await usersRes.json()
    for (const u of users) {
      await api.delete(`http://localhost:8180/admin/realms/customers/users/${u.id}`, {
        headers: { Authorization: `Bearer ${access_token}` },
      })
    }
    // Also search by phone as username
    const byPhone = await api.get(
      `http://localhost:8180/admin/realms/customers/users?username=${encodeURIComponent(E164_PHONE)}&exact=true`,
      { headers: { Authorization: `Bearer ${access_token}` } }
    )
    const phoneUsers: { id: string }[] = await byPhone.json()
    for (const u of phoneUsers) {
      await api.delete(`http://localhost:8180/admin/realms/customers/users/${u.id}`, {
        headers: { Authorization: `Bearer ${access_token}` },
      })
    }
    await api.dispose()
  } catch { /* ignore */ }
}

async function fetchOtpFromMailHog(email: string): Promise<string> {
  const api = await request.newContext()
  // Poll MailHog for up to 15 seconds
  for (let i = 0; i < 15; i++) {
    const res = await api.get('http://localhost:8025/api/v2/messages')
    const data = await res.json()
    const items: { To: { Mailbox: string; Domain: string }[]; Content: { Body: string } }[] =
      data.items ?? []
    const match = items.find(m =>
      m.To.some(t => `${t.Mailbox}@${t.Domain}`.toLowerCase() === email.toLowerCase())
    )
    if (match) {
      // Body looks like: "Your FixPay verification code is:\n\n  123456\n\nExpires in 10 minutes."
      const body = match.Content.Body
      const otpMatch = body.match(/\b(\d{6})\b/)
      if (otpMatch) {
        await api.dispose()
        return otpMatch[1]
      }
    }
    await new Promise(r => setTimeout(r, 1000))
  }
  await api.dispose()
  throw new Error('OTP not received in MailHog within 15s')
}

async function clearMailHog() {
  const api = await request.newContext()
  await api.delete('http://localhost:8025/api/v1/messages')
  await api.dispose()
}

/** Ensure the fixpay-app Keycloak client exists (idempotent). */
async function ensureKeycloakClient() {
  const api = await request.newContext()
  const tokenRes = await api.post('http://localhost:8180/realms/master/protocol/openid-connect/token', {
    form: {
      grant_type: 'password', client_id: 'admin-cli',
      username: 'admin', password: 'admin_dev_secret',
    },
  })
  const { access_token } = await tokenRes.json()
  const headers = { Authorization: `Bearer ${access_token}` }

  const existing = await api.get(
    'http://localhost:8180/admin/realms/customers/clients?clientId=fixpay-app',
    { headers }
  )
  const list: { id: string }[] = await existing.json()
  if (list.length === 0) {
    await api.post('http://localhost:8180/admin/realms/customers/clients', {
      headers,
      data: {
        clientId: 'fixpay-app',
        publicClient: true,
        directAccessGrantsEnabled: true,
        enabled: true,
        standardFlowEnabled: false,
      },
    })
    console.log('Created Keycloak fixpay-app client')
  }
  await api.dispose()
}

// ─── Test ─────────────────────────────────────────────────────────────────────

test.beforeAll(async () => {
  await ensureKeycloakClient()
  await cleanTestUser()
  await clearMailHog()
})

test('register → verify OTP → login', async ({ page }) => {
  // Switch to LIVE mode (bypass MSW)
  await page.goto('/')
  await page.evaluate(() => {
    localStorage.setItem('fixpay_dev_mode', 'live')
  })

  // ── Step 1: Register ────────────────────────────────────────────────────────
  await page.goto('/auth/register')
  await expect(page.getByText('Create Account')).toBeVisible()

  await page.getByPlaceholder('08012345678').fill(TEST_PHONE)
  await page.getByPlaceholder('you@example.com').fill(TEST_EMAIL)

  const passwordFields = page.getByPlaceholder('At least 8 characters')
  await passwordFields.fill(TEST_PASSWORD)
  await page.getByPlaceholder('Re-enter your password').fill(TEST_PASSWORD)

  // Continue button should be enabled now
  const continueBtn = page.getByRole('button', { name: 'Continue' })
  await expect(continueBtn).toBeEnabled()
  await continueBtn.click()

  // ── Step 2: OTP screen ──────────────────────────────────────────────────────
  await expect(page).toHaveURL(/\/auth\/otp/, { timeout: 15_000 })
  await expect(page.getByText('Verify Email')).toBeVisible()

  // Fetch OTP from MailHog
  const otp = await fetchOtpFromMailHog(TEST_EMAIL)
  console.log(`OTP received: ${otp}`)

  // Paste into first OTP input (handlePaste spreads all digits)
  const firstInput = page.locator('input[inputmode="numeric"]').first()
  await firstInput.click()
  await page.keyboard.insertText(otp) // triggers handlePaste via clipboard simulation

  // Playwright clipboard paste
  await firstInput.evaluate((el, code) => {
    const dt = new DataTransfer()
    dt.setData('text', code)
    el.dispatchEvent(new ClipboardEvent('paste', { clipboardData: dt, bubbles: true }))
  }, otp)

  const verifyBtn = page.getByRole('button', { name: 'Verify' })
  await expect(verifyBtn).toBeVisible()
  await verifyBtn.click()

  // ── Step 3: Redirected to login with verified banner ───────────────────────
  await expect(page).toHaveURL(/\/auth\/login/, { timeout: 15_000 })
  await expect(page.getByText('Email verified')).toBeVisible()

  // ── Step 4: Login ───────────────────────────────────────────────────────────
  await page.getByPlaceholder('08012345678').fill(TEST_PHONE)
  // password field placeholder is "Your password"
  await page.getByPlaceholder('Your password').fill(TEST_PASSWORD)

  const signInBtn = page.getByRole('button', { name: 'Sign In' })
  await signInBtn.click()

  // ── Step 5: Should land on Pin setup, KYC, or Home (depending on account state)
  await expect(page).toHaveURL(/\/(pin|kyc|home)/, { timeout: 20_000 })
  console.log('E2E PASSED — current URL:', page.url())
})
