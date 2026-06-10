package ng.fixpay.core.payment.provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link VtpassServiceRegistry} fail-open fallback behavior.
 *
 * <p>The registry must never block payments solely because the VTpass info-API is
 * transiently unavailable:
 * <ul>
 *   <li>A warm cache is retained when a scheduled refresh cycle fails completely.</li>
 *   <li>An empty cache after a failed refresh is fail-open: {@link VtpassServiceRegistry#isAvailable}
 *       returns {@code true} for any non-blank serviceId.</li>
 *   <li>A partial category failure loads only the categories that succeeded.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class VtpassServiceRegistryFallbackTest {

    @Mock
    private VtpassClient vtpassClient;

    // ── Retain warm cache on full refresh failure ─────────────────────────────

    /**
     * Seeds the cache with one successful refresh, then simulates a complete outage
     * on the next refresh cycle. The existing cache entry must be retained.
     */
    @Test
    void refresh_allCategoriesFail_withPopulatedCache_shouldRetainExistingCache() {
        VtpassServiceDto airtelService = new VtpassServiceDto(
                "airtel", "Airtel Airtime",
                new BigDecimal("50.00"), new BigDecimal("50000.00"), "airtime");

        // Seed: first category of first refresh returns a service; all other calls throw.
        when(vtpassClient.getServicesForCategory(any()))
                .thenReturn(List.of(airtelService))           // call 1: seeds "airtel"
                .thenReturn(List.of())                        // calls 2-5: rest of first refresh
                .thenThrow(new RuntimeException("VTpass outage")); // all of second refresh

        VtpassServiceRegistry registry = new VtpassServiceRegistry(vtpassClient);
        registry.refresh(); // seeds "airtel" — loaded > 0, cache updated
        registry.refresh(); // all categories fail — retain existing cache

        assertTrue(registry.isAvailable("airtel"),
                "Cache entry from the previous cycle must survive a full refresh failure");
    }

    // ── Fail-open when cache is empty and all categories fail ─────────────────

    /**
     * When the cache is empty and all categories fail, {@code isAvailable()} must
     * return {@code true} for any valid serviceId (fail-open mode), while still
     * rejecting null and blank serviceIds.
     */
    @Test
    void refresh_emptyCache_allCategoriesFail_shouldBeFailOpen() {
        when(vtpassClient.getServicesForCategory(any()))
                .thenThrow(new RuntimeException("VTpass outage"));

        VtpassServiceRegistry registry = new VtpassServiceRegistry(vtpassClient);
        registry.refresh();

        assertTrue(registry.isAvailable("airtel"),
                "Empty cache must be fail-open to avoid blocking all payments on VTpass outage");
        assertTrue(registry.isAvailable("dstv"),
                "Fail-open must apply regardless of serviceId");
        assertFalse(registry.isAvailable(null),
                "null serviceId must be rejected even in fail-open mode");
        assertFalse(registry.isAvailable(""),
                "blank serviceId must be rejected even in fail-open mode");
    }

    // ── Partial category failure ───────────────────────────────────────────────

    /**
     * When some categories fail but others succeed, the registry must cache only the
     * services from the successful categories and reject services from failed ones.
     */
    @Test
    void refresh_partialCategoryFailure_shouldLoadSuccessfulCategories() {
        VtpassServiceDto mtnData = new VtpassServiceDto(
                "mtn-data", "MTN Data",
                new BigDecimal("100.00"), new BigDecimal("20000.00"), "data");

        when(vtpassClient.getServicesForCategory("airtime"))
                .thenThrow(new RuntimeException("timeout"));
        when(vtpassClient.getServicesForCategory("data"))
                .thenReturn(List.of(mtnData));
        when(vtpassClient.getServicesForCategory("tv-subscription"))
                .thenThrow(new RuntimeException("timeout"));
        when(vtpassClient.getServicesForCategory("electricity-bill"))
                .thenThrow(new RuntimeException("timeout"));
        when(vtpassClient.getServicesForCategory("education"))
                .thenThrow(new RuntimeException("timeout"));

        VtpassServiceRegistry registry = new VtpassServiceRegistry(vtpassClient);
        registry.refresh();

        assertTrue(registry.isAvailable("mtn-data"),
                "Service from the successful category must be available");
        assertFalse(registry.isAvailable("airtel"),
                "Service from a failed category must not appear in cache");
    }
}
