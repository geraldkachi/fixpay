package ng.fixpay.shared.payment;

import java.util.List;

/**
 * Describes a single configuration field for a payment rail processor.
 *
 * <p>Used by the no-code admin wizard to render dynamic configuration forms — no code
 * changes are needed when a new processor type is added.
 *
 * @param key         JSON key in the processorConfig map (e.g. {@code "apiKey"})
 * @param label       Human-readable form label
 * @param type        Field type: {@code "text"}, {@code "password"}, {@code "url"},
 *                    {@code "number"}, {@code "select"}, {@code "toggle"}
 * @param required    Whether the field must have a non-blank value
 * @param description Help text shown beneath the field
 * @param placeholder Example or default value for the field
 * @param options     For {@code "select"} type: the allowed values
 */
public record ConfigFieldSchema(
        String key,
        String label,
        String type,
        boolean required,
        String description,
        String placeholder,
        List<String> options) {

    // ─── Factory helpers ──────────────────────────────────────────────────────

    public static ConfigFieldSchema requiredText(String key, String label,
                                                  String description, String placeholder) {
        return new ConfigFieldSchema(key, label, "text", true, description, placeholder, List.of());
    }

    public static ConfigFieldSchema requiredUrl(String key, String label, String description) {
        return new ConfigFieldSchema(key, label, "url", true, description, "https://", List.of());
    }

    public static ConfigFieldSchema requiredSecret(String key, String label, String description) {
        return new ConfigFieldSchema(key, label, "password", true, description, "sk_...", List.of());
    }

    public static ConfigFieldSchema optionalText(String key, String label, String description) {
        return new ConfigFieldSchema(key, label, "text", false, description, "", List.of());
    }

    public static ConfigFieldSchema optionalSelect(String key, String label,
                                                    String description, List<String> options) {
        return new ConfigFieldSchema(key, label, "select", false, description, "", options);
    }
}
