package ng.fixpay.core.admin;

import ng.fixpay.core.admin.dto.SuspendTenantRequest;
import ng.fixpay.core.tenant.domain.Tenant;
import ng.fixpay.core.portal.domain.ApiKeyRepository;
import ng.fixpay.core.portal.domain.IpWhitelistRuleRepository;
import ng.fixpay.core.tenant.domain.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TenantAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class TenantAdminControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    TenantRepository tenantRepository;

    @MockBean
    ApiKeyRepository apiKeyRepository;

    @MockBean
    IpWhitelistRuleRepository ipWhitelistRuleRepository;

    @Test
    void list_withFilters_returnsPagedResult() throws Exception {
        Tenant tenant = sampleTenant(UUID.randomUUID(), "acme", "Acme Ltd", Tenant.Status.ACTIVE);
        Page<Tenant> page = new PageImpl<>(List.of(tenant), PageRequest.of(0, 20), 1);

        when(tenantRepository.search(eq(Tenant.Status.ACTIVE), eq(Tenant.Plan.STARTER), eq(Tenant.KybStatus.PENDING), eq("acme"), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/tenants")
                        .param("status", "ACTIVE")
                        .param("plan", "starter")
                        .param("kybStatus", "pending")
                        .param("search", "acme"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].slug").value("acme"))
                .andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"));
    }

    @Test
    void get_existingTenant_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(tenantRepository.findById(id)).thenReturn(Optional.of(sampleTenant(id, "demo", "Demo Tenant", Tenant.Status.ACTIVE)));

        mockMvc.perform(get("/api/admin/tenants/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.slug").value("demo"));
    }

    @Test
    void update_invalidPlan_returns500ByGlobalHandler() throws Exception {
        UUID id = UUID.randomUUID();
        when(tenantRepository.findById(id)).thenReturn(Optional.of(sampleTenant(id, "demo", "Demo Tenant", Tenant.Status.ACTIVE)));

        mockMvc.perform(patch("/api/admin/tenants/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plan\":\"INVALID\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"));
    }

    @Test
    void suspend_offboardedTenant_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        when(tenantRepository.findById(id)).thenReturn(Optional.of(sampleTenant(id, "demo", "Demo Tenant", Tenant.Status.OFFBOARDED)));

        mockMvc.perform(post("/api/admin/tenants/{id}/suspend", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"fraud\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
    }

    @Test
    void updateFeatureFlags_updatesAndReturnsTenant() throws Exception {
        UUID id = UUID.randomUUID();
        Tenant tenant = sampleTenant(id, "demo", "Demo Tenant", Tenant.Status.ACTIVE);
        when(tenantRepository.findById(id)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(patch("/api/admin/tenants/{id}/feature-flags", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"walletTransfers\":true,\"intlAirtime\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.featureFlags.walletTransfers").value(true));
    }

    private Tenant sampleTenant(UUID id, String slug, String name, Tenant.Status status) throws Exception {
        Tenant tenant = newTenant();
        setField(tenant, "id", id);
        setField(tenant, "slug", slug);
        setField(tenant, "name", name);
        setField(tenant, "status", status);
        setField(tenant, "plan", Tenant.Plan.STARTER);
        setField(tenant, "kybStatus", Tenant.KybStatus.PENDING);
        setField(tenant, "supportEmail", "ops@fixpay.ng");
        setField(tenant, "supportPhone", "+2348000000000");
        setField(tenant, "featureFlags", Map.of("walletTransfers", true));
        setField(tenant, "whitelabelConfig", Map.of("primaryColor", "#111111"));
        return tenant;
    }

    private Tenant newTenant() throws Exception {
        var ctor = Tenant.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
