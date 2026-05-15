package ng.fixpay.core.payment.rail.domain;

import ng.fixpay.core.payment.dto.VtpassPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PaymentRailConfigRepository extends JpaRepository<PaymentRailConfig, UUID> {

    /**
     * Returns all enabled configs for the given tenant and method, ordered by priority.
     * Used by the registry to resolve the active processor with fallback support.
     */
    @Query("""
        SELECT c FROM PaymentRailConfig c
        WHERE c.paymentMethod = :method
          AND c.enabled = true
          AND (c.tenantId = :tenantId OR c.tenantId IS NULL)
        ORDER BY
          CASE WHEN c.tenantId = :tenantId THEN 0 ELSE 1 END,
          c.priority ASC
        """)
    List<PaymentRailConfig> findEnabledByTenantAndMethod(
            @Param("tenantId") UUID tenantId,
            @Param("method") VtpassPaymentMethod method);

    /** Returns all configs for admin listing (tenant-scoped). */
    List<PaymentRailConfig> findByTenantIdOrderByPaymentMethodAscPriorityAsc(UUID tenantId);

    /** Returns global default configs (tenant_id IS NULL). */
    List<PaymentRailConfig> findByTenantIdIsNullOrderByPaymentMethodAscPriorityAsc();
}
