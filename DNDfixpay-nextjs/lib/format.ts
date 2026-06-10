/** Format kobo → NGN string e.g. "₦1,250.00" */
export function formatNaira(kobo: number): string {
  return new Intl.NumberFormat('en-NG', {
    style: 'currency',
    currency: 'NGN',
    minimumFractionDigits: 2,
  }).format(kobo / 100)
}

/** Format ISO date string to locale date */
export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-NG', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

/** Mask account number: show last 4 digits */
export function maskAccount(account: string): string {
  return account.length > 4 ? `****${account.slice(-4)}` : account
}
