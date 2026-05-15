package ng.fixpay.shared.payment;

/**
 * Result of a connectivity test for a payment rail processor.
 *
 * @param success   Whether the processor's endpoint responded successfully
 * @param message   Human-readable summary (e.g. "OK — Paystack API is reachable")
 * @param latencyMs Round-trip latency in milliseconds; {@code -1} if not measured
 */
public record ConnectionTestResult(boolean success, String message, long latencyMs) {

    public static ConnectionTestResult ok(String message, long latencyMs) {
        return new ConnectionTestResult(true, message, latencyMs);
    }

    public static ConnectionTestResult failed(String message) {
        return new ConnectionTestResult(false, message, -1);
    }
}
