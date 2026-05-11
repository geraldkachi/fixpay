package ng.fixpay.core.auth;

import ng.fixpay.core.auth.dto.RegisterRequest;
import ng.fixpay.core.auth.dto.RegisterResponse;
import ng.fixpay.core.user.domain.AppUser;
import ng.fixpay.core.user.domain.UserRepository;
import ng.fixpay.core.wallet.domain.Wallet;
import ng.fixpay.core.wallet.domain.WalletRepository;
import ng.fixpay.shared.exception.FixPayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final KeycloakAdminClient keycloak;
    private final UserRepository      userRepo;
    private final WalletRepository    walletRepo;

    public AuthService(KeycloakAdminClient keycloak,
                       UserRepository userRepo,
                       WalletRepository walletRepo) {
        this.keycloak   = keycloak;
        this.userRepo   = userRepo;
        this.walletRepo = walletRepo;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest req) {
        // Guard: duplicate phone
        if (userRepo.existsByPhone(req.phone())) {
            throw FixPayException.conflict("Phone number already registered");
        }

        UUID tenantId = UUID.fromString(req.tenantId());

        // 1. Create user in Keycloak
        UUID keycloakId = keycloak.createUser(
                req.phone(), req.email(), req.password(), req.tenantId());

        // 2. Persist shadow user in local DB
        AppUser user = new AppUser(keycloakId, tenantId, req.phone(), req.email());
        user = userRepo.save(user);

        // 3. Provision default NGN wallet
        Wallet wallet = new Wallet(user.getId(), tenantId);
        wallet = walletRepo.save(wallet);

        log.info("Registered user {} with wallet {}", user.getId(), wallet.getId());
        return new RegisterResponse(
                user.getId(), wallet.getId(), user.getPhone(),
                "Registration successful. Please verify your phone.");
    }
}
