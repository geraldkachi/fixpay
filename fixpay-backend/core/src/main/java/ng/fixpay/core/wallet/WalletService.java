package ng.fixpay.core.wallet;

import ng.fixpay.core.user.domain.AppUser;
import ng.fixpay.core.user.domain.UserRepository;
import ng.fixpay.core.wallet.domain.Wallet;
import ng.fixpay.core.wallet.domain.WalletRepository;
import ng.fixpay.core.wallet.dto.WalletBalanceResponse;
import ng.fixpay.shared.exception.FixPayException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class WalletService {

    private final WalletRepository walletRepo;
    private final UserRepository   userRepo;

    public WalletService(WalletRepository walletRepo, UserRepository userRepo) {
        this.walletRepo = walletRepo;
        this.userRepo   = userRepo;
    }

    @Transactional(readOnly = true)
    public WalletBalanceResponse getBalance(Jwt jwt) {
        UUID keycloakId = UUID.fromString(jwt.getSubject());
        AppUser user = userRepo.findByKeycloakId(keycloakId)
                .orElseThrow(() -> FixPayException.notFound("User not found"));

        Wallet wallet = walletRepo.findByUserIdAndCurrency(user.getId(), "NGN")
                .orElseThrow(() -> FixPayException.notFound("Wallet not found"));

        return new WalletBalanceResponse(
                wallet.getId(),
                wallet.getCurrency(),
                wallet.getBalance(),
                wallet.getLedgerBalance(),
                wallet.getStatus()
        );
    }
}
