package ng.fixpay.core.payment.rail.adapters;

import ng.fixpay.core.mandate.MandateService;
import ng.fixpay.shared.exception.FixPayException;
import ng.fixpay.shared.payment.ConfigSchema;
import ng.fixpay.shared.payment.PaymentRailAdapter;
import ng.fixpay.shared.payment.PaymentRailRequest;
import ng.fixpay.shared.payment.PaymentRailResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * NIBSS direct debit rail — pulls funds from the customer's linked bank account
 * using a pre-authorised NIBSS mandate.
 *
 * <p>The mandate is set up ahead of time via the Mandates API. At payment time,
 * this adapter validates that the mandate is still active and returns
 * {@link ng.fixpay.shared.payment.RailStatus#PENDING_AUTHORIZATION} while NIBSS
 * processes the debit asynchronously. The NIBSS callback webhook marks the payment
 * as funded and triggers execution.
 *
 * <p>Processor ID: {@code nibss-direct-debit}
 */
@Component
public class NibssDirectDebitAdapter implements PaymentRailAdapter {

    private final MandateService mandateService;

    public NibssDirectDebitAdapter(MandateService mandateService) {
        this.mandateService = mandateService;
    }

    @Override
    public String processorId() {
        return "nibss-direct-debit";
    }

    @Override
    public ConfigSchema configSchema() {
        return new ConfigSchema(processorId(), "NIBSS Direct Debit", "mandate",
                "Mandate-backed direct debits via NIBSS. No external API credentials required.",
                null, List.of());
    }

    /**
     * Validates that the mandate is active and initiates the NIBSS pull debit.
     * Returns {@link ng.fixpay.shared.payment.RailStatus#PENDING_AUTHORIZATION} since
     * NIBSS processes debits asynchronously.
     */
    @Override
    public PaymentRailResult initiate(PaymentRailRequest request) {
        if (request.mandateReference() == null || request.mandateReference().isBlank()) {
            return PaymentRailResult.failed(
                    "mandateReference is required for the NIBSS Direct Debit payment method.", null);
        }

        boolean active = mandateService.isActiveMandate(request.userId(), request.mandateReference());
        if (!active) {
            return PaymentRailResult.failed(
                    "Mandate '" + request.mandateReference() + "' is not active. " +
                    "Please set up a valid mandate before using Direct Debit.", null);
        }

        return PaymentRailResult.pendingMandate(
                "NIBSS-" + request.paymentReference(),
                "Mandate debit initiated. Awaiting NIBSS confirmation."
        );
    }

    /**
     * Checks whether the NIBSS debit callback has been received.
     *
     * <p><strong>Current stub behaviour:</strong> returns FUNDED optimistically.
     * Full implementation will query the mandate provider client for confirmation status.
     */
    @Override
    public PaymentRailResult confirmFunded(String paymentReference) {
        // TODO: query NibssMandateProviderClient for confirmation of the pull debit
        return PaymentRailResult.funded("NIBSS-" + paymentReference, null);
    }
}
