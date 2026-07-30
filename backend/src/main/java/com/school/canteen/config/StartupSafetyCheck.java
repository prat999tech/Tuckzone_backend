package com.school.canteen.config;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Fails fast (or warns loudly) on dangerous configuration, instead of letting the app boot
 * into an insecure state that nobody notices until it is exploited.
 */
@Component
public class StartupSafetyCheck {

    private static final Logger log = LoggerFactory.getLogger(StartupSafetyCheck.class);

    /** The placeholder committed to the repository — must never be a live signing key. */
    private static final String DEV_DEFAULT_SECRET =
            "dev-only-secret-change-me-0123456789-abcdefghijklmnop";

    /** HMAC-SHA256 needs at least 256 bits of key material. */
    private static final int MIN_SECRET_BYTES = 32;

    private final JwtProperties jwtProperties;
    private final PaymentProperties paymentProperties;
    private final AdminProperties adminProperties;
    private final Environment environment;

    public StartupSafetyCheck(JwtProperties jwtProperties,
                              PaymentProperties paymentProperties,
                              AdminProperties adminProperties,
                              Environment environment) {
        this.jwtProperties = jwtProperties;
        this.paymentProperties = paymentProperties;
        this.adminProperties = adminProperties;
        this.environment = environment;
    }

    @PostConstruct
    public void verify() {
        boolean production = environment.matchesProfiles("prod");

        String secret = jwtProperties.secret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least " + MIN_SECRET_BYTES + " characters long");
        }
        if (production && DEV_DEFAULT_SECRET.equals(secret)) {
            // The default is public in the git history; running prod on it would let
            // anyone forge an admin token.
            throw new IllegalStateException(
                    "Refusing to start: JWT_SECRET is still the committed development default. "
                            + "Set a unique JWT_SECRET environment variable.");
        }

        if (production && DEV_DEFAULT_SECRET.equals(adminProperties.signupCode())) {
            log.warn("ADMIN_SIGNUP_CODE is using its default value — change it before launch.");
        }

        if (paymentProperties.allowMockTopup()) {
            log.warn("""

                    ==========================================================
                     MOCK TOP-UP IS ENABLED (app.payment.allow-mock-topup=true)
                     Wallets can be credited WITHOUT real money.
                     Intended only until a real payment gateway is integrated.
                     Disable with APP_ALLOW_MOCK_TOPUP=false before taking
                     real payments.
                    ==========================================================""");
        }
    }
}
