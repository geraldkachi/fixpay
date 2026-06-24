import type { User, Wallet, Transaction, Mandate, Dispute, TenantConfig, NipBank, ServiceVariation, MandateResponse } from '@/types'

export const mockUser: User = {
  id: 'usr_01HXKP3VWRY2J9NVW3B5C',
  phone: '08012345678',
  email: 'john.adeyemi@gmail.com',
  firstName: 'John',
  lastName: 'Adeyemi',
  tier: 2,
  kycStatus: 'verified',
  createdAt: '2026-01-15T10:30:00Z',
}

export const mockWallet: Wallet = {
  id: 'wlt_01HXKP3VWRY2J9NVW3B5D',
  balanceKobo: 4_523_750,
  currency: 'NGN',
  status: 'active',
  virtualAccount: { accountNumber: '9901234567', bankName: 'Providus Bank', bankCode: '101' },
}

const ago = (h: number) => new Date(Date.now() - h * 3_600_000).toISOString()
const dago = (d: number) => new Date(Date.now() - d * 86_400_000).toISOString()

export const mockTransactions: Transaction[] = [
  { id: 'txn_01', type: 'transfer_in',    amountKobo: 1_000_000, feeKobo: 0,   status: 'completed', reference: '20260511-TIN001', description: 'Transfer from Emmanuel Adeyemi', counterpartyName: 'Emmanuel Adeyemi', createdAt: ago(1) },
  { id: 'txn_02', type: 'bill_payment',   amountKobo: 100_000,   feeKobo: 0,   status: 'completed', reference: '20260511-BP001',  description: 'MTN Airtime', serviceId: 'mtn', serviceName: 'MTN Airtime', createdAt: ago(3) },
  { id: 'txn_03', type: 'bill_payment',   amountKobo: 960_000,   feeKobo: 0,   status: 'completed', reference: '20260511-BP002',  description: 'DStv Compact – 1212121212', serviceId: 'dstv', serviceName: 'DStv', createdAt: dago(1) },
  { id: 'txn_04', type: 'bill_payment',   amountKobo: 500_000,   feeKobo: 0,   status: 'completed', reference: '20260510-BP003',  description: 'Ikeja Electric – Prepaid', serviceId: 'ikeja-electric', serviceName: 'Ikeja Electric', token: '1234-5678-9012-3456-7890', createdAt: dago(2) },
  { id: 'txn_05', type: 'transfer_out',   amountKobo: 250_000,   feeKobo: 5250, status: 'completed', reference: '20260509-TOUT01', description: 'Transfer to Fatima Bello', counterpartyName: 'Fatima Bello', createdAt: dago(3) },
  { id: 'txn_06', type: 'wallet_funding', amountKobo: 5_000_000, feeKobo: 0,   status: 'completed', reference: '20260508-WF001',  description: 'Wallet funding via Providus Bank', createdAt: dago(4) },
  { id: 'txn_07', type: 'bill_payment',   amountKobo: 300_000,   feeKobo: 0,   status: 'completed', reference: '20260507-BP004',  description: 'MTN 1GB Data – 08012345678', serviceId: 'mtn-data', serviceName: 'MTN Data', createdAt: dago(5) },
  { id: 'txn_08', type: 'bill_payment',   amountKobo: 1_445_000, feeKobo: 0,   status: 'failed',    reference: '20260506-BP005',  description: 'WAEC Registration PIN', serviceId: 'waec-registration', serviceName: 'WAEC Registration', createdAt: dago(6) },
  { id: 'txn_09', type: 'transfer_in',    amountKobo: 2_000_000, feeKobo: 0,   status: 'completed', reference: '20260505-TIN002', description: 'Salary – April 2026', counterpartyName: 'ABC Tech Ltd', createdAt: dago(7) },
  { id: 'txn_10', type: 'bill_payment',   amountKobo: 770_000,   feeKobo: 0,   status: 'completed', reference: '20260504-BP006',  description: 'JAMB PIN UTME – 0123456789', serviceId: 'jamb', serviceName: 'JAMB', createdAt: dago(8) },
]

