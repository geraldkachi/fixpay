package ng.fixpay.core.auth;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
public class OtpService {

    private static final int    OTP_LENGTH  = 6;
    private static final String KEY_PREFIX  = "otp:";
    private static final Duration TTL       = Duration.ofMinutes(10);

    private final StringRedisTemplate redis;
    private final SecureRandom        random = new SecureRandom();

    public OtpService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Generate a 6-digit OTP, store it in Valkey with a 10-min TTL, and return it. */
    public String generate(String email) {
        String otp = String.format("%0" + OTP_LENGTH + "d",
                random.nextInt((int) Math.pow(10, OTP_LENGTH)));
        redis.opsForValue().set(KEY_PREFIX + email.toLowerCase(), otp, TTL);
        return otp;
    }

    /**
     * Validate the OTP for the given email.
     * Deletes the key on success (single-use).
     * @return true if valid, false if expired or wrong
     */
    public boolean verify(String email, String otp) {
        String key    = KEY_PREFIX + email.toLowerCase();
        String stored = redis.opsForValue().get(key);
        if (stored == null || !stored.equals(otp)) {
            return false;
        }
        redis.delete(key);
        return true;
    }
}
