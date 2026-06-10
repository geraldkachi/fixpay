package ng.fixpay.core.payment.rail.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentRailAuditLogRepository extends JpaRepository<PaymentRailAuditLog, UUID> {

    Page<PaymentRailAuditLog> findByEntityIdOrderByCreatedAtDesc(UUID entityId, Pageable pageable);

    Page<PaymentRailAuditLog> findByAdminUserIdOrderByCreatedAtDesc(UUID adminUserId, Pageable pageable);

    Page<PaymentRailAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
