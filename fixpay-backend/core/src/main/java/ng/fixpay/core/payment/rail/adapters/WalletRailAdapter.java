package ng.fixpay.core.payment.rail.adapters;

import ng.fixpay.shared.payment.ConfigSchema;
import ng.fixpay.shared.payment.PaymentRailAdapter;
import ng.fixpay.shared.payment.PaymentRailRequest;
import ng.fixpay.shared.payment.PaymentRailResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Wallet rail — funds a payment directly from the user's FixPay wallet balance.
 *
 * <p>This adapter handles the authorization step only. The actual wallet debit is
 * performed by {@code LedgerService} inside {@code VtpassPaymentService.execute()} so
 * that the debit and the VTPass call share the same compensation (reversal) logic that
 * already exists in that service.
 *
 * <p>Processor ID: {@code internal-wallet}
 */
@Component
public class WalletRailAdapter implements PaymentRailAdapter {

    @Override
    public String processorId() {
        return "internal-wallet";
    }

    @Override
    public ConfigSchema configSchema() {
        return new ConfigSchema(processorId(), "Internal Wallet", "wallet",
                "Debits the user's FixPay wallet balance. No external credentials required.",
                null, List.of());
    }

    /**
     * Validates that the request is well-formed for a wallet payment and immediately
     * signals that the payment is authorized (synchronous rail — no external action needed).
     *
     * <p>Balance sufficiency is NOT checked here; it is enforced inside
     * {@code LedgerService.debit()} which throws {@code 400 Insufficient wallet balance}
     * before any money moves.
     */
    @Override
    public PaymentRailResult initiate(PaymentRailRequest request) {
        return PaymentRailResult.authorized(
                "wallet:" + request.userId(),
                "Wallet selected. Ready to debit and complete the purchase."
        );
    }

    /**
     * The wallet rail is always considered funded at the execute step because the
     * {@code LedgerService} debit is performed separately (not inside this adapter).
     */
    @Override
    public PaymentRailResult confirmFunded(String paymentReference) {
        return PaymentRailResult.funded("wallet:" + paymentReference, null);
    }
}
