package ng.fixpay.core.transfer;

import jakarta.validation.Valid;
import ng.fixpay.core.transfer.dto.*;
import ng.fixpay.shared.dto.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    /** NIP-enabled bank list (cached 24 h). */
    @GetMapping("/banks")
    public ApiResponse<List<NipBankDto>> banks() {
        return ApiResponse.ok(transferService.listBanks());
    }

    /** Resolves account number → account holder name before the user confirms. */
    @PostMapping("/verify-account")
    public ApiResponse<NameEnquiryResponse> verifyAccount(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody NameEnquiryRequest request) {
        return ApiResponse.ok(transferService.verifyAccount(request));
    }

    /** Initiates a bank (NIP) transfer from the user's FixPay wallet. */
    @PostMapping("/bank")
    public ApiResponse<TransferResponse> bankTransfer(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BankTransferRequest request) {
        return ApiResponse.ok("Transfer initiated", transferService.bankTransfer(jwt, request));
    }

    /** Internal P2P wallet transfer between two FixPay users. */
    @PostMapping("/wallet")
    public ApiResponse<TransferResponse> walletTransfer(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody WalletTransferRequest request) {
        return ApiResponse.ok("Transfer completed", transferService.walletTransfer(jwt, request));
    }
}
