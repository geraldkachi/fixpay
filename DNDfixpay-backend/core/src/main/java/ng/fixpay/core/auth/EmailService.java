package ng.fixpay.core.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String         fromAddress;

    public EmailService(JavaMailSender mailSender,
                        @Value("${fixpay.mail.from:noreply@fixpay.local}") String fromAddress) {
        this.mailSender  = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendOtp(String toEmail, String otp) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(toEmail);
        msg.setSubject("Your FixPay verification code");
        msg.setText("""
                Your FixPay verification code is:

                  %s

                This code expires in 10 minutes. Do not share it with anyone.
                """.formatted(otp));
        mailSender.send(msg);
        log.info("OTP email sent to {}", toEmail);
    }
}
