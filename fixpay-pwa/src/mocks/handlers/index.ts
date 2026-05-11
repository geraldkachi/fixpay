import { authHandlers }        from './auth'
import { tenantHandlers }      from './tenant'
import { walletHandlers }      from './wallet'
import { paymentHandlers }     from './payments'
import { kycHandlers }         from './kyc'
import { transferHandlers }    from './transfer'
import { disputeHandlers }     from './dispute'
import { directDebitHandlers } from './directdebit'

export const handlers = [
  ...tenantHandlers,
  ...authHandlers,
  ...kycHandlers,
  ...walletHandlers,
  ...paymentHandlers,
  ...transferHandlers,
  ...disputeHandlers,
  ...directDebitHandlers,
]
