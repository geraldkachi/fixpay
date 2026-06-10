package ng.fixpay.core.ledger.domain;

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

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LedgerEntryRepositoryTest {

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
    LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private UUID tenantA;
    private UUID tenantB;
    private UUID userA;

    @BeforeEach
    void reset() {
        jdbcTemplate.update("DELETE FROM ledger_entries");

        tenantA = UUID.randomUUID();
        tenantB = UUID.randomUUID();
        userA = UUID.randomUUID();

        insertEntry(tenantA, userA, "corr-1", "DEBIT", "REF-ONE", "2026-05-10T10:00:00Z");
        insertEntry(tenantA, userA, "corr-1", "CREDIT", "REF-ONE", "2026-05-11T10:00:00Z");
        insertEntry(tenantB, UUID.randomUUID(), "corr-2", "DEBIT", "REF-TWO", "2026-05-12T10:00:00Z");
    }

    @Test
    void adminSearch_filtersByTenantAndTypeAndReference() {
        Page<LedgerEntry> page = ledgerEntryRepository.adminSearch(
                tenantA,
                null,
                LedgerEntry.EntryType.DEBIT,
                null,
                null,
                "ONE",
                PageRequest.of(0, 20)
        );

        assertEquals(1, page.getTotalElements());
        assertEquals("REF-ONE", page.getContent().getFirst().getReference());
        assertEquals(LedgerEntry.EntryType.DEBIT, page.getContent().getFirst().getEntryType());
    }

    @Test
    void adminSearch_filtersByDateRange() {
        Page<LedgerEntry> page = ledgerEntryRepository.adminSearch(
                null,
                null,
                null,
                Instant.parse("2026-05-11T00:00:00Z"),
                Instant.parse("2026-05-11T23:59:59Z"),
                null,
                PageRequest.of(0, 20)
        );

        assertEquals(1, page.getTotalElements());
        assertEquals("REF-ONE", page.getContent().getFirst().getReference());
    }

    private void insertEntry(UUID tenantId, UUID userId, String corr, String type, String reference, String createdAt) {
        jdbcTemplate.update("""
            INSERT INTO ledger_entries (
              id, wallet_id, user_id, tenant_id, correlation_id, entry_type,
              amount, running_balance, currency, reference, description, created_at
            ) VALUES (
              gen_random_uuid(), gen_random_uuid(), ?, ?, ?, ?,
              100.00, 900.00, 'NGN', ?, 'test', ?::timestamptz
            )
            """,
            userId, tenantId, corr, type, reference, createdAt
        );
    }
}
