package ng.fixpay.core.events;

import java.util.Map;

public interface DomainEventPublisher {
    void publish(String topic, Map<String, Object> payload);
}
