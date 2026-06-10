// VTpass Response Codes and status mapping
export interface VtpassCodeInfo {
  meaning: string
  note?: string
  severity: 'success' | 'pending' | 'warning' | 'error'
}

export const VTPASS_CODES: Record<string, VtpassCodeInfo> = {
  // Verification Response Codes
  '020': { meaning: 'Biller Confirmed', note: 'Biller is confirmed successfully.', severity: 'success' },

  // Transaction / Requery Response Codes
  '000': { meaning: 'Transaction Processed', note: 'Transaction is processed. Check actual state (initiated, pending, delivered).', severity: 'success' },
  '099': { meaning: 'Transaction is Processing', note: 'Transaction is currently processing. Requery to ascertain current status.', severity: 'pending' },
  '001': { meaning: 'Transaction Query', note: 'The current status of a given transaction carried out on the platform.', severity: 'success' },
  '044': { meaning: 'Transaction Resolved', note: 'Transaction has been resolved.', severity: 'success' },
  '091': { meaning: 'Transaction Not Processed', note: 'Transaction is not processed and you will not be charged.', severity: 'warning' },
  '016': { meaning: 'Transaction Failed', note: 'Transaction failed.', severity: 'error' },
  '010': { meaning: 'Variation Code Does Not Exist', note: 'Invalid variation code used.', severity: 'error' },
  '011': { meaning: 'Invalid Arguments', note: 'One or more required arguments are missing from your request.', severity: 'error' },
  '012': { meaning: 'Product Does Not Exist', note: 'Product does not exist.', severity: 'error' },
  '013': { meaning: 'Below Minimum Amount Allowed', note: 'Amount is below the minimum allowed limit.', severity: 'error' },
  '014': { meaning: 'Request ID Already Exists', note: 'This RequestID was used for a previous transaction.', severity: 'error' },
  '015': { meaning: 'Invalid Request ID', note: 'This RequestID was not found or used on the platform.', severity: 'error' },
  '017': { meaning: 'Above Maximum Amount Allowed', note: 'Amount is above the maximum allowed limit.', severity: 'error' },
  '018': { meaning: 'Low Wallet Balance', note: 'Insufficient funds in wallet to cover transaction cost.', severity: 'error' },
  '019': { meaning: 'Likely Duplicate Transaction', note: 'Attempted to purchase same service for same biller within 30 seconds.', severity: 'warning' },
  '021': { meaning: 'Account Locked', note: 'Your account is locked.', severity: 'error' },
  '022': { meaning: 'Account Suspended', note: 'Your account is suspended.', severity: 'error' },
  '023': { meaning: 'API Access Not Enabled', note: 'API access is not enabled for your account.', severity: 'error' },
  '024': { meaning: 'Account Inactive', note: 'Your account is inactive.', severity: 'error' },
  '025': { meaning: 'Recipient Bank Invalid', note: 'Bank code for bank transfer is invalid.', severity: 'error' },
  '026': { meaning: 'Recipient Account Could Not Be Verified', note: 'Bank account number could not be verified.', severity: 'error' },
  '027': { meaning: 'IP Not Whitelisted', note: 'IP is not whitelisted, please contact support.', severity: 'error' },
  '028': { meaning: 'Product Not Whitelisted', note: 'Product is not whitelisted on your account.', severity: 'error' },
  '030': { meaning: 'Biller Not Reachable', note: 'The service provider/biller is unreachable at this point.', severity: 'error' },
  '031': { meaning: 'Below Minimum Quantity Allowed', note: 'Requested quantity is below the allowed limit.', severity: 'error' },
  '032': { meaning: 'Above Maximum Quantity Allowed', note: 'Requested quantity is above the allowed limit.', severity: 'error' },
  '034': { meaning: 'Service Suspended', note: 'This service has been suspended temporarily.', severity: 'error' },
  '035': { meaning: 'Service Inactive', note: 'This service is currently turned off.', severity: 'error' },
  '040': { meaning: 'Transaction Reversal', note: 'Transaction reversal to wallet.', severity: 'warning' },
  '083': { meaning: 'System Error', note: 'System error occurred. Please contact tech support.', severity: 'error' },
  '085': { meaning: 'Improper Request ID (No Date)', note: 'Request ID must contain today\'s date in YYYYMMDD format.', severity: 'error' },
  '087': { meaning: 'Invalid Credentials', note: 'Authentication credentials are invalid.', severity: 'error' },
  '089': { meaning: 'Request Is Processing', note: 'Please wait before making another request.', severity: 'pending' },
}

export interface ResolvedOutcome {
  message: string
  note: string
  severity: 'success' | 'pending' | 'warning' | 'error'
  isSuccess: boolean
  isPending: boolean
  isFatal: boolean
}

export function resolveVtpassCode(code: string | undefined): ResolvedOutcome {
  if (!code) {
    return {
      message: 'Unknown Response',
      note: 'Treat as pending. Please initiate a transaction requery to confirm status.',
      severity: 'pending',
      isSuccess: false,
      isPending: true,
      isFatal: false,
    }
  }

  const info = VTPASS_CODES[code]
  if (!info) {
    return {
      message: `Response Code ${code}`,
      note: 'Unknown response code received. Treat as pending and initiate a transaction requery.',
      severity: 'pending',
      isSuccess: false,
      isPending: true,
      isFatal: false,
    }
  }

  return {
    message: info.meaning,
    note: info.note || '',
    severity: info.severity,
    isSuccess: code === '000' || code === '020' || code === '001' || code === '044',
    isPending: code === '099' || code === '089',
    isFatal: info.severity === 'error' && code !== '099' && code !== '089',
  }
}
