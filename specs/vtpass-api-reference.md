# VTPass API Integration Reference
**Source:** https://vtpass.com/documentation/integrating-api/  
**Last scraped:** 2026-05-11  
**Purpose:** AI development reference — complete API surface of VTPass bill payment gateway

---

## TABLE OF CONTENTS
1. [Overview](#overview)
2. [Environments & Base URLs](#environments--base-urls)
3. [Authentication](#authentication)
4. [Request ID Format](#request-id-format)
5. [Common Endpoints Summary](#common-endpoints-summary)
6. [Airtime VTU APIs](#airtime-vtu-apis)
7. [Data Subscription APIs](#data-subscription-apis)
8. [TV Subscription APIs](#tv-subscription-apis)
9. [Electricity Payment APIs](#electricity-payment-apis)
10. [Education Payment APIs](#education-payment-apis)
11. [Callback / Webhook API](#callback--webhook-api)
12. [Response Codes](#response-codes)
13. [Sandbox Test Values](#sandbox-test-values)
14. [Support](#support)

---

## Overview

VTPass is a Nigerian bill payment API that supports:
- **Airtime VTU** – MTN, GLO, Airtel, 9mobile (Etisalat), International
- **Data Subscription** – MTN, Airtel, GLO, GLO SME, 9mobile, Smile, Spectranet
- **TV Subscription** – DSTV, GOTV, Startimes
- **Electricity Bills** – IKEDC (Ikeja), EKEDC (Eko), KEDCO (Kano), PHED (Port Harcourt), JED (Jos), IBEDC (Ibadan), KAEDCO (Kaduna), AEDC (Abuja), EEDC (Enugu), BEDC (Benin), ABA Electric, YEDC (Yola)
- **Education** – WAEC Registration, WAEC Result Checker, JAMB PIN
- **Insurance** (available via dashboard)
- **Messaging API**

All APIs follow a RESTful pattern with three key actions:
1. (Optional) **GET variation codes** — retrieve subscription plans/options
2. (Optional) **POST merchant-verify** — validate a meter/smartcard/profile before purchase
3. **POST pay** — purchase a product/service
4. **POST requery** — query/poll transaction status

---

## Environments & Base URLs

| Environment | API Base URL |
|---|---|
| **Live** | `https://vtpass.com/api/` |
| **Sandbox** | `https://sandbox.vtpass.com/api/` |

**Sandbox account registration:** https://sandbox.vtpass.com/register  
**Live account registration:** https://www.vtpass.com/register  

> After successful sandbox testing, contact VTPass support to be provisioned for live API access.

---

## Authentication

VTPass uses **API Key Authentication** (recommended).

### Generating API Keys
1. Go to your profile page:
   - Live: https://vtpass.com/account
   - Sandbox: https://sandbox.vtpass.com/account
2. Click the **API Keys** tab.
3. Ensure **API AUTHENTICATION TYPE** is set to `all` or `API keys`.
4. Copy your **static API Key**.
5. Click "click to generate your public and secret key" — these are shown **once only**. Save immediately.

### Using API Keys in Requests

**GET requests** — pass in request headers:
```
api-key: xxxxxxxxxxxxxxxxxxxx
public-key: PK_xxxxxxxxxxxxxxxxx
```

**POST requests** — pass in request headers:
```
api-key: xxxxxxxxxxxxxxxxxxxx
secret-key: SK_xxxxxxxxxxxxxxxxx
```

### Legacy: Basic Authentication (older docs)
Some older pages reference HTTP Basic Auth:
```
Authorization: Basic base64(username:password)
```
Where `username` = VTPass email, `password` = VTPass password.  
**Prefer API Key auth for all new integrations.**

---

## Request ID Format

The `request_id` field is a **mandatory unique reference** you generate for every transaction. Rules:
- Must be **unique** per transaction (never reuse a request_id).
- First **8 characters** must be today's date in `YYYYMMDD` format.
- Example: `20260511-abc123xyz789`
- Error code `085` = does not contain date, `086` = date not in correct format, `015` = request_id not found (for requery of unknown ID), `014` = request_id already used.

**Recommended generation pattern:** `{YYYYMMDD}-{unique_random_suffix}`

---

## Common Endpoints Summary

| Action | Method | Endpoint |
|---|---|---|
| Get variation codes | GET | `/api/service-variations?serviceID={serviceID}` |
| Verify biller (meter/smartcard/profile) | POST | `/api/merchant-verify` |
| Purchase product | POST | `/api/pay` |
| Query transaction status | POST | `/api/requery` |

All endpoints are prefixed with the base URL for your environment.

---

## Airtime VTU APIs

All airtime APIs share the same purchase and requery endpoints. Only the `serviceID` differs.

### Supported Providers

| Provider | serviceID |
|---|---|
| MTN | `mtn` |
| GLO | `glo` |
| Airtel | `airtel` |
| 9mobile (Etisalat) | `etisalat` |

### Purchase Airtime

**Endpoint:** POST `/api/pay`  
**ServiceID:** see table above

**Request Payload:**

| Field | M/O | Type | Description |
|---|---|---|---|
| `request_id` | M | String | Unique transaction reference (YYYYMMDD-prefix) |
| `serviceID` | M | String | e.g. `mtn`, `glo`, `airtel`, `etisalat` |
| `amount` | M | Number | Amount to topup in Naira |
| `phone` | M | Number | Recipient phone number |

**Headers (POST):**
```
api-key: YOUR_API_KEY
secret-key: YOUR_SECRET_KEY
```

**Example Response (success):**
```json
{
  "code": "000",
  "content": {
    "transactions": {
      "status": "delivered",
      "product_name": "MTN Airtime VTU",
      "unique_element": "08011111111",
      "unit_price": 100,
      "quantity": 1,
      "channel": "api",
      "commission": 3,
      "total_amount": 97,
      "discount": null,
      "type": "Airtime Recharge",
      "phone": "08011111111",
      "amount": 100,
      "platform": "api",
      "method": "api",
      "transactionId": "1234567890123456789"
    }
  },
  "response_description": "TRANSACTION SUCCESSFUL",
  "requestId": "20260511-mytxnref001",
  "amount": 100,
  "transaction_date": "2026-05-11T10:00:00.000000Z",
  "purchased_code": ""
}
```

### Query Airtime Transaction

**Endpoint:** POST `/api/requery`

| Field | M/O | Type | Description |
|---|---|---|---|
| `request_id` | M | String | The original request_id used during purchase |

---

## Data Subscription APIs

### Supported Providers

| Provider | serviceID |
|---|---|
| MTN Data | `mtn-data` |
| Airtel Data | `airtel-data` |
| GLO Data | `glo-data` |
| GLO SME Data | `glo-sme-data` |
| 9mobile Data | `9mobile` |
| Smile Network | `smile-direct` |
| Spectranet | `spectranet` |

### Step 1: Get Variation Codes

**Endpoint:** GET `/api/service-variations?serviceID={serviceID}`  
**Headers:** `api-key` + `public-key`

**Example Response:**
```json
{
  "response_description": "000",
  "content": {
    "ServiceName": "MTN Data",
    "serviceID": "mtn-data",
    "convinience_fee": "0 %",
    "variations": [
      {
        "variation_code": "mtn-10mb-100",
        "name": "MTN 100MB Daily Plan",
        "variation_amount": "100.00",
        "fixedPrice": "Yes"
      },
      {
        "variation_code": "mtn-1gb-300",
        "name": "MTN 1GB for 30 days",
        "variation_amount": "300.00",
        "fixedPrice": "Yes"
      }
    ]
  }
}
```

### Step 2: Purchase Data Bundle

**Endpoint:** POST `/api/pay`

| Field | M/O | Type | Description |
|---|---|---|---|
| `request_id` | M | String | Unique transaction reference |
| `serviceID` | M | String | e.g. `mtn-data` |
| `billersCode` | M | String | Phone number to subscribe |
| `variation_code` | M | String | Variation code from GET variations |
| `amount` | O | Number | Amount (usually ignored; determined by variation_code) |
| `phone` | M | Number | Phone number of customer/recipient |

### Step 3: Query Data Transaction

**Endpoint:** POST `/api/requery`

| Field | M/O | Type | Description |
|---|---|---|---|
| `request_id` | M | String | Original request_id |

---

## TV Subscription APIs

### Supported Providers

| Provider | serviceID | Verify Required |
|---|---|---|
| DSTV | `dstv` | Yes (smartcard) |
| GOTV | `gotv` | Yes (smartcard) |
| Startimes | `startimes` | Yes (smartcard) |

### Flow for TV Subscriptions

1. GET variation codes → get bouquet options
2. POST merchant-verify → verify smartcard number, get customer name + `Renewal_Amount`
3. POST pay → purchase (bouquet change OR bouquet renewal)
4. POST requery → poll status if needed

### Step 1: Get TV Variation Codes

**Endpoint:** GET `/api/service-variations?serviceID={serviceID}`

### Step 2: Verify Smartcard Number

**Endpoint:** POST `/api/merchant-verify`

| Field | M/O | Type | Description |
|---|---|---|---|
| `billersCode` | M | Number | Smartcard number |
| `serviceID` | M | String | e.g. `dstv`, `gotv`, `startimes` |

**Expected Response:**
```json
{
  "code": "020",
  "content": {
    "Customer_Name": "John Doe",
    "Status": "Active",
    "Due_Date": "2026-06-10",
    "Customer_Number": "1212121212",
    "Current_Bouquet": "DStv Compact",
    "Renewal_Amount": 9600
  }
}
```

### Step 3a: Bouquet Change (DSTV/GOTV)

**Endpoint:** POST `/api/pay`

| Field | M/O | Type | Description |
|---|---|---|---|
| `request_id` | M | String | Unique transaction reference |
| `serviceID` | M | String | `dstv` or `gotv` |
| `billersCode` | M | String | Smartcard number |
| `variation_code` | M | String | Bouquet variation code |
| `amount` | O | Number | Amount (uses variation price if not specified) |
| `phone` | M | Number | Customer phone number |
| `subscription_type` | M | String | `"change"` |
| `quantity` | O | Number | Number of months (default: 1) |

### Step 3b: Bouquet Renewal (DSTV/GOTV)

Same as bouquet change but:
- `subscription_type` = `"renew"`
- `amount` = use `Renewal_Amount` from the merchant-verify response
- No `variation_code` required

### Startimes Purchase

**Endpoint:** POST `/api/pay`  
**ServiceID:** `startimes`

| Field | M/O | Type | Description |
|---|---|---|---|
| `request_id` | M | String | Unique reference |
| `serviceID` | M | String | `startimes` |
| `billersCode` | M | String | Smartcard number or ewallet number |
| `variation_code` | M | String | Bouquet variation code |
| `amount` | O | Number | Optional; uses variation price if omitted |
| `phone` | M | Number | Customer phone |

### Step 4: Query TV Transaction

**Endpoint:** POST `/api/requery`

| Field | M/O | Type | Description |
|---|---|---|---|
| `request_id` | M | String | Original request_id |

---

## Electricity Payment APIs

### Supported DISCOs (Distribution Companies)

| DISCO | serviceID | Coverage Area |
|---|---|---|
| IKEDC – Ikeja Electric | `ikeja-electric` | Abule Egba, Akowonjo, Ikeja, Ikorodu, Oshodi, Shomolu (Lagos) |
| EKEDC – Eko Electric | `eko-electric` | Eko areas (Lagos Island, etc.) |
| KEDCO – Kano Electric | `kedco` | Kano State |
| PHED – Port Harcourt Electric | `phed` | Rivers State |
| JED – Jos Electric | `jos-electric` | Plateau State |
| IBEDC – Ibadan Electric | `ibadan-electric` | Oyo, Osun, Ogun, Kwara States |
| KAEDCO – Kaduna Electric | `kaduna-electric` | Kaduna State |
| AEDC – Abuja Electric | `abuja-electric` | FCT Abuja |
| EEDC – Enugu Electric | `enugu-electric` | Enugu, Anambra, Imo, Abia, Ebonyi |
| BEDC – Benin Electric | `benin-electric` | Edo, Delta, Ondo, Ekiti States |
| ABA Electric | `aba-electric` | ABA area |
| YEDC – Yola Electric | `yola-electric` | Adamawa, Taraba States |

### Meter Types

- **Prepaid** — generates a token (load onto physical meter); `variation_code = "prepaid"`
- **Postpaid** — pays electricity bill; `variation_code = "postpaid"`

### Customer Account Types (IKEDC note)

- `MD` = Maximum Demand (large commercial consumption)
- `NMD` = Non-Maximum Demand (households, small businesses)

Commission rates differ by meter type and account type. Check https://vtpass.com/commissions.

### Flow for Electricity Payment

1. POST merchant-verify → validate meter number, get customer name + account type
2. POST pay → vend token (prepaid) or pay bill (postpaid)
3. POST requery → poll status if pending

### Step 1: Verify Meter Number

**Endpoint:** POST `/api/merchant-verify`

| Field | M/O | Type | Description |
|---|---|---|---|
| `billersCode` | M | Number | Customer meter number |
| `serviceID` | M | String | e.g. `ikeja-electric` |
| `type` | M | String | `"prepaid"` or `"postpaid"` |

**Sandbox meter numbers (IKEDC):**
- Prepaid: `1111111111111`
- Postpaid: `1010101010101`

**Example Verify Response:**
```json
{
  "code": "020",
  "content": {
    "Customer_Name": "Jane Doe",
    "Meter_Number": "1111111111111",
    "Address": "12 Test Street, Ikeja",
    "Customer_District": "Ikeja",
    "Outstanding": "0",
    "Meter_Type": "prepaid",
    "Customer_Account_Type": "NMD"
  }
}
```

### Step 2: Purchase Electricity (Prepaid)

**Endpoint:** POST `/api/pay`  
**ServiceID:** e.g. `ikeja-electric`

| Field | M/O | Type | Description |
|---|---|---|---|
| `request_id` | M | String | Unique transaction reference |
| `serviceID` | M | String | e.g. `ikeja-electric` |
| `billersCode` | M | String | Customer meter number |
| `variation_code` | M | String | `"prepaid"` |
| `amount` | M | Number | Amount in Naira to purchase |
| `phone` | M | Number | Customer phone number |

> For successful prepaid transactions, a **token** is returned in the response. **Always display and SMS/email this token to the customer.**

### Step 2: Purchase Electricity (Postpaid)

Same as prepaid but `variation_code = "postpaid"`.

**Expected Response (Prepaid success):**
```json
{
  "code": "000",
  "content": {
    "transactions": {
      "status": "delivered",
      "product_name": "Ikeja Electric Payment Service",
      "unique_element": "1111111111111",
      "unit_price": 1000,
      "quantity": 1,
      "channel": "api",
      "commission": 30,
      "total_amount": 970,
      "type": "Electricity",
      "phone": "08011111111",
      "amount": 1000,
      "transactionId": "123456789012345"
    }
  },
  "response_description": "TRANSACTION SUCCESSFUL",
  "requestId": "20260511-elec001",
  "amount": 1000,
  "transaction_date": "2026-05-11T10:30:00.000000Z",
  "purchased_code": "1234-5678-9012-3456-7890",
  "token": "1234-5678-9012-3456-7890",
  "units": "10.5"
}
```

### Step 3: Query Electricity Transaction

**Endpoint:** POST `/api/requery`

| Field | M/O | Type | Description |
|---|---|---|---|
| `request_id` | M | String | Original request_id |

---

## Education Payment APIs

### Supported Services

| Service | serviceID |
|---|---|
| WAEC Registration PIN | `waec-registration` |
| WAEC Result Checker | `waec` |
| JAMB PIN Vending | `jamb` |

---

### WAEC Registration API

#### Get Variation Codes

**Endpoint:** GET `/api/service-variations?serviceID=waec-registration`

**Example Response:**
```json
{
  "response_description": "000",
  "content": {
    "ServiceName": "WAEC Registration PIN",
    "serviceID": "waec-registration",
    "convinience_fee": "N0.00",
    "variations": [
      {
        "variation_code": "waec-registraion",
        "name": "WASSCE for Private Candidates",
        "variation_amount": "14450.00",
        "fixedPrice": "Yes"
      }
    ]
  }
}
```

#### Purchase WAEC Registration PIN

**Endpoint:** POST `/api/pay`

| Field | M/O | Type | Description |
|---|---|---|---|
| `request_id` | M | String | Unique transaction reference |
| `serviceID` | M | String | `waec-registration` |
| `variation_code` | M | String | From GET variations |
| `amount` | O | Number | Optional; determined by variation_code |
| `quantity` | O | Number | Number of pins (default: 1) |
| `phone` | M | Number | Customer phone number |

**Example Response:**
```json
{
  "code": "000",
  "content": {
    "transactions": {
      "status": "delivered",
      "product_name": "WAEC Registration PIN",
      "unique_element": "08011111111",
      "unit_price": 14450,
      "quantity": 1,
      "commission": 150,
      "total_amount": 14300,
      "type": "Education",
      "amount": 14450,
      "transactionId": "1582290782154"
    }
  },
  "response_description": "TRANSACTION SUCCESSFUL",
  "requestId": "20260511-waec001",
  "amount": 14450,
  "transaction_date": "2026-05-11T10:00:00.000000Z",
  "purchased_code": "Token: 0100070365657400875",
  "tokens": ["0100070365657400875"]
}
```

---

### JAMB PIN Vending API

#### Get Variation Codes

**Endpoint:** GET `/api/service-variations?serviceID=jamb`

**Example Response:**
```json
{
  "response_description": "000",
  "content": {
    "ServiceName": "Jamb",
    "serviceID": "jamb",
    "convinience_fee": "0 %",
    "variations": [
      {
        "variation_code": "utme-mock",
        "name": "UTME PIN (with mock)",
        "variation_amount": "7700.00",
        "fixedPrice": "Yes"
      },
      {
        "variation_code": "utme-no-mock",
        "name": "UTME PIN (without mock)",
        "variation_amount": "6200.00",
        "fixedPrice": "Yes"
      }
    ]
  }
}
```

#### Verify JAMB Profile ID

**Endpoint:** POST `/api/merchant-verify`  
**Sandbox Profile ID:** `0123456789`

| Field | M/O | Type | Description |
|---|---|---|---|
| `billersCode` | M | Number | JAMB Profile ID (from JAMB Official Website) |
| `serviceID` | M | String | `jamb` |
| `type` | M | String | Variation code e.g. `utme-mock` |

**Example Response:**
```json
{
  "code": "000",
  "content": {
    "Customer_Name": "Capital James",
    "commission_details": {
      "amount": 10.22,
      "rate": "1.50",
      "rate_type": "percent",
      "computation_type": "default"
    }
  }
}
```

#### Purchase JAMB PIN

**Endpoint:** POST `/api/pay`

| Field | M/O | Type | Description |
|---|---|---|---|
| `request_id` | M | String | Unique transaction reference |
| `serviceID` | M | String | `jamb` |
| `variation_code` | M | String | e.g. `utme-mock` or `utme-no-mock` |
| `billersCode` | M | String | JAMB Profile ID |
| `amount` | O | Number | Optional; determined by variation_code |
| `phone` | M | Number | Customer phone number |

**Example Response:**
```json
{
  "code": "000",
  "content": {
    "transactions": {
      "status": "delivered",
      "product_name": "JAMB PIN VENDING (UTME & Direct Entry)",
      "unique_element": "0123456789",
      "unit_price": "7700.00",
      "quantity": 1,
      "commission": "100.00",
      "total_amount": 7600,
      "type": "Education",
      "phone": "123450987623",
      "amount": "7700.00",
      "transactionId": "17398810413069178444218360"
    }
  },
  "response_description": "TRANSACTION SUCCESSFUL",
  "requestId": "20250218131720-0rjx1p27xnj",
  "amount": 7700,
  "transaction_date": "2025-02-18T12:17:21.000000Z",
  "purchased_code": "Pin : 3678251321392432",
  "Pin": "Pin : 3678251321392432"
}
```

> **Important:** Do not sell JAMB pins above the JAMB-approved pricing.

---

## Callback / Webhook API

### Setup

1. Log in to your VTPass dashboard.
2. Navigate to settings and update your **Callback URL** (must be a POST endpoint).
3. Your server must reply with:
   ```json
   { "response": "success" }
   ```
4. VTPass will retry up to **5 times** until it receives `{ "response": "success" }`.

### When Webhooks Are Sent

- **Transaction status update** — when a pending transaction is resolved by VTPass staff
- **Variation codes update** — when a service's variation codes change

### Transaction Update Webhook

**Type:** `transaction-update`

**Payload sent to your URL:**
```json
{
  "type": "transaction-update",
  "data": {
    "code": "000",
    "content": {
      "transactions": {
        "status": "delivered",
        "product_name": "Airtel Airtime",
        "unique_element": "08011111111",
        "unit_price": 5,
        "quantity": 1,
        "channel": "api",
        "commission": 0,
        "total_amount": 4.85,
        "type": "Airtime Recharge",
        "phone": "07061933309",
        "amount": 5,
        "platform": "api",
        "method": "wallet",
        "transactionId": "1583519914158857111079"
      }
    },
    "response_description": "TRANSACTION DELIVERED",
    "amount": 5,
    "transaction_date": null,
    "requestId": "ELAET1RA7PC06250-12S5962-2102P",
    "purchased_code": ""
  }
}
```

| Field | Type | Description |
|---|---|---|
| `type` | string | `"transaction-update"` |
| `data.code` | string | Response code (e.g. `"000"`) |
| `data.content.transactions.status` | string | Actual status: `delivered`, `pending`, `reversed` |
| `data.requestId` | string | The requestId you originally sent |
| `data.content.transactions.transactionId` | string | VTPass internal transaction identifier |

### Transaction Reversal Webhook

Sent when a pending transaction is reversed (refunded to wallet).

```json
{
  "type": "transaction-update",
  "data": {
    "code": "040",
    "content": {
      "transactions": {
        "status": "reversed",
        "unique_element": "08011111111",
        "unit_price": 50,
        "commission": 2,
        "total_amount": 48.5,
        "amount": 50,
        "transactionId": "1583501216545377109916",
        "wallet_credit_id": "15835022148401519675278125"
      }
    },
    "response_description": "TRANSACTION REVERSAL TO WALLET",
    "amount": 48.5,
    "requestId": "ELAET1A7PC06250-12S592-2102P"
  }
}
```

| Field | Description |
|---|---|
| `code` | `"040"` = Transaction Reversed |
| `status` | `"reversed"` |
| `amount` | Amount reversed back to your wallet |
| `wallet_credit_id` | Identifier for the wallet reversal transaction |

### Variation Codes Update Webhook

Sent when a service's available plans/codes change.

```json
{
  "type": "variations-update",
  "response_description": "VARIATION CODE UPDATE",
  "serviceID": "smile-direct",
  "service": "Smile Payment",
  "summary": {
    "added_variation_codes": 0,
    "updated_variation_codes": 38,
    "removed_variation_codes": 23
  },
  "data": {
    "content": {
      "ServiceName": "Smile Payment",
      "serviceID": "smile-direct",
      "variations": [ ... ]
    }
  },
  "actionRequired": {
    "added": { "total": 0, "variation_codes": [] },
    "updated": { "total": 38, "variation_codes": ["624", "625", ...] },
    "removed": { "total": 23, "variation_codes": ["545", "522", ...] }
  },
  "datetime": "2020-06-11 23-31-15"
}
```

---

## Response Codes

### Transaction / Requery Response Codes

| Code | Meaning | Notes |
|---|---|---|
| `000` | TRANSACTION PROCESSED | Check `content.transactions.status` for actual state |
| `001` | TRANSACTION QUERY | Status query of a given transaction |
| `010` | VARIATION CODE DOES NOT EXIST | Invalid variation code used |
| `011` | INVALID ARGUMENTS | Missing required field(s) |
| `012` | PRODUCT DOES NOT EXIST | Invalid serviceID |
| `013` | BELOW MINIMUM AMOUNT | Amount too low for service |
| `014` | REQUEST ID ALREADY EXIST | Duplicate request_id — already used |
| `015` | INVALID REQUEST ID | request_id not found on VTPass (for requery) |
| `016` | TRANSACTION FAILED | Transaction failed |
| `017` | ABOVE MAXIMUM AMOUNT | Amount exceeds service maximum |
| `018` | LOW WALLET BALANCE | Insufficient funds |
| `019` | LIKELY DUPLICATE TRANSACTION | Same service + biller_code within 30 seconds |
| `021` | ACCOUNT LOCKED | Account is locked |
| `022` | ACCOUNT SUSPENDED | Account is suspended |
| `023` | API ACCESS NOT ENABLED | Contact VTPass to activate API access |
| `024` | ACCOUNT INACTIVE | Account is inactive |
| `025` | RECIPIENT BANK INVALID | Invalid bank code (bank transfer) |
| `026` | RECIPIENT ACCOUNT NOT VERIFIED | Bank account could not be verified |
| `027` | IP NOT WHITELISTED | Submit your server IP to VTPass for whitelisting |
| `028` | PRODUCT NOT WHITELISTED | Product not enabled on your account |
| `030` | BILLER NOT REACHABLE | Service provider is unreachable |
| `031` | BELOW MINIMUM QUANTITY | Under the minimum quantity for this service |
| `032` | ABOVE MAXIMUM QUANTITY | Over the maximum quantity for this service |
| `034` | SERVICE SUSPENDED | Service temporarily suspended |
| `035` | SERVICE INACTIVE | Service is turned off |
| `040` | TRANSACTION REVERSAL | Funds reversed to wallet |
| `044` | TRANSACTION RESOLVED | Resolved — contact VTPass for info |
| `083` | SYSTEM ERROR | Contact tech support |
| `085` | IMPROPER REQUEST ID: NO DATE | request_id must start with YYYYMMDD |
| `086` | IMPROPER REQUEST ID: BAD DATE FORMAT | Date must be valid YYYYMMDD |
| `087` | INVALID CREDENTIALS | Wrong API key / secret key |
| `089` | REQUEST PROCESSING — PLEASE WAIT | Previous request still processing |
| `091` | TRANSACTION NOT PROCESSED | Not processed; not charged |
| `099` | TRANSACTION IS PROCESSING | Currently processing — requery later |

### Verification Response Codes (merchant-verify)

| Code | Meaning | Notes |
|---|---|---|
| `020` | BILLER CONFIRMED | Verification successful |
| `011` | INVALID ARGUMENTS | Missing required verification fields |
| `012` | PRODUCT DOES NOT EXIST | Invalid serviceID |
| `030` | BILLER NOT REACHABLE | Provider unreachable |

### Transaction Inner Status (`content.transactions.status`)

| Status | Meaning |
|---|---|
| `initiated` | Transaction started but not yet processed |
| `pending` | Processing — provider has not confirmed yet; requery to update |
| `delivered` | Successful — service confirmed delivered |
| `failed` | Transaction failed |
| `reversed` | Transaction reversed — funds returned to wallet |

> **Important:** When `code = "000"`, always inspect `content.transactions.status` for the real outcome. `"000"` alone does not mean success — it means "processed" (could be `pending`).

---

## Sandbox Test Values

Use these in the sandbox environment only.

### Airtime / Data Phone Numbers

| Phone Number | Simulated Event |
|---|---|
| `08011111111` | Success |
| `201000000000` | Pending (unexpected) |
| `500000000000` | Unexpected response |
| `400000000000` | No response |
| `300000000000` | Timeout |
| Any other number | Failed |

### TV Subscription Smartcard Numbers

| BillersCode | Simulated Event |
|---|---|
| `1212121212` | Success |
| `201000000000` | Pending |
| `500000000000` | Unexpected response |
| `400000000000` | No response |
| `300000000000` | Timeout |
| Any other number | Failed |

### Electricity Meter Numbers (IKEDC)

| Meter Number | Type | Simulated Event |
|---|---|---|
| `1111111111111` | Prepaid | Success |
| `1010101010101` | Postpaid | Success |
| `201000000000` | Either | Pending |
| `500000000000` | Either | Unexpected |
| `400000000000` | Either | No response |
| `300000000000` | Either | Timeout |
| Any other number | Either | Failed |

### JAMB Profile ID (Sandbox)

| Profile ID | Event |
|---|---|
| `0123456789` | Success |
| Any other value | Failed |

---

## Integration Best Practices

1. **Always requery on pending.** If `code = "099"` or `status = "pending"`, poll `/api/requery` at intervals until status resolves.
2. **Treat unknown/timeout responses as pending.** Never assume failure — always requery.
3. **Never reuse request_id.** Generates a `014` error and may interfere with requery.
4. **Always verify before purchase** for meter/smartcard-based services (electricity, TV). Use `merchant-verify` to confirm biller and get correct amounts.
5. **Display prepaid electricity tokens prominently.** The token in `purchased_code` / `token` must be shown/sent to the customer.
6. **Implement webhook handler.** Register your callback URL to receive async transaction updates without constant polling.
7. **Whitelist your server IP.** Contact VTPass support with your server's public IP for API whitelisting (required before go-live).
8. **Test all sandbox scenarios** (success, pending, failure, timeout) before requesting live provisioning.

---

## Support

- **Email:** support@vtpass.com
- **Skype:** vtpass.techsupport
- **Documentation:** https://vtpass.com/documentation/
- **Changelog:** https://vtpass.com/documentation/?page_id=312
- **Glossary:** https://vtpass.com/documentation/?page_id=314
- **Commission Rates:** https://vtpass.com/commissions
