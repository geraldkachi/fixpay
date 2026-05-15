package ng.fixpay.core.payment.provider;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Polls the VTpass service catalogue on startup and every 5 minutes, caching
 * every active {@code serviceID} per category.
 *
 * <p><b>Availability contract:</b> a serviceID is considered <em>available</em>
 * if and only if VTpass currently lists it under one of the supported categories.
 * When the cache is empty (e.g. VTpass info API unreachable at startup) the
 * check is <em>fail-open</em> — payments are allowed through rather than blocking
 * all transactions due to a monitoring API hiccup.
 */
@Component
public class VtpassServiceRegistry {

    private static final Logger log = LoggerFactory.getLogger(VtpassServiceRegistry.class);

    /** Categories to poll — must match the identifier values accepted by VTpass. */
    private static final List<String> CATEGORIES = List.of(
            "airtime",
            "data",
            "tv-subscription",
            "electricity-bill",
            "education"
    );

    private final VtpassClient vtpassClient;

    /** serviceID → service details. ConcurrentHashMap for thread-safe reads. */
    private volatile Map<String, VtpassServiceDto> cache = new ConcurrentHashMap<>();

    public VtpassServiceRegistry(VtpassClient vtpassClient) {
        this.vtpassClient = vtpassClient;
    }

    @PostConstruct
    public void loadOnStartup() {
        refresh();
    }

    /**
     * Refreshes the service cache from VTpass every 5 minutes.
     * On failure, the previous cache is retained and a warning is logged.
     */
    @Scheduled(fixedDelay = 300_000)
    public void refresh() {
        Map<String, VtpassServiceDto> fresh = new ConcurrentHashMap<>();
        int loaded = 0;
        for (String category : CATEGORIES) {
            try {
                List<VtpassServiceDto> services = vtpassClient.getServicesForCategory(category);
                for (VtpassServiceDto dto : services) {
                    fresh.put(dto.serviceID(), dto);
                    loaded++;
                }
            } catch (Exception ex) {
                log.warn("VtpassServiceRegistry: failed to load category '{}': {}", category, ex.getMessage());
            }
        }

        if (loaded > 0) {
            this.cache = fresh;
            log.info("VtpassServiceRegistry: loaded {} services across {} categories", loaded, CATEGORIES.size());
        } else if (fresh.isEmpty() && !this.cache.isEmpty()) {
            log.warn("VtpassServiceRegistry: refresh returned zero services — retaining previous cache ({} entries)", this.cache.size());
        } else {
            log.warn("VtpassServiceRegistry: no services loaded and cache is empty — fail-open mode active");
        }
    }

    /**
     * Returns {@code true} if the serviceID is currently listed by VTpass.
     *
     * <p>Fail-open: returns {@code true} when the cache is empty so transient
     * VTpass info-API unavailability does not block all bill payments.
     */
    public boolean isAvailable(String serviceId) {
        if (serviceId == null || serviceId.isBlank()) return false;
        Map<String, VtpassServiceDto> snapshot = this.cache;
        if (snapshot.isEmpty()) {
            log.debug("VtpassServiceRegistry: cache empty — fail-open for serviceId='{}'", serviceId);
            return true;
        }
        return snapshot.containsKey(serviceId);
    }

    /** Returns the cached service details, if present. */
    public Optional<VtpassServiceDto> find(String serviceId) {
        if (serviceId == null) return Optional.empty();
        return Optional.ofNullable(this.cache.get(serviceId));
    }

    /** Exposed for tests only. */
    int cacheSize() {
        return cache.size();
    }
}
