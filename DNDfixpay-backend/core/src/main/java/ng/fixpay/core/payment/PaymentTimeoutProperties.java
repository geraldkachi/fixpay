package ng.fixpay.core.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Runtime-tunable properties for payment timeout enforcement.
 *
 * <p>Bound from {@code fixpay.payment.*} in {@code application.yml} at startup.
 * Platform admins may update {@link #setTimeoutSeconds(int)} at runtime via
 * {@code PUT /api/admin/settings/payment-timeout} without restarting the service.
 */
@Component
@ConfigurationProperties(prefix = "fixpay.payment")
public class PaymentTimeoutProperties {

    /**
     * How long (in seconds) a payment may remain in a non-terminal state before the
     * timeout scheduler permanently fails it. Default: 60.
     *
     * <p>Volatile so that the scheduler thread always reads the latest value written
     * by the admin API thread.
     */
    private volatile int timeoutSeconds = 60;

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        if (timeoutSeconds < 1 || timeoutSeconds > 3600) {
            throw new IllegalArgumentException("timeoutSeconds must be between 1 and 3600");
        }
        this.timeoutSeconds = timeoutSeconds;
    }
}
