package ng.fixpay.core.tenant.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findBySlugAndActiveTrue(String slug);
    Optional<Tenant> findByContactEmailIgnoreCaseAndActiveTrue(String email);

    @Query("SELECT t FROM Tenant t WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:plan IS NULL OR t.plan = :plan) AND " +
           "(:kybStatus IS NULL OR t.kybStatus = :kybStatus) AND " +
          "(:hasSearch = false OR LOWER(t.name) LIKE CONCAT('%', :search, '%') " +
          "  OR LOWER(t.slug) LIKE CONCAT('%', :search, '%'))")
    Page<Tenant> search(
            @org.springframework.data.repository.query.Param("status")    Tenant.Status status,
            @org.springframework.data.repository.query.Param("plan")      Tenant.Plan plan,
            @org.springframework.data.repository.query.Param("kybStatus") Tenant.KybStatus kybStatus,
           @org.springframework.data.repository.query.Param("hasSearch") boolean hasSearch,
            @org.springframework.data.repository.query.Param("search")    String search,
            Pageable pageable);
}