export const mockMandates: Mandate[] = [
  { id: 'mnd_01', accountNumber: '0123456789', bankName: 'Guaranty Trust Bank', bankCode: '058', amountKobo: 500_000, frequency: 'monthly', startDate: '2026-04-01', endDate: '2027-03-31', narrative: 'Monthly Savings Plan', status: 'active', createdAt: '2026-04-01T09:00:00Z' },
  { id: 'mnd_02', accountNumber: '1234567890', bankName: 'Zenith Bank', bankCode: '057', amountKobo: 200_000, frequency: 'weekly', startDate: '2026-05-15', endDate: '2026-12-31', narrative: 'Weekly Contribution', status: 'pending_auth', authorizationUrl: 'https://sandbox.zenithbank.com/mandate-auth?ref=MND20260511', createdAt: '2026-05-11T08:00:00Z' },
]

// Backend-shaped mock mandates — used by /api/mandates handlers
export const mockMandateResponses: MandateResponse[] = [
  {
    mandateReference: 'MND-GTB-2026-001',
    providerReference: 'NIBSS_REF_001',
    bankCode: '058',
    accountNumber: '0123456789',
    maxAmount: 5000,
    status: 'active',
    startDate: '2026-04-01',
    endDate: '2027-03-31',
    providerMessage: 'Mandate active and approved',
    createdAt: '2026-04-01T09:00:00Z',
    updatedAt: '2026-04-01T09:00:00Z',
  },
  {
    mandateReference: 'MND-ZNB-2026-002',
    providerReference: null,
    bankCode: '057',
    accountNumber: '1234567890',
    maxAmount: 2000,
    status: 'pending_auth',
    startDate: '2026-05-15',
    endDate: '2026-12-31',
    providerMessage: 'Awaiting customer authorization',
    createdAt: '2026-05-11T08:00:00Z',
    updatedAt: '2026-05-11T08:00:00Z',
  },
]

export const mockDisputes: Dispute[] = [
  { id: 'dsp_01', transactionId: 'txn_08', category: 'not_delivered', description: 'Paid for WAEC registration PIN but never received it after 24 hours.', status: 'under_review', slaDeadline: new Date(Date.now() + 2 * 86_400_000).toISOString(), createdAt: dago(1), transaction: mockTransactions[7] },
  { id: 'dsp_02', transactionId: 'txn_05', category: 'wrong_amount', description: 'Expected ₦2,500 deducted but ₦2,552.50 was charged.', status: 'resolved', resolution: 'Transfer fee of ₦52.50 per our fee schedule. Dispute closed.', slaDeadline: dago(2), createdAt: dago(5), transaction: mockTransactions[4] },
]

export const mockTenantConfig: TenantConfig = {
  tenantId: 'ten_01HXKP3VWRY2J9NVW3B5E',
  slug: 'fixpay', appName: 'FixPay',
  primaryColor: '#A51D21', secondaryColor: '#34C759', accentColor: '#FF9500',
  logoUrl: null, faviconUrl: null,
  supportEmail: 'support@fixpay.com', supportPhone: '07000000000',
  features: { billPayments: true, directDebit: true, walletTransfers: true, internationalAirtime: false, disputeManagement: true, nibssTransfers: true },
}

export const mockBanks: NipBank[] = [
  { bankCode: '058', bankName: 'Guaranty Trust Bank' },
  { bankCode: '057', bankName: 'Zenith Bank' },
  { bankCode: '011', bankName: 'First Bank of Nigeria' },
  { bankCode: '044', bankName: 'Access Bank' },
  { bankCode: '033', bankName: 'United Bank for Africa' },
  { bankCode: '070', bankName: 'Fidelity Bank' },
  { bankCode: '039', bankName: 'Stanbic IBTC Bank' },
  { bankCode: '032', bankName: 'Union Bank' },
  { bankCode: '035', bankName: 'Wema Bank' },
  { bankCode: '101', bankName: 'Providus Bank' },
  { bankCode: '214', bankName: 'First City Monument Bank' },
  { bankCode: '050', bankName: 'Ecobank Nigeria' },
  { bankCode: '076', bankName: 'Polaris Bank' },
]

