package ng.fixpay.core.payment.dto;

public record BillPaymentResponse(
        String requestId,
        String transaction_date,
        String amount,
        /** Electricity prepaid token (e.g. "5024-8167-3921-4856-7301"). */
        String token,
        /** Electricity units credited. */
        String units,
        /** JAMB/WAEC purchased PIN. */
        String Pin,
        /** Generic purchased code from VTpass. */
        String purchased_code
) {}
