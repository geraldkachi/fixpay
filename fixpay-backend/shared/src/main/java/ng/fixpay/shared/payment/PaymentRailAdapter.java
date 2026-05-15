package ng.fixpay.shared.payment;

/**
 * Contract every payment rail processor must implement.
 *
 * <p>A <em>payment rail</em> is the mechanism used to collect funds before
 * a bill purchase is submitted to the bill provider (e.g. VTPass). FixPay
 * supports multiple rails running side-by-side; the active processor for each
 * rail is selected from the {@code payment_rail_config} table — no code
 * changes are required to switch processors.
 *
 * <h3>Lifecycle</h3>
 * <pre>
 *   initialize()
 *       └─► adapter.initiate(request)
 *               ├─ AUTHORIZED            → markAuthorized() → user calls execute()
 *               └─ PENDING_AUTHORIZATION → markPendingAuth() → user completes
 *                                          external action → execute()
 *
 *   execute()
 *       └─► adapter.confirmFunded(reference)
 *               ├─ FUNDED  → proceed to bill provider → success
 *               └─ FAILED  → abort payment
 * </pre>
 *
 * <h3>Registration</h3>
 * <p>Implementations must be Spring {@code @Component}s (or equivalent). The
 * {@link ng.fixpay.core.payment.rail.PaymentRailRegistry} discovers them via
 * Spring's {@code List<PaymentRailAdapter>} injection and indexes them by
 * {@link #processorId()}.
 */
public interface PaymentRailAdapter {

    /**
     * Unique, stable identifier for this processor implementation.
     *
     * <p>This string is stored in {@code payment_rail_config.processor_id} and
     * is used by the registry to select the right adapter at runtime.
     *
     * <p>Convention: {@code provider-method}, e.g. {@code internal-wallet},
     * {@code paystack-card}, {@code monnify-transfer}.
     */
    String processorId();

    /**
     * Initiate the funding flow for this rail.
     *
     * <p>For <em>synchronous</em> rails (e.g. Wallet): validates preconditions and
     * returns {@link RailStatus#AUTHORIZED} immediately.
     *
     * <p>For <em>asynchronous</em> rails (e.g. Card, USSD, Bank Transfer): creates a
     * payment session on the provider, returns {@link RailStatus#PENDING_AUTHORIZATION}
     * together with the redirect URL, USSD code, or virtual account number the user
     * must act on.
     *
     * @param request normalised payment context
     * @return result describing the next step; never null
     */
    PaymentRailResult initiate(PaymentRailRequest request);

    /**
     * Confirm that funds have been received / authorised for the given payment.
     *
     * <p>Called from the {@code execute()} step before the bill purchase is
     * submitted to the bill provider.
     *
     * <ul>
     *   <li>Wallet rail: always returns {@link RailStatus#FUNDED} (wallet debit is
     *       handled separately by {@code LedgerService} in the service layer).</li>
     *   <li>Async rails: checks whether a provider webhook has been received confirming
     *       the payment, then returns {@link RailStatus#FUNDED} or {@link RailStatus#FAILED}.</li>
     * </ul>
     *
     * @param paymentReference the FixPay payment reference (e.g. "FP-VTP-XXXXXXXX")
     * @return result with status FUNDED or FAILED; never null
     */
    PaymentRailResult confirmFunded(String paymentReference);

    /**
     * Returns the configuration schema for this processor.
     *
     * <p>The admin wizard uses this schema to render a typed form for the processor's
     * {@code config_json} fields — no code changes needed when a new processor is added.
     *
     * <p>Default implementation returns an empty schema; processors should override this.
     */
    default ConfigSchema configSchema() {
        return new ConfigSchema(processorId(), processorId(), "other",
                "No schema defined.", null, java.util.List.of());
    }

    /**
     * Tests connectivity to the processor's API endpoint.
     *
     * <p>Called from the admin wizard's "Test Connection" step. The default
     * implementation returns a "not implemented" failure; processors should override.
     *
     * @param processorConfig the config_json map for this processor instance
     * @return test result with success flag and latency; never null
     */
    default ConnectionTestResult testConnectivity(java.util.Map<String, String> processorConfig) {
        return ConnectionTestResult.failed("Connectivity test not implemented for processor: " + processorId());
    }
}
