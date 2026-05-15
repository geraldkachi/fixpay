package ng.fixpay.core.admin.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {
    Optional<AdminUser> findByKeycloakUserId(String keycloakUserId);
    boolean existsByKeycloakUserId(String keycloakUserId);
}
