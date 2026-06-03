package ng.fixpay.core.dispute;

import jakarta.validation.Valid;
import ng.fixpay.core.dispute.dto.DisputeResponse;
import ng.fixpay.core.dispute.dto.RaiseDisputeRequest;
import ng.fixpay.shared.dto.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/disputes")
public class DisputeController {

    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    /** Returns a paginated-style response matching the PWA's `{ content: Dispute[] }` shape. */
    @GetMapping
    public ApiResponse<Map<String, Object>> list(@AuthenticationPrincipal Jwt jwt) {
        List<DisputeResponse> disputes = disputeService.listDisputes(jwt);
        return ApiResponse.ok(Map.of("content", disputes));
    }

    @GetMapping("/{id}")
    public ApiResponse<DisputeResponse> get(@AuthenticationPrincipal Jwt jwt,
                                             @PathVariable UUID id) {
        return ApiResponse.ok(disputeService.getDispute(jwt, id));
    }

    @PostMapping
    public ApiResponse<DisputeResponse> raise(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody RaiseDisputeRequest request) {
        return ApiResponse.ok("Dispute raised successfully", disputeService.raise(jwt, request));
    }
}
