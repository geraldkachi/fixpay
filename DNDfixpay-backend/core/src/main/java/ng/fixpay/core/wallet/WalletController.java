package ng.fixpay.core.wallet;

import ng.fixpay.core.wallet.dto.WalletBalanceResponse;
import ng.fixpay.shared.dto.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/balance")
    public ApiResponse<WalletBalanceResponse> balance(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(walletService.getBalance(jwt));
    }
}
