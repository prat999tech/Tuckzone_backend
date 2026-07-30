package com.school.canteen.notification;

import com.school.canteen.enums.OtpPurpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Development OTP sender: writes the passcode to the application log and allows the API to
 * return it, so sign-in, registration and password reset can be demonstrated without a
 * mail server configured.
 *
 * Active while {@code app.otp.delivery=log} (the default).
 */
@Component
@ConditionalOnProperty(name = "app.otp.delivery", havingValue = "log", matchIfMissing = true)
public class LoggingOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingOtpSender.class);

    @Override
    public void send(String email, String recipientName, String code, OtpPurpose purpose) {
        log.warn("[DEV OTP] purpose={} email={} code={}", purpose, email, code);
    }

    @Override
    public boolean exposesCodeInResponse() {
        return true;
    }
}
