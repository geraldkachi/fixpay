package ng.fixpay.core.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Nats;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@Component
public class NatsDomainEventPublisher implements DomainEventPublisher {

    private final ObjectMapper objectMapper;
    private final String natsUrl;
    private final boolean enabled;

    public NatsDomainEventPublisher(
            ObjectMapper objectMapper,
            @Value("${fixpay.nats.url:nats://localhost:4222}") String natsUrl,
            @Value("${fixpay.events.enabled:false}") boolean enabled
    ) {
        this.objectMapper = objectMapper;
        this.natsUrl = natsUrl;
        this.enabled = enabled;
    }

    @Override
    public void publish(String topic, Map<String, Object> payload) {
        if (!enabled) {
            return;
        }

        try (Connection connection = Nats.connect(natsUrl)) {
            byte[] data = objectMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);
            connection.publish(topic, data);
            connection.flush(Duration.ofSeconds(1));
        } catch (Exception ignored) {
            // Non-blocking event emission; API flow should not fail on telemetry/event outages.
        }
    }
}
