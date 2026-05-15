package ng.fixpay.core.tenant.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TenantRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    TenantRepository tenantRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void reset() {
        jdbcTemplate.update("DELETE FROM admin_users");
        jdbcTemplate.update("DELETE FROM tenants");

        insertTenant("acme", "Acme Ltd", true, "ACTIVE", "STARTER", "PENDING");
        insertTenant("beta", "Beta Labs", true, "SUSPENDED", "GROWTH", "IN_REVIEW");
        insertTenant("gamma", "Gamma Group", false, "OFFBOARDED", "ENTERPRISE", "APPROVED");
    }

    @Test
    void search_filtersByStatusAndSearch() {
        Page<Tenant> page = tenantRepository.search(
                Tenant.Status.ACTIVE,
                null,
                null,
                "acme",
                PageRequest.of(0, 10)
        );

        assertEquals(1, page.getTotalElements());
        assertEquals("acme", page.getContent().getFirst().getSlug());
    }

    @Test
    void search_filtersByPlanAndKybStatus() {
        Page<Tenant> page = tenantRepository.search(
                null,
                Tenant.Plan.GROWTH,
                Tenant.KybStatus.IN_REVIEW,
                null,
                PageRequest.of(0, 10)
        );

        assertEquals(1, page.getTotalElements());
        assertEquals("beta", page.getContent().getFirst().getSlug());
    }

    @Test
    void findBySlugAndActiveTrue_returnsOnlyActive() {
        Optional<Tenant> active = tenantRepository.findBySlugAndActiveTrue("acme");
        Optional<Tenant> inactive = tenantRepository.findBySlugAndActiveTrue("gamma");

        assertTrue(active.isPresent());
        assertTrue(inactive.isEmpty());
    }

    private void insertTenant(String slug, String name, boolean active, String status, String plan, String kybStatus) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO tenants (
              id, slug, name,
              primary_color, secondary_color, accent_color,
              feat_bill_payments, feat_direct_debit, feat_wallet_transfers,
              feat_intl_airtime, feat_dispute_management, feat_nibss_transfers,
              active, status, plan, kyb_status,
              feature_flags, whitelabel_config,
              created_at, updated_at
            ) VALUES (
              ?, ?, ?,
              '#111111', '#222222', '#333333',
              true, true, true,
              false, true, true,
              ?, ?, ?, ?,
              '{}'::jsonb, '{}'::jsonb,
              now(), now()
            )
            """,
            id, slug, name,
            active, status, plan, kybStatus
        );
    }
}
