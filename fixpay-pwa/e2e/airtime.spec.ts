import { test, expect } from '@playwright/test'
import { execSync } from 'node:child_process'

/**
 * Airtime vending E2E test.
 *
 * Uses the standing test user created during live-mode verification:
 *   Phone    : 08099999999
 *   Password : password123
 *   PIN      : 123456
 *   Wallet   : ₦50,000 (auto-funded on OTP verification)
 *   Keycloak : +2348099999999  (realm: customers)
 *
 * Prerequisites (must be running before executing this test):
 *   • Vite dev server  — http://localhost:5173
 *   • Spring Boot API  — http://localhost:8081
 *   • Keycloak         — http://localhost:8180
 *   • PostgreSQL       — localhost:5434
 *   • Redis/Valkey     — localhost:6380
 *
 * VTpass credentials are absent in dev, so payImmediately() falls through
 * to the safe "CFG" fallback — the wallet is still debited and a receipt
 * is still produced.  The test asserts the receipt screen, not a live VTpass
 * transaction ID.
 */

const TEST_PHONE    = '08099999999'  // login phone
const TEST_PASSWORD = 'password123'
const TEST_PIN      = '123456'
const AMOUNT        = 500  // ₦500 quick-select preset

// VTPass sandbox magic numbers — only these phone numbers return success.
// Any other number results in "TRANSACTION FAILED" from the sandbox API.
// See vtpass-api-reference.md § "Sandbox Test Values".
const AIRTIME_PHONE = '08011111111'

/** Reset the test user's wallet to ₦50,000 before each run so the test is repeatable. */
const WALLET_RESET_SQL =
  `UPDATE wallets SET balance = 50000, ledger_balance = 50000 WHERE user_id = 'ddeb22c7-c4fb-4b8b-a12d-fd0131cb6539'`

// ─── Test ─────────────────────────────────────────────────────────────────────

test.beforeEach(() => {
  execSync(
    `docker exec fixpay-postgres psql -U fixpay -d fixpay -c "${WALLET_RESET_SQL}"`,
    { stdio: 'ignore' },
  )
})

test('airtime purchase — MTN ₦500 end-to-end', async ({ page }) => {
  // ── 1. Set live mode and pre-seed Zustand route-guard flags ──────────────
  //
  // main.tsx always calls sessionStorage.removeItem('splash_shown') on every
  // hard page load, so RequireAuth will redirect to /splash on the first
  // protected access.  SplashScreen plays its 3.8 s animation, sets
  // splash_shown, then SPA-navigates to /home.  The test waits for /home
  // before using the Quick Pay grid to SPA-navigate to /payments/airtime —
  // this avoids any further hard page.goto() calls that would re-trigger the
  // main.tsx removal.
  await page.goto('/')
  await page.evaluate(() => {
    localStorage.setItem('fixpay_dev_mode', 'live')
    // pinCreated + kycCompleted pre-seeded so RequireAuth never redirects to
    // /auth/pin or /kyc after the splash, and the Zustand partialise merge
    // keeps them true throughout the SPA session.
    localStorage.setItem('fixpay-auth', JSON.stringify({
      state: {
        user: null,
        isAuthenticated: false,
        pinCreated: true,
        kycCompleted: true,
        pendingPhone: null,
        pendingEmail: null,
      },
      version: 0,
    }))
  })

  // ── 2. Log in via the browser ─────────────────────────────────────────────
  await page.goto('/auth/login')
  await expect(page.getByRole('heading', { name: 'Sign In' })).toBeVisible()

  await page.getByPlaceholder('08012345678').fill(TEST_PHONE)
  await page.getByPlaceholder('Your password').fill(TEST_PASSWORD)
  await page.getByRole('button', { name: 'Sign In' }).click()

  // Login → SPA navigates to /kyc → RequireAuth (splash_shown=null) redirects
  // to /splash → SplashScreen 3.8 s → sets splash_shown → SPA navigates to
  // /home (isAuthenticated=true, pinCreated=true, kycCompleted=true).
  await expect(page).toHaveURL(/\/home/, { timeout: 15_000 })
  await expect(page.getByText('Quick Pay')).toBeVisible()

  // ── 3. Navigate to the Airtime screen via the Quick Pay grid (SPA nav) ───
  //
  // Clicking the card triggers React Router's navigate('/payments/airtime')
  // — no hard page reload, so sessionStorage.splash_shown persists and the
  // in-memory access token stays alive.
  await page.getByRole('button', { name: 'Airtime' }).click()
  await expect(page).toHaveURL(/\/payments\/airtime/, { timeout: 5_000 })
  await expect(page.getByText('Buy Airtime')).toBeVisible()

  // ── 4. Select MTN (it is the default, but click to confirm) ──────────────
  await page.getByRole('button', { name: 'MTN' }).click()

  // ── 5. Enter recipient phone number ────────────────────────────────────────
  // Use the VTPass sandbox success number (08011111111) — any other number
  // returns "TRANSACTION FAILED" from the sandbox API.
  await page.getByPlaceholder('08012345678').fill(AIRTIME_PHONE)

  // ── 6. Pick the ₦500 quick-select preset ─────────────────────────────────
  await page.getByRole('button', { name: `₦${AMOUNT.toLocaleString()}` }).click()

  // ── 7. Submit the form — PIN bottom sheet should slide up ─────────────────
  await page.getByRole('button', { name: 'Continue' }).click()
  await expect(page.getByText('Enter PIN')).toBeVisible({ timeout: 5_000 })

  // ── 8. Register airtime API listener BEFORE entering the PIN ─────────────
  //
  // waitForResponse must be created before the action that triggers the call
  // so Playwright does not miss a fast response.
  const airtimeResponsePromise = page.waitForResponse(
    r => r.url().includes('/api/payments/airtime') && r.request().method() === 'POST',
    { timeout: 30_000 },
  )

  // ── 9. Enter the 6-digit PIN via keyboard ─────────────────────────────────
  //
  // PinPad registers a window.addEventListener('keydown') handler on mount, so
  // keyboard presses are the most reliable way to fill the padcode without
  // worrying about button disambiguation on the page.
  for (const digit of TEST_PIN) {
    await page.keyboard.press(digit)
  }

  // ── 10. Assert the airtime backend call succeeded ─────────────────────────
  //
  // Even with no VTpass credentials the backend records the payment and returns
  // 200 — the VtpassClient falls through to the "CFG" pending fallback.
  const airtimeResponse = await airtimeResponsePromise
  expect(airtimeResponse.status()).toBe(200)

  // ── 11. Receipt screen ─────────────────────────────────────────────────────
  await expect(page).toHaveURL(/\/payments\/receipt/, { timeout: 20_000 })
  await expect(page.getByText('Payment Successful')).toBeVisible()

  // Amount paid — formatCurrency(500 * 100) via en-NG locale → ₦500.00
  await expect(page.getByText(/₦500/, { exact: false })).toBeVisible()

  // Details rows
  await expect(page.getByText('MTN')).toBeVisible()          // network toUpperCase
  await expect(page.getByText(AIRTIME_PHONE)).toBeVisible()  // recipient phone

  console.log('Airtime E2E PASSED — receipt URL:', page.url())
})
