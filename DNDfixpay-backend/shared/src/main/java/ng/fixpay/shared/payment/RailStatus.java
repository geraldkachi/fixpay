package ng.fixpay.shared.payment;

/**
 * Lifecycle status of a payment rail operation.
 *
 * <ul>
 *   <li>{@link #AUTHORIZED} — synchronous rail (e.g. Wallet) is ready to proceed; no
 *       external confirmation required before executing the bill purchase.</li>
 *   <li>{@link #PENDING_AUTHORIZATION} — async rail (e.g. Card, USSD, Bank Transfer)
 *       requires the user to complete an external action before execution.</li>
 *   <li>{@link #FUNDED} — funds have been confirmed received; bill purchase may begin.</li>
 *   <li>{@link #FAILED} — rail initiation or confirmation failed; do not proceed.</li>
 * </ul>
 */
public enum RailStatus {
    /** Synchronous rail ready — proceed directly to bill provider. */
    AUTHORIZED,
    /** Awaiting external user action (redirect, USSD dial, bank transfer). */
    PENDING_AUTHORIZATION,
    /** External payment confirmed received — proceed to bill provider. */
    FUNDED,
    /** Rail failed — abort payment. */
    FAILED
}
