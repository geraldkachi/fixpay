package ng.fixpay.shared.payment;

/**
 * Normalised result returned by every {@link PaymentRailAdapter} operation.
 *
 * <p>Consumers should check {@link #status()} first. All other fields may be null
 * depending on the rail and the outcome.
 *
 * @param status                lifecycle status of this rail operation
 * @param externalReference     provider-side transaction or session ID
 * @param authorizationMessage  human-readable next-step message shown to the user
 * @param ussdCode              USSD dial string (USSD rail only)
 * @param checkoutUrl           provider redirect URL (Card / OPay rail only)
 * @param virtualAccountNumber  virtual account number (Bank Transfer rail only)
 * @param virtualAccountBank    bank name for the virtual account (Bank Transfer rail only)
 * @param rawProviderResponse   raw provider API response body for audit
 * @param failureReason         why the rail failed (only when status == FAILED)
 */
public record PaymentRailResult(
        RailStatus status,
        String externalReference,
        String authorizationMessage,
        String ussdCode,
        String checkoutUrl,
        String virtualAccountNumber,
        String virtualAccountBank,
        String rawProviderResponse,
        String failureReason
) {
    // ── Convenience factories ──────────────────────────────────────────────

    public static PaymentRailResult authorized(String externalReference, String message) {
        return new PaymentRailResult(RailStatus.AUTHORIZED, externalReference, message,
                null, null, null, null, null, null);
    }

    public static PaymentRailResult funded(String externalReference, String rawResponse) {
        return new PaymentRailResult(RailStatus.FUNDED, externalReference, "Payment funded",
                null, null, null, null, rawResponse, null);
    }

    public static PaymentRailResult pendingUssd(String externalReference, String ussdCode, String message) {
        return new PaymentRailResult(RailStatus.PENDING_AUTHORIZATION, externalReference, message,
                ussdCode, null, null, null, null, null);
    }

    public static PaymentRailResult pendingRedirect(String externalReference, String checkoutUrl, String message) {
        return new PaymentRailResult(RailStatus.PENDING_AUTHORIZATION, externalReference, message,
                null, checkoutUrl, null, null, null, null);
    }

    public static PaymentRailResult pendingTransfer(String externalReference,
                                                     String accountNumber, String bankName, String message) {
        return new PaymentRailResult(RailStatus.PENDING_AUTHORIZATION, externalReference, message,
                null, null, accountNumber, bankName, null, null);
    }

    public static PaymentRailResult pendingMandate(String externalReference, String message) {
        return new PaymentRailResult(RailStatus.PENDING_AUTHORIZATION, externalReference, message,
                null, null, null, null, null, null);
    }

    public static PaymentRailResult failed(String reason, String rawResponse) {
        return new PaymentRailResult(RailStatus.FAILED, null, null,
                null, null, null, null, rawResponse, reason);
    }

    // ── Status helpers ─────────────────────────────────────────────────────

    public boolean isReadyToExecute() {
        return status == RailStatus.AUTHORIZED || status == RailStatus.FUNDED;
    }

    public boolean isPending() {
        return status == RailStatus.PENDING_AUTHORIZATION;
    }

    public boolean isFailed() {
        return status == RailStatus.FAILED;
    }
}