export const variations: Record<string, ServiceVariation[]> = {
  'mtn-data': [
    { variationCode: 'mtn-10mb-100', name: 'MTN 100MB Daily', variationAmount: '100.00', fixedPrice: 'Yes' },
    { variationCode: 'mtn-1gb-300',  name: 'MTN 1GB (30 days)', variationAmount: '300.00', fixedPrice: 'Yes' },
    { variationCode: 'mtn-2gb-500',  name: 'MTN 2GB (30 days)', variationAmount: '500.00', fixedPrice: 'Yes' },
    { variationCode: 'mtn-5gb-1000', name: 'MTN 5GB (30 days)', variationAmount: '1000.00', fixedPrice: 'Yes' },
    { variationCode: 'mtn-10gb-2000',name: 'MTN 10GB (30 days)', variationAmount: '2000.00', fixedPrice: 'Yes' },
  ],
  'airtel-data': [
    { variationCode: 'airtel-100mb', name: 'Airtel 100MB Daily', variationAmount: '100.00', fixedPrice: 'Yes' },
    { variationCode: 'airtel-1gb',   name: 'Airtel 1GB (30 days)', variationAmount: '300.00', fixedPrice: 'Yes' },
    { variationCode: 'airtel-2gb',   name: 'Airtel 2GB (30 days)', variationAmount: '500.00', fixedPrice: 'Yes' },
    { variationCode: 'airtel-5gb',   name: 'Airtel 5GB (30 days)', variationAmount: '1500.00', fixedPrice: 'Yes' },
  ],
  'glo-data': [
    { variationCode: 'glo-1gb',  name: 'Glo 1GB (30 days)',  variationAmount: '300.00', fixedPrice: 'Yes' },
    { variationCode: 'glo-2gb',  name: 'Glo 2GB (30 days)',  variationAmount: '500.00', fixedPrice: 'Yes' },
    { variationCode: 'glo-5gb',  name: 'Glo 5GB (30 days)',  variationAmount: '1500.00', fixedPrice: 'Yes' },
  ],
  dstv: [
    { variationCode: 'dstv-padi',         name: 'DStv Padi',         variationAmount: '2500.00',  fixedPrice: 'Yes' },
    { variationCode: 'dstv-yanga',        name: 'DStv Yanga',        variationAmount: '3500.00',  fixedPrice: 'Yes' },
    { variationCode: 'dstv-confam',       name: 'DStv Confam',       variationAmount: '6200.00',  fixedPrice: 'Yes' },
    { variationCode: 'dstv-compact',      name: 'DStv Compact',      variationAmount: '9600.00',  fixedPrice: 'Yes' },
    { variationCode: 'dstv-compact-plus', name: 'DStv Compact Plus', variationAmount: '14250.00', fixedPrice: 'Yes' },
    { variationCode: 'dstv-premium',      name: 'DStv Premium',      variationAmount: '21000.00', fixedPrice: 'Yes' },
  ],
  gotv: [
    { variationCode: 'gotv-smallie', name: 'GOtv Smallie', variationAmount: '1575.00', fixedPrice: 'Yes' },
    { variationCode: 'gotv-jinja',   name: 'GOtv Jinja',   variationAmount: '2460.00', fixedPrice: 'Yes' },
    { variationCode: 'gotv-jolli',   name: 'GOtv Jolli',   variationAmount: '3300.00', fixedPrice: 'Yes' },
    { variationCode: 'gotv-max',     name: 'GOtv Max',     variationAmount: '4850.00', fixedPrice: 'Yes' },
    { variationCode: 'gotv-supa',    name: 'GOtv Supa',    variationAmount: '6400.00', fixedPrice: 'Yes' },
  ],
  startimes: [
    { variationCode: 'nova',    name: 'Startimes Nova',    variationAmount: '900.00',  fixedPrice: 'Yes' },
    { variationCode: 'basic',   name: 'Startimes Basic',   variationAmount: '1850.00', fixedPrice: 'Yes' },
    { variationCode: 'smart',   name: 'Startimes Smart',   variationAmount: '2600.00', fixedPrice: 'Yes' },
    { variationCode: 'classic', name: 'Startimes Classic', variationAmount: '2500.00', fixedPrice: 'Yes' },
    { variationCode: 'super',   name: 'Startimes Super',   variationAmount: '4900.00', fixedPrice: 'Yes' },
  ],
  jamb: [
    { variationCode: 'utme-mock',    name: 'UTME PIN (with mock)',    variationAmount: '7700.00', fixedPrice: 'Yes' },
    { variationCode: 'utme-no-mock', name: 'UTME PIN (without mock)', variationAmount: '6200.00', fixedPrice: 'Yes' },
  ],
  'waec-registration': [
    { variationCode: 'waec-registraion', name: 'WASSCE for Private Candidates', variationAmount: '14450.00', fixedPrice: 'Yes' },
  ],
  waec: [
    { variationCode: 'waec-result', name: 'WAEC Result Checker', variationAmount: '1000.00', fixedPrice: 'Yes' },
  ],
  'etisalat-data': [
    { variationCode: 'etisalat-100mb', name: '9mobile 100MB Daily',  variationAmount: '100.00',  fixedPrice: 'Yes' },
    { variationCode: 'etisalat-1gb',   name: '9mobile 1GB (30 days)', variationAmount: '300.00', fixedPrice: 'Yes' },
    { variationCode: 'etisalat-2gb',   name: '9mobile 2GB (30 days)', variationAmount: '500.00', fixedPrice: 'Yes' },
    { variationCode: 'etisalat-5gb',   name: '9mobile 5GB (30 days)', variationAmount: '1500.00', fixedPrice: 'Yes' },
  ],
  'ui-insure': [
    { variationCode: 'third-party-motor',        name: 'Third Party Motor Insurance (Bronze)', variationAmount: '5000.00', fixedPrice: 'Yes' },
    { variationCode: 'third-party-motor-silver',  name: 'Third Party Motor Insurance (Silver)', variationAmount: '7500.00', fixedPrice: 'Yes' },
  ],
  'personal-accident-insurance': [
    { variationCode: 'personal-accident-basic',    name: 'Personal Accident (Basic)',    variationAmount: '2500.00',  fixedPrice: 'Yes' },
    { variationCode: 'personal-accident-standard', name: 'Personal Accident (Standard)', variationAmount: '5000.00',  fixedPrice: 'Yes' },
  ],
  'home-cover-insurance': [
    { variationCode: 'home-cover-basic',   name: 'Home Cover (Basic)',   variationAmount: '5000.00',  fixedPrice: 'Yes' },
    { variationCode: 'home-cover-premium', name: 'Home Cover (Premium)', variationAmount: '10000.00', fixedPrice: 'Yes' },
  ],
  'smile-direct': [
    { variationCode: 'smile-500mb', name: 'Smile 500MB',     variationAmount: '500.00',  fixedPrice: 'Yes' },
    { variationCode: 'smile-1gb',   name: 'Smile 1GB',       variationAmount: '1000.00', fixedPrice: 'Yes' },
    { variationCode: 'smile-5gb',   name: 'Smile 5GB',       variationAmount: '3000.00', fixedPrice: 'Yes' },
  ],
  'spectranet': [
    { variationCode: 'spectranet-500mb', name: 'Spectranet 500MB', variationAmount: '500.00',  fixedPrice: 'Yes' },
    { variationCode: 'spectranet-1gb',   name: 'Spectranet 1GB',   variationAmount: '1000.00', fixedPrice: 'Yes' },
    { variationCode: 'spectranet-5gb',   name: 'Spectranet 5GB',   variationAmount: '3500.00', fixedPrice: 'Yes' },
  ],
  'glo-sme-data': [
    { variationCode: 'glo-sme-1gb', name: 'Glo SME 1GB (30 days)', variationAmount: '300.00',  fixedPrice: 'Yes' },
    { variationCode: 'glo-sme-2gb', name: 'Glo SME 2GB (30 days)', variationAmount: '500.00',  fixedPrice: 'Yes' },
    { variationCode: 'glo-sme-5gb', name: 'Glo SME 5GB (30 days)', variationAmount: '1000.00', fixedPrice: 'Yes' },
  ],
  'etisalat-sme-data': [
    { variationCode: '9mobile-sme-1gb', name: '9mobile SME 1GB', variationAmount: '300.00', fixedPrice: 'Yes' },
    { variationCode: '9mobile-sme-2gb', name: '9mobile SME 2GB', variationAmount: '500.00', fixedPrice: 'Yes' },
  ],
  'showmax': [
    { variationCode: 'showmax-mobile',        name: 'ShowMax Mobile',          variationAmount: '1200.00', fixedPrice: 'Yes' },
    { variationCode: 'showmax-mobile-sports', name: 'ShowMax Mobile + Sports', variationAmount: '2400.00', fixedPrice: 'Yes' },
    { variationCode: 'showmax-standard',      name: 'ShowMax Standard',        variationAmount: '3200.00', fixedPrice: 'Yes' },
  ],
}
