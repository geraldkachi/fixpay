package ng.fixpay.core.admin;

import ng.fixpay.core.ledger.domain.LedgerEntry;
import ng.fixpay.core.ledger.domain.LedgerEntryRepository;
import ng.fixpay.core.portal.domain.ApiKeyRepository;
import ng.fixpay.core.portal.domain.IpWhitelistRuleRepository;
import ng.fixpay.core.tenant.domain.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GlobalTransactionAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class GlobalTransactionAdminControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    LedgerEntryRepository ledgerEntryRepository;

    @MockBean
    ApiKeyRepository apiKeyRepository;

    @MockBean
    IpWhitelistRuleRepository ipWhitelistRuleRepository;

    @MockBean
    TenantRepository tenantRepository;

    @Test
    void list_withFilters_returnsPagedLedger() throws Exception {
        LedgerEntry e = new LedgerEntry(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "corr-1", LedgerEntry.EntryType.DEBIT,
                new BigDecimal("100.00"), new BigDecimal("900.00"),
                "NGN", "REF-001", "Wallet debit");

        when(ledgerEntryRepository.adminSearch(any(), any(), eq(LedgerEntry.EntryType.DEBIT), any(), any(), eq("REF"), any()))
                .thenReturn(new PageImpl<>(List.of(e), PageRequest.of(0, 50), 1));

        mockMvc.perform(get("/api/admin/transactions")
                        .param("entryType", "debit")
                        .param("reference", "REF")
                        .param("from", "2026-05-01T00:00:00Z")
                        .param("to", "2026-05-30T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].entryType").value("DEBIT"))
                .andExpect(jsonPath("$.data.content[0].reference").value("REF-001"));
    }

    @Test
    void list_invalidEntryType_returns400() throws Exception {
        mockMvc.perform(get("/api/admin/transactions").param("entryType", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
    }

    @Test
    void list_invalidDate_returns500ByGlobalHandler() throws Exception {
        mockMvc.perform(get("/api/admin/transactions").param("from", "not-an-instant"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"));
    }

    @Test
    void get_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(ledgerEntryRepository.findById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/transactions/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void byCorrelation_returnsEntries() throws Exception {
        LedgerEntry e = new LedgerEntry(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "corr-xyz", LedgerEntry.EntryType.CREDIT,
                new BigDecimal("100.00"), new BigDecimal("1000.00"),
                "NGN", "REF-002", "Wallet credit");

        when(ledgerEntryRepository.findByCorrelationIdOrderByCreatedAtAsc("corr-xyz")).thenReturn(List.of(e));

        mockMvc.perform(get("/api/admin/transactions/correlation/{cid}", "corr-xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].correlationId").value("corr-xyz"));
    }
}
