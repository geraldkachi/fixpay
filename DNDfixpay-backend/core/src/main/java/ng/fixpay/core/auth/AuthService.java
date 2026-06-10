package ng.fixpay.core.auth;

import ng.fixpay.core.auth.dto.LoginRequest;
import ng.fixpay.core.auth.dto.LoginResponse;
import ng.fixpay.core.auth.dto.RegisterRequest;
import ng.fixpay.core.auth.dto.RegisterResponse;
import ng.fixpay.core.auth.dto.VerifyOtpRequest;
import ng.fixpay.core.auth.dto.VerifyOtpResponse;
import ng.fixpay.core.user.domain.AppUser;
import ng.fixpay.core.user.domain.UserRepository;
import ng.fixpay.core.wallet.domain.Wallet;
import ng.fixpay.core.wallet.domain.WalletRepository;
import ng.fixpay.shared.exception.FixPayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Arrays;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final KeycloakAdminClient keycloak;
    private final UserRepository      userRepo;
    private final WalletRepository    walletRepo;
    private final OtpService          otpService;
    private final EmailService        emailService;
    private final Environment         env;

    public AuthService(KeycloakAdminClient keycloak,
                       UserRepository userRepo,
                       WalletRepository walletRepo,
                       OtpService otpService,
                       EmailService emailService,
                       Environment env) {
        this.keycloak     = keycloak;
        this.userRepo     = userRepo;
        this.walletRepo   = walletRepo;
        this.otpService   = otpService;
        this.emailService = emailService;
        this.env          = env;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest req) {
        // Guard: duplicate phone / email
        if (userRepo.existsByPhone(req.phone())) {
            throw FixPayException.conflict("Phone number already registered");
        }
        if (userRepo.existsByEmail(req.email())) {
            throw FixPayException.conflict("Email address already registered");
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

        // 4. Generate and email OTP
        String otp = otpService.generate(req.email());
        emailService.sendOtp(req.email(), otp);

        log.info("Registered user {} with wallet {}, OTP sent to {}", user.getId(), wallet.getId(), req.email());
        return new RegisterResponse(
                user.getId(), wallet.getId(), user.getPhone(),
                "Registration successful. Check your email for a verification code.");
    }

    public VerifyOtpResponse verifyOtp(VerifyOtpRequest req) {
        if (!otpService.verify(req.email(), req.otp())) {
            throw FixPayException.badRequest("Invalid or expired OTP");
        }
        AppUser user = userRepo.findByEmail(req.email())
                .orElseThrow(() -> FixPayException.notFound("user"));

        // Auto-fund wallet in dev mode
        fundWalletIfDev(user.getId());

        log.info("OTP verified for user {}", user.getId());
        return new VerifyOtpResponse(user.getId(), req.email(),
                "Email verified. You can now sign in.");
    }

    /**
     * Auto-fund wallet with ₦50,000 in dev mode.
     * This is a development convenience feature to allow testing without real payments.
     */
    @Transactional
    public void fundWalletIfDev(UUID userId) {
        // Check if we're in dev mode (via environment variable or active profiles)
        String appEnv = env.getProperty("app.environment");
        String[] activeProfiles = env.getActiveProfiles();
        boolean isDev = "dev".equalsIgnoreCase(appEnv) || 
                       Arrays.asList(activeProfiles).contains("dev");

        if (!isDev) {
            return; // Only in dev mode
        }

        Wallet wallet = walletRepo.findByUserIdAndCurrency(userId, "NGN")
                .orElseThrow(() -> FixPayException.notFound("wallet"));

        java.math.BigDecimal devFundAmount = new java.math.BigDecimal("50000");
        wallet.setBalance(wallet.getBalance().add(devFundAmount));
        walletRepo.save(wallet);

        log.info("Dev mode: Auto-funded wallet {} with ₦50,000", wallet.getId());
    }

    public LoginResponse login(LoginRequest req) {
        // Resolve Keycloak username (phone in E164)
        String username;
        if (req.phone() != null && !req.phone().isBlank()) {
            // Normalise local format 0XX → +234XX if needed
            username = req.phone().startsWith("0")
                    ? "+234" + req.phone().substring(1)
                    : req.phone();
        } else if (req.email() != null && !req.email().isBlank()) {
            AppUser byEmail = userRepo.findByEmail(req.email())
                    .orElseThrow(() -> FixPayException.unauthorized("Invalid credentials"));
            username = byEmail.getPhone();
        } else {
            throw FixPayException.badRequest("Phone or email is required");
        }

        KeycloakAdminClient.TokenPair tokens = keycloak.getUserTokenFull(username, req.password());

        AppUser user = userRepo.findByPhone(username)
                .orElseThrow(() -> FixPayException.unauthorized("Invalid credentials"));

        log.info("User {} logged in", user.getId());
        return new LoginResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                new LoginResponse.UserDto(
                        user.getId(), user.getPhone(), user.getEmail(),
                        "", "", user.getTier(), user.getKycStatus(), user.getCreatedAt()
                )
        );
    }

    public KeycloakAdminClient.TokenPair refreshToken(String rawRefreshToken) {
        return keycloak.refreshToken(rawRefreshToken);
    }
}
