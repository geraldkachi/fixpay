package ng.fixpay.core.config;

import ng.fixpay.core.admin.GlobalTransactionAdminController;
import ng.fixpay.core.admin.TenantAdminController;
import ng.fixpay.core.ledger.domain.LedgerEntryRepository;
import ng.fixpay.core.portal.domain.ApiKeyRepository;
import ng.fixpay.core.portal.domain.IpWhitelistRuleRepository;
import ng.fixpay.core.tenant.domain.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {TenantAdminController.class, GlobalTransactionAdminController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc
class SecurityConfigAdminRoutesTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    JwtDecoder jwtDecoder;

    @MockBean
    TenantRepository tenantRepository;

    @MockBean
    LedgerEntryRepository ledgerEntryRepository;

    @MockBean
    ApiKeyRepository apiKeyRepository;

    @MockBean
    IpWhitelistRuleRepository ipWhitelistRuleRepository;

    @Test
    void adminRoute_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/tenants"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_001"));
    }

    @Test
    void adminRoute_withNonAdminRole_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/tenants")
                        .with(jwt().authorities(() -> "SUPPORT_AGENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("AUTH_002"));
    }

    @Test
    void adminRoute_withPlatformAdmin_returns200() throws Exception {
        when(tenantRepository.search(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/tenants")
                        .with(jwt().authorities(() -> "PLATFORM_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void adminTransactionsRoute_withFinanceOps_returns200() throws Exception {
        when(ledgerEntryRepository.adminSearch(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/transactions")
                        .with(jwt().authorities(() -> "FINANCE_OPS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
