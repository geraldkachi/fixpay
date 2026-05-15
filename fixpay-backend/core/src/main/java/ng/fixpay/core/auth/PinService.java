package ng.fixpay.core.auth;

import ng.fixpay.core.user.domain.AppUser;
import ng.fixpay.core.user.domain.UserRepository;
import ng.fixpay.shared.exception.FixPayException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PinService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PinService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void createPin(String keycloakSubject, String rawPin) {
        AppUser user = userRepository.findByKeycloakId(UUID.fromString(keycloakSubject))
                .orElseThrow(() -> FixPayException.notFound("User not found"));
        user.setPinHash(passwordEncoder.encode(rawPin));
        userRepository.save(user);
    }

    public void verifyPin(String keycloakSubject, String rawPin) {
        AppUser user = userRepository.findByKeycloakId(UUID.fromString(keycloakSubject))
                .orElseThrow(() -> FixPayException.notFound("User not found"));
        if (user.getPinHash() == null) {
            throw FixPayException.badRequest("PIN not set");
        }
        if (!passwordEncoder.matches(rawPin, user.getPinHash())) {
            throw FixPayException.unauthorized("Incorrect PIN");
        }
    }
}
