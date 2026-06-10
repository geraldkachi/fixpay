# FixPay Mobile & Laravel Architecture Audit & Compliance Report

This document reviews the codebase of **FixPay Mobile PWA** and the **FixPay Laravel Backend** relative to the project guidelines, technical constraints, security protocols, and operational rules defined in [.antigravityrules](file:///c:/Users/kolugbenga/fixpay-mobile/.antigravityrules).

---

## 1. Executive Summary & App Overview

FixPay is a multi-tenant fintech platform designed to facilitate secure digital payments in Nigeria. It supports wallet transfers, bill payments (airtime, data, TV, electricity, education, insurance) via **VTpass**, and bank transfers via **Paystack**. It also incorporates identity verification processes (KYC) for Bank Verification Numbers (BVN), National Identification Numbers (NIN), and Corporate Affairs Commission (CAC) registration.

### Directory Analysis
- **`fixpay-pwa/`**: A progressive web application built with **React**, **TypeScript**, **Vite**, **Zustand** (state management), **React Query** (data fetching), and **TailwindCSS**. It utilizes **MSW (Mock Service Worker)** for development/mocking.
- **`fixpay-laravel/`**: The core API backend built with **Laravel**. It handles customer authentication (Sanctum), virtual accounts (Providus adapters), identity verification (Prembly adapters), transfers (Paystack), and bill payments (VTpass).
- **`specs/`**: The specifications directory storing API references, test scripts, sandbox details, and implementation plans.
- **`DND*` directories** (`DNDfixpay-backend`, `DNDfixpay-portal`, etc.): Decommissioned or archived source code (specifically a Spring Boot Java/Kotlin implementation). They serve only as a design reference and are **not** part of the active production runtime.

---

## 2. Rule Compliance Matrix

Below is a detailed audit of each rule defined in [.antigravityrules](file:///c:/Users/kolugbenga/fixpay-mobile/.antigravityrules) against the active implementations in `fixpay-pwa` and `fixpay-laravel`.

### 2.1 Progressive Web App (PWA) Constraints

| Rule ID | Rule Requirement | Current Implementation | Compliance Status |
| :--- | :--- | :--- | :--- |
| **`pwa_caching_boundaries`** | Strict caching limits. Precache shell elements only. Forbid caching dynamic media, large datasets, or non-critical dependencies. | In `vite.config.ts`, Workbox is configured to cache JS, CSS, HTML, and core icons. It has a runtime cache for `/api/` using `NetworkFirst` with a limit of 100 entries and a 300-second expiration. | **PARTIALLY COMPLIANT**<br>API caching on `/api/` should be strictly auditted to prevent caching of dynamic ledger balance endpoints or account statistics. |
| **`pwa_installability_compliance`** | Valid `manifest.json`, `display: "standalone"`, `start_url`, high-res icons, and functional `fetch` handler in Service Worker. | `vite.config.ts` has a fully configured PWA manifest with `display: 'standalone'` and `start_url: '/'`. High-resolution icons (192px and 512px) are defined. | **COMPLIANT** |

---

### 2.2 Frontend State & Transaction Integrity

| Rule ID | Rule Requirement | Current Implementation | Compliance Status |
| :--- | :--- | :--- | :--- |
| **`zustand_freeze_and_poll_strategy`** | No optimistic updates for wallet balances, transfers, or bill payments. Zustand actions must trigger a UI 'loading/frozen' state. Force the UI to remain interactive-locked while polling/listening. | Zustand stores (`auth.store.ts`, `tenant.store.ts`) do **not** manage wallet balances or payment transaction states. All screens (e.g., `ElectricityScreen.tsx`) use component-local state and fetch balances directly from React Query. | **NON-COMPLIANT**<br>No central Zustand lock or interactive-freezing state is implemented. |
| **`prevent_double_submit_ui`** | Button-disabling on transaction submit. Submission components must check for an active `isProcessing` flag from the Zustand store. | Submit screens use a local `submitting` state to control loading spinner and disable buttons. No Zustand-level `isProcessing` flag is referenced or check evaluated. | **NON-COMPLIANT**<br>Missing Zustand-scoped transaction locking mechanism. |

---

### 2.3 Backend Engineering & Ledger Safety (Laravel)

| Rule ID | Rule Requirement | Current Implementation | Compliance Status |
| :--- | :--- | :--- | :--- |
| **`laravel_idempotency_enforcement`** | Mandate an `X-Idempotency-Key` header check for all POST/PUT routes touching wallets/VTPass. Use Cache or DB locks to atomically intercept duplicate requests. Return `409 Conflict` or cached response. | `VtpassPaymentController` and `TransferController` validate an `idempotency_key` string inside the request payload body (not the HTTP headers). They check if the row exists in the DB. If it does, they return it, which triggers a re-run of execution workflows (submitting to processor) rather than short-circuiting with a 409 or cached response. | **CRITICAL NON-COMPLIANCE**<br>No header-based checking is used. Lacks atomic cache/database locking, introducing race conditions where concurrent requests charge the customer twice. |
| **`precise_financial_math`** | Forbid standard PHP floating-point operators (`+`, `-`, `*`, `/`) for money. Mandate `BCMath` extension or dedicated library (e.g. `Brick\Money`). | `WalletService.php` uses integer arithmetic (`-`, `+`) for kobo balances, which is safe. However, `ProcessorFeeSchedule.php` uses float casting and arithmetic: `round($amountKobo * (float) $this->percentage_fee)`. | **NON-COMPLIANT**<br>Float math is present in fee schedules, and `BCMath` is not utilized. |
| **`environment_variable_isolation`** | Forbid hardcoding credentials. Enforce reference bindings through platform environment variables (`env()`). | All third-party secrets (VTpass keys, Paystack keys, Providus secrets) are decoupled from service files and referenced via environment variables in `config/services.php`. | **COMPLIANT** |
| **`architectural_boundary_enforcement`** | Prevent leakage of backend execution logic, env parameters, or DB clients into client-side bundles. | Frontend uses isolated Axios calls to `/api/*` and does not embed any DB models or server parameters. | **COMPLIANT** |
| **`injection_prevention`** | Reject raw SQL concatenation. Utilize parameterized queries or ORM. | The Laravel backend uses Eloquent ORM and Query Builder parameterized wrappers exclusively. | **COMPLIANT** |

---

### 2.4 Card Tokenization & Gateway Security (PCI-DSS)

| Rule ID | Rule Requirement | Current Implementation | Compliance Status |
| :--- | :--- | :--- | :--- |
| **`pci_dss_iframe_isolation`** | Zero-trust on raw PAN data (card numbers, CVVs, expiration dates). Force capture via hosted fields/SDKs/iframes. Store only token. | Currently, the PWA does not capture card details; payments are simulated or run via bank transfer/wallet rails. Biller screens collect only smartcard or meter numbers. | **COMPLIANT**<br>(By omission — card payment forms are not yet built in PWA). |
| **`secure_token_reuse_storage`** | Store only reusable gateway token strings in the DB. Re-authenticate user session (Sanctum/MFA) before charging token. | Database migrations and models for card payments are not yet actively implemented. | **COMPLIANT**<br>(To be verified when card payment persistence is implemented). |

---

## 3. Discovered Gaps & Financial Risks

### ⚠️ CRITICAL: Idempotency Race Condition
The current idempotency implementation in both [VtpassService.php](file:///c:/Users/kolugbenga/fixpay-mobile/fixpay-laravel/app/Services/Payment/VtpassService.php) and [TransferService.php](file:///c:/Users/kolugbenga/fixpay-mobile/fixpay-laravel/app/Services/Transfer/TransferService.php) is vulnerable to race conditions:
1. Two requests with the same `idempotency_key` arrive milliseconds apart.
2. Both query `Transfer::where('idempotency_key', $key)->first()` and get `null` because neither transaction has finished and committed to the database yet.
3. Both proceed to debit the wallet via `$this->walletService->debit()`.
4. Both trigger external API payouts to Paystack or VTpass.
5. The customer is charged twice, and two ledger debits are written.

### ⚠️ Floating Point Leak in Fee Calculations
[ProcessorFeeSchedule.php](file:///c:/Users/kolugbenga/fixpay-mobile/fixpay-laravel/app/Models/ProcessorFeeSchedule.php) calculates fees using standard PHP float multiplication:
```php
$fee = (int) round($amountKobo * (float) $this->percentage_fee) + $this->flat_fee_kobo;
```
For large numbers, floating-point precision loss can result in incorrect fee calculations, violating rule **`precise_financial_math`**.

### ⚠️ Missing Central UI Freeze Lock
The React PWA does not check a unified Zustand state variable (like `isProcessing`) before initiating requests, allowing users to rapidly click checkout buttons or double-tap forms if the local loading state transitions are slow or unhandled.

---

## 4. Actionable Remediation Plan

To resolve these architectural issues and align the project with `.antigravityrules`, the following steps must be taken:

### Step 1: Implement an Idempotency Middleware in Laravel
1. Create a middleware `CheckIdempotency` that runs on all POST/PUT routes touching ledger balances or bill payments.
2. Read the `X-Idempotency-Key` header from the request.
3. Use Laravel Cache with lock capabilities to atomic-lock the key:
   ```php
   $lock = Cache::lock("idempotency_lock:{$key}", 10);
   if (!$lock->get()) {
       return response()->json(['message' => 'Concurrent request in progress.'], 409);
   }
   ```
4. If a transaction with that key already exists, return the cached representation or a `409 Conflict` status.

### Step 2: Refactor Financial Math to use BCMath
1. Refactor [ProcessorFeeSchedule.php](file:///c:/Users/kolugbenga/fixpay-mobile/fixpay-laravel/app/Models/ProcessorFeeSchedule.php) to use `BCMath`:
   ```php
   // Using bcmul and bcadd to avoid any float operators
   $percentFee = bcmul((string) $amountKobo, (string) $this->percentage_fee, 4);
   $roundedPercentFee = (int) round((float) $percentFee);
   $fee = $roundedPercentFee + $this->flat_fee_kobo;
   ```

### Step 3: Implement Zustand State Locking in PWA
1. Extend `useAuthStore` or introduce a `useTransactionStore` in Zustand.
2. Expose an `isProcessing` flag, a `lockUI()` action, and an `unlockUI()` action.
3. Require payment buttons and form submission handlers to disable interaction if `isProcessing` is active.
4. Bind checkout execution to poll endpoints or listen to webhooks before releasing the lock.
