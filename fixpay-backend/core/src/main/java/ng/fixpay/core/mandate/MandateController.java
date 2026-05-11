package ng.fixpay.core.mandate;

import jakarta.validation.Valid;
import ng.fixpay.core.mandate.dto.CreateMandateRequest;
import ng.fixpay.core.mandate.dto.MandateResponse;
import ng.fixpay.shared.dto.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mandates")
public class MandateController {

    private final MandateService mandateService;

    public MandateController(MandateService mandateService) {
        this.mandateService = mandateService;
    }

    @PostMapping
    public ApiResponse<MandateResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateMandateRequest request
    ) {
        return ApiResponse.ok("Mandate created", mandateService.create(jwt, request));
    }

    @GetMapping
    public ApiResponse<List<MandateResponse>> listMine(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(mandateService.listMine(jwt));
    }

    @GetMapping("/{mandateReference}")
    public ApiResponse<MandateResponse> getMine(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String mandateReference
    ) {
        return ApiResponse.ok(mandateService.getMine(jwt, mandateReference));
    }

    @PostMapping("/{mandateReference}/sync")
    public ApiResponse<MandateResponse> sync(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String mandateReference
    ) {
        return ApiResponse.ok("Mandate synchronized", mandateService.syncMine(jwt, mandateReference));
    }
}
