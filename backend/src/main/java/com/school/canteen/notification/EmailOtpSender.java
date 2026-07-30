package com.school.canteen.notification;

import com.school.canteen.config.OtpProperties;
import com.school.canteen.enums.OtpPurpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Real OTP delivery, by email.
 *
 * Deliberately delegates to {@link EmailSender} rather than talking to a mail server
 * itself: OTP and notifications then share one transport, so configuring email once makes
 * both work, and neither is tied to a particular vendor.
 *
 * Active when {@code app.otp.delivery=email}.
 */
@Component
@ConditionalOnProperty(name = "app.otp.delivery", havingValue = "email")
public class EmailOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(EmailOtpSender.class);

    private final EmailSender emailSender;
    private final OtpProperties otpProperties;

    public EmailOtpSender(EmailSender emailSender, OtpProperties otpProperties) {
        this.emailSender = emailSender;
        this.otpProperties = otpProperties;
    }

    @Override
    public void send(String email, String recipientName, String code, OtpPurpose purpose) {
        boolean delivered = emailSender.send(new EmailMessage(
                email, subjectFor(purpose), body(recipientName, code, purpose)));
        if (!delivered) {
            // Not rethrown on purpose: the OTP request endpoint must answer identically
            // whether or not the address exists or delivery succeeded, otherwise its
            // response becomes a way to discover which addresses have accounts. The user's
            // recourse is the same either way — tap "Resend".
            log.error("Could not email OTP for purpose={}", purpose);
        }
    }

    private String subjectFor(OtpPurpose purpose) {
        return switch (purpose) {
            case LOGIN -> "Your TuckZone sign-in code";
            case PASSWORD_RESET -> "Reset your TuckZone password";
            case EMAIL_VERIFICATION -> "Verify your TuckZone email";
        };
    }

    private String body(String recipientName, String code, OtpPurpose purpose) {
        String intro = switch (purpose) {
            case LOGIN -> "Use this code to sign in to TuckZone.";
            case PASSWORD_RESET -> "Use this code to set a new password.";
            case EMAIL_VERIFICATION -> "Welcome to TuckZone! Use this code to confirm your email address.";
        };
        String greeting = (recipientName == null || recipientName.isBlank())
                ? "Hello," : "Hello " + escape(recipientName) + ",";

        return """
                <div style="font-family:sans-serif;font-size:15px;color:#1f2937">
                  <p>%s</p>
                  <p>%s</p>
                  <p style="font-size:30px;font-weight:800;letter-spacing:6px;color:#B45309;margin:20px 0">%s</p>
                  <p style="color:#6b7280;font-size:13px">
                    This code expires in %d minutes. If you did not request it, you can ignore this email.
                    Never share this code with anyone.
                  </p>
                </div>
                """.formatted(greeting, intro, escape(code), otpProperties.ttlMinutes());
    }

    /** The code is digits and the name is user-supplied, so escape before embedding. */
    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    @Override
    public boolean exposesCodeInResponse() {
        // Never echo a real code — that would defeat the point of sending it out-of-band.
        return false;
    }
}
