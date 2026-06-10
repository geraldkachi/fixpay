package ng.fixpay.core.mandate;

import ng.fixpay.core.events.DomainEventPublisher;
import ng.fixpay.core.mandate.domain.NibssMandate;
import ng.fixpay.core.mandate.domain.NibssMandateRepository;
import ng.fixpay.core.mandate.dto.CreateMandateRequest;
import ng.fixpay.core.mandate.dto.MandateResponse;
import ng.fixpay.core.mandate.provider.MandateProviderClient;
import ng.fixpay.core.mandate.provider.MandateProviderResult;
import ng.fixpay.core.user.domain.AppUser;
import ng.fixpay.core.user.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MandateServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NibssMandateRepository mandateRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    @Mock
    private MandateProviderClient mandateProviderClient;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private MandateService mandateService;

    @Test
    void create_shouldPersistActiveStatusFromProvider() {
        UUID keycloakId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        AppUser user = org.mockito.Mockito.mock(AppUser.class);
        when(user.getId()).thenReturn(userId);
        when(user.getTenantId()).thenReturn(tenantId);

        CreateMandateRequest request = new CreateMandateRequest(
                "044",
                "0123456789",
                new BigDecimal("50000.00"),
                LocalDate.now(),
                LocalDate.now().plusYears(1)
        );

        when(jwt.getSubject()).thenReturn(keycloakId.toString());
        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(user));
        when(mandateProviderClient.createMandate(any(), eq(request)))
                .thenReturn(new MandateProviderResult("active", "Approved", "PRV-001", "000", "{}"));
        when(mandateRepository.save(any(NibssMandate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MandateResponse response = mandateService.create(jwt, request);

        assertEquals("active", response.status());
        assertEquals("PRV-001", response.providerReference());
        assertEquals("Approved", response.providerMessage());

        ArgumentCaptor<NibssMandate> captor = ArgumentCaptor.forClass(NibssMandate.class);
        verify(mandateRepository).save(captor.capture());
        assertEquals("active", captor.getValue().getStatus());
        assertEquals("PRV-001", captor.getValue().getProviderReference());
        verify(eventPublisher, times(1)).publish(eq("mandate.status.updated"), any());
    }

    @Test
    void syncMine_shouldSetPendingStatusWhenProviderReturnsPending() {
        UUID keycloakId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        AppUser user = org.mockito.Mockito.mock(AppUser.class);
        when(user.getId()).thenReturn(userId);

        NibssMandate mandate = new NibssMandate(
                userId,
                tenantId,
                "NPSM-ABC123456789",
                "044",
                "0123456789",
                new BigDecimal("50000.00"),
                LocalDate.now(),
                LocalDate.now().plusYears(1)
        );
        mandate.updateStatus("active", "Approved", "PRV-OLD");

        when(jwt.getSubject()).thenReturn(keycloakId.toString());
        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(user));
        when(mandateRepository.findByMandateReference("NPSM-ABC123456789")).thenReturn(Optional.of(mandate));
        when(mandateProviderClient.syncMandateStatus("NPSM-ABC123456789", "PRV-OLD"))
                .thenReturn(new MandateProviderResult("pending", "Still processing", null, "102", "{}"));

        MandateResponse response = mandateService.syncMine(jwt, "NPSM-ABC123456789");

        assertEquals("pending", response.status());
        assertEquals("PRV-OLD", response.providerReference());
        assertEquals("Still processing", response.providerMessage());
        verify(eventPublisher, times(1)).publish(eq("mandate.status.updated"), any());
    }

    @Test
    void syncMine_shouldSetFailedStatusWhenProviderReturnsFailed() {
        UUID keycloakId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        AppUser user = org.mockito.Mockito.mock(AppUser.class);
        when(user.getId()).thenReturn(userId);

        NibssMandate mandate = new NibssMandate(
                userId,
                tenantId,
                "NPSM-ZZZ123456789",
                "044",
                "0123456789",
                new BigDecimal("10000.00"),
                LocalDate.now(),
                LocalDate.now().plusMonths(6)
        );
        mandate.updateStatus("pending", "Awaiting provider", "PRV-777");

        when(jwt.getSubject()).thenReturn(keycloakId.toString());
        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(user));
        when(mandateRepository.findByMandateReference("NPSM-ZZZ123456789")).thenReturn(Optional.of(mandate));
        when(mandateProviderClient.syncMandateStatus("NPSM-ZZZ123456789", "PRV-777"))
                .thenReturn(new MandateProviderResult("failed", "Mandate rejected", "PRV-777", "301", "{}"));

        MandateResponse response = mandateService.syncMine(jwt, "NPSM-ZZZ123456789");

        assertNotNull(response);
        assertEquals("failed", response.status());
        assertEquals("PRV-777", response.providerReference());
        assertEquals("Mandate rejected", response.providerMessage());
        verify(eventPublisher, times(1)).publish(eq("mandate.status.updated"), any());
    }

    // ── Provider outage ───────────────────────────────────────────────────────

    /**
     * When the mandate provider throws (e.g. NIBSS connection refused), the exception
     * must propagate and the mandate must NOT be persisted or any event published.
     */
    @Test
    void create_mandateProviderThrows_shouldNotPersistMandateOrPublishEvent() {
        UUID keycloakId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        AppUser user = org.mockito.Mockito.mock(AppUser.class);
        when(user.getId()).thenReturn(userId);
        when(user.getTenantId()).thenReturn(tenantId);

        CreateMandateRequest request = new CreateMandateRequest(
                "044", "0123456789",
                new BigDecimal("50000.00"),
                LocalDate.now(), LocalDate.now().plusYears(1)
        );

        when(jwt.getSubject()).thenReturn(keycloakId.toString());
        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(user));
        when(mandateProviderClient.createMandate(any(), eq(request)))
                .thenThrow(new RuntimeException("NIBSS connection refused"));

        assertThrows(RuntimeException.class, () -> mandateService.create(jwt, request));

        verify(mandateRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any(), any());
    }
}
