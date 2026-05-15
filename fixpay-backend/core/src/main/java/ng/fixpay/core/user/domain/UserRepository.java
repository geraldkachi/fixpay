package ng.fixpay.core.user.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByKeycloakId(UUID keycloakId);
    Optional<AppUser> findByPhone(String phone);
    Optional<AppUser> findByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
}
