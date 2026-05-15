package ng.fixpay.shared.payment;

import java.util.List;

/**
 * Self-describing configuration schema for a payment rail processor.
 *
 * <p>Each {@link PaymentRailAdapter} exposes a {@code ConfigSchema} so the admin
 * wizard can render a typed configuration form for that processor without requiring
 * any code changes on the platform side.
 *
 * @param processorId    Matches {@link PaymentRailAdapter#processorId()}
 * @param displayName    Human-readable name shown in the wizard (e.g. "Paystack Card")
 * @param category       Grouping hint: {@code "wallet"}, {@code "card"}, {@code "ussd"},
 *                       {@code "bank_transfer"}, {@code "mandate"}, {@code "other"}
 * @param description    Short description of the processor
 * @param documentationUrl Optional link to provider documentation
 * @param fields         Ordered list of configuration fields
 */
public record ConfigSchema(
        String processorId,
        String displayName,
        String category,
        String description,
        String documentationUrl,
        List<ConfigFieldSchema> fields) {}
