package com.school.canteen.service.impl;

import com.school.canteen.config.OtpProperties;
import com.school.canteen.config.RateLimitProperties;
import com.school.canteen.entity.OtpCode;
import com.school.canteen.enums.OtpPurpose;
import com.school.canteen.exception.BadRequestException;
import com.school.canteen.exception.TooManyRequestsException;
import com.school.canteen.notification.OtpSender;
import com.school.canteen.repository.OtpCodeRepository;
import com.school.canteen.security.RateLimiterService;
import com.school.canteen.service.OtpService;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OtpServiceImpl implements OtpService {

    /** SecureRandom, not Random: a predictable passcode is no passcode at all. */
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpCodeRepository otpCodeRepository;
    private final OtpSender otpSender;
    private final OtpProperties otpProperties;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiterService rateLimiter;
    private final RateLimitProperties rateLimitProperties;

    public OtpServiceImpl(OtpCodeRepository otpCodeRepository,
                          OtpSender otpSender,
                          OtpProperties otpProperties,
                          PasswordEncoder passwordEncoder,
                          RateLimiterService rateLimiter,
                          RateLimitProperties rateLimitProperties) {
        this.otpCodeRepository = otpCodeRepository;
        this.otpSender = otpSender;
        this.otpProperties = otpProperties;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
        this.rateLimitProperties = rateLimitProperties;
    }

    @Override
    @Transactional
    public String issue(String email, String recipientName, OtpPurpose purpose) {
        String key = normalise(email);
        // Throttle issuance per address: otherwise anyone could spam a stranger's inbox,
        // and a mail provider would start rate-limiting or blocking us for it.
        rateLimiter.checkAndConsume("otp-request:" + purpose + ":" + key,
                rateLimitProperties.otpRequestsPerWindow(),
                Duration.ofMinutes(rateLimitProperties.windowMinutes()),
                "Too many passcode requests. Please wait before trying again.");

        Instant now = Instant.now();
        enforceResendCooldown(key, purpose, now);
        // Retire outstanding codes so only the newest is valid; otherwise every code ever
        // sent would remain usable until its own expiry.
        otpCodeRepository.consumeOutstanding(key, purpose, now);

        String code = generateCode();
        OtpCode otp = new OtpCode();
        otp.setEmail(key);
        otp.setPurpose(purpose);
        otp.setCodeHash(passwordEncoder.encode(code));
        otp.setExpiresAt(now.plus(Duration.ofMinutes(otpProperties.ttlMinutes())));
        otpCodeRepository.save(otp);

        otpSender.send(email, recipientName, code, purpose);
        return otpSender.exposesCodeInResponse() ? code : null;
    }

    @Override
    @Transactional
    public void verify(String email, String code, OtpPurpose purpose) {
        String key = normalise(email);
        // Separate budget from issuance: this is the one that stops a 6-digit code being
        // guessed, which brute force would otherwise manage in seconds.
        rateLimiter.checkAndConsume("otp-verify:" + purpose + ":" + key,
                rateLimitProperties.otpVerifyAttemptsPerWindow(),
                Duration.ofMinutes(rateLimitProperties.windowMinutes()),
                "Too many incorrect passcode attempts. Please request a new code.");

        OtpCode otp = otpCodeRepository.lockLatest(key, purpose)
                .orElseThrow(() -> new BadRequestException("Request a passcode first"));

        if (!otp.isUsable(Instant.now())) {
            throw new BadRequestException("This passcode has expired. Request a new one.");
        }
        if (otp.getAttempts() >= otpProperties.maxAttempts()) {
            otp.setConsumedAt(Instant.now()); // burn it rather than allow endless guessing
            throw new BadRequestException("Too many incorrect attempts. Request a new code.");
        }
        if (!passwordEncoder.matches(code, otp.getCodeHash())) {
            otp.setAttempts(otp.getAttempts() + 1);
            throw new BadRequestException("Incorrect passcode");
        }

        // Single use: marking it consumed here is what prevents the same code being
        // replayed a second time.
        otp.setConsumedAt(Instant.now());
        rateLimiter.reset("otp-verify:" + purpose + ":" + key);
    }

    /**
     * Rejects a new code if one was issued too recently.
     *
     * The sliding-window rate limit above caps total volume (N per 15 minutes) but says
     * nothing about spacing — a user could otherwise burn that whole budget by tapping
     * "Resend" five times in a second. This is what makes the frontend's countdown timer a
     * real guarantee rather than a UI suggestion the API doesn't enforce.
     */
    private void enforceResendCooldown(String key, OtpPurpose purpose, Instant now) {
        Duration cooldown = Duration.ofSeconds(otpProperties.resendCooldownSeconds());
        otpCodeRepository.latestIssuedAt(key, purpose).ifPresent(lastIssuedAt -> {
            Duration elapsed = Duration.between(lastIssuedAt, now);
            if (elapsed.compareTo(cooldown) < 0) {
                long waitSeconds = cooldown.minus(elapsed).toSeconds() + 1;
                throw new TooManyRequestsException(
                        "Please wait " + waitSeconds + "s before requesting another code.");
            }
        });
    }

    /** Email is case-insensitive in practice, so store and look up one canonical form —
     *  otherwise "A@x.com" and "a@x.com" would get separate, mutually invisible codes. */
    private String normalise(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateCode() {
        int digits = otpProperties.length();
        StringBuilder code = new StringBuilder(digits);
        for (int i = 0; i < digits; i++) {
            code.append(RANDOM.nextInt(10));
        }
        return code.toString();
    }

    /** Expired codes carry no value; drop them so the table stays small. */
    @Scheduled(cron = "0 15 3 * * *")
    @Transactional
    public void purgeExpiredCodes() {
        otpCodeRepository.deleteExpiredBefore(Instant.now().minus(Duration.ofDays(2)));
    }
}
