package ng.fixpay.core.admin;

import ng.fixpay.core.admin.domain.AdminUser;
import ng.fixpay.core.admin.domain.AdminUserRepository;
import ng.fixpay.core.portal.domain.ApiKeyRepository;
import ng.fixpay.core.portal.domain.IpWhitelistRuleRepository;
import ng.fixpay.core.tenant.domain.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminUserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AdminUserRepository adminUserRepository;

    @MockBean
    ApiKeyRepository apiKeyRepository;

    @MockBean
    IpWhitelistRuleRepository ipWhitelistRuleRepository;

    @MockBean
    TenantRepository tenantRepository;

    @Test
    void list_returnsAdminUsers() throws Exception {
        AdminUser user = sampleAdminUser(UUID.randomUUID(), "kc-001", AdminUser.AdminRole.PLATFORM_ADMIN);
        when(adminUserRepository.findAll()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].keycloakUserId").value("kc-001"))
                .andExpect(jsonPath("$.data[0].role").value("PLATFORM_ADMIN"));
    }

    @Test
    void create_duplicateKeycloakUser_returns409() throws Exception {
        when(adminUserRepository.existsByKeycloakUserId("kc-dup")).thenReturn(true);

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keycloakUserId\":\"kc-dup\",\"username\":\"ops\",\"email\":\"ops@fixpay.ng\",\"role\":\"PLATFORM_ADMIN\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"));
    }

    @Test
    void create_validRequest_returns200() throws Exception {
        AdminUser user = sampleAdminUser(UUID.randomUUID(), "kc-new", AdminUser.AdminRole.SUPPORT_AGENT);
        when(adminUserRepository.existsByKeycloakUserId("kc-new")).thenReturn(false);
        when(adminUserRepository.save(any(AdminUser.class))).thenReturn(user);

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keycloakUserId\":\"kc-new\",\"username\":\"ops\",\"email\":\"ops@fixpay.ng\",\"role\":\"SUPPORT_AGENT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.keycloakUserId").value("kc-new"))
                .andExpect(jsonPath("$.data.role").value("SUPPORT_AGENT"));
    }

    @Test
    void changeRole_invalidRole_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        when(adminUserRepository.findById(id)).thenReturn(Optional.of(sampleAdminUser(id, "kc-001", AdminUser.AdminRole.PLATFORM_ADMIN)));

        mockMvc.perform(patch("/api/admin/users/{id}/role", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"NOT_A_ROLE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
    }

    @Test
    void deactivate_setsInactive() throws Exception {
        UUID id = UUID.randomUUID();
        AdminUser user = sampleAdminUser(id, "kc-001", AdminUser.AdminRole.PLATFORM_ADMIN);
        when(adminUserRepository.findById(id)).thenReturn(Optional.of(user));
        when(adminUserRepository.save(any(AdminUser.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(patch("/api/admin/users/{id}/deactivate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
    }

    private AdminUser sampleAdminUser(UUID id, String keycloakUserId, AdminUser.AdminRole role) throws Exception {
        AdminUser user = new AdminUser(keycloakUserId, "ops", "ops@fixpay.ng", role, null);
        Field field = AdminUser.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(user, id);
        return user;
    }
}
