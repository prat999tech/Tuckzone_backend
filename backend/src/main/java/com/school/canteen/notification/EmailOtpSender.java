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
 * both work, and neither is tied to a particular vendor (see
 * {@code app.notification.email-provider}).
 *
 * Active when {@code app.otp.delivery=email}.
 */
@Component
@ConditionalOnProperty(name = "app.otp.delivery", havingValue = "email")
public class EmailOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(EmailOtpSender.class);

    // TuckZone "Vibrant Professional" palette (DESIGN.md) — kept in sync with
    // mobile/src/theme/colors.ts so the email reads as the same product as the app.
    private static final String BRAND = "#006948";
    private static final String BRAND_DARK = "#005137";
    private static final String BG = "#f8f9ff";
    private static final String CARD_BG = "#ffffff";
    private static final String TEXT_PRIMARY = "#121c28";
    private static final String TEXT_SECONDARY = "#3d4a42";
    private static final String TEXT_MUTED = "#6d7a72";
    private static final String BORDER = "#d9e3f4";
    private static final String WARN_BG = "#fef3c7";
    private static final String WARN_TEXT = "#92400e";

    private final EmailSender emailSender;
    private final OtpProperties otpProperties;

    public EmailOtpSender(EmailSender emailSender, OtpProperties otpProperties) {
        this.emailSender = emailSender;
        this.otpProperties = otpProperties;
    }

    @Override
    public void send(String email, String recipientName, String code, OtpPurpose purpose) {
        boolean delivered = emailSender.send(new EmailMessage(
                email, subjectFor(purpose), htmlBody(recipientName, code, purpose),
                textBody(recipientName, code, purpose)));
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

    private String introFor(OtpPurpose purpose) {
        return switch (purpose) {
            case LOGIN -> "Use this code to sign in to TuckZone.";
            case PASSWORD_RESET -> "Use this code to set a new password.";
            case EMAIL_VERIFICATION -> "Welcome to TuckZone! Use this code to confirm your email address.";
        };
    }

    /**
     * Table-based layout with only inline styles, which is what survives the aggressive
     * CSS stripping done by Outlook, Gmail and most corporate mail gateways. The one
     * {@code <style>} block is progressive enhancement for clients that honour
     * {@code prefers-color-scheme} (Apple Mail, modern Outlook, some Gmail apps) — clients
     * that ignore it just render the light inline styles below, which is a safe fallback.
     */
    private String htmlBody(String recipientName, String code, OtpPurpose purpose) {
        String greeting = (recipientName == null || recipientName.isBlank())
                ? "Hello," : "Hello " + escape(recipientName) + ",";
        String intro = introFor(purpose);
        String safeCode = escape(code);
        int ttl = otpProperties.ttlMinutes();

        return """
                <!doctype html>
                <html lang="en">
                <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <meta name="color-scheme" content="light dark">
                <meta name="supported-color-schemes" content="light dark">
                <title>%s</title>
                <style>
                  @media (prefers-color-scheme: dark) {
                    .email-bg   { background-color: #0f1a14 !important; }
                    .email-card { background-color: #16241c !important; border-color: #24352b !important; }
                    .email-text-primary   { color: #f2f5f2 !important; }
                    .email-text-secondary { color: #c7d1cb !important; }
                    .email-text-muted     { color: #8fa199 !important; }
                    .email-code { color: #85f8c4 !important; }
                    .email-border { border-color: #24352b !important; }
                  }
                  @media (max-width: 480px) {
                    .email-container { width: 100%% !important; }
                    .email-padding   { padding: 24px !important; }
                    .email-code      { font-size: 32px !important; letter-spacing: 6px !important; }
                  }
                </style>
                </head>
                <body class="email-bg" style="margin:0;padding:0;background-color:%s;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
                  <span style="display:none;font-size:1px;color:%s;line-height:1px;max-height:0;max-width:0;opacity:0;overflow:hidden;">
                    Your TuckZone verification code is %s. It expires in %d minutes.
                  </span>
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" class="email-bg" style="background-color:%s;">
                    <tr>
                      <td align="center" style="padding:32px 16px;">
                        <table role="presentation" width="480" cellpadding="0" cellspacing="0" class="email-container" style="width:480px;max-width:100%%;">

                          <!-- Wordmark -->
                          <tr>
                            <td align="center" style="padding-bottom:24px;">
                              <table role="presentation" cellpadding="0" cellspacing="0">
                                <tr>
                                  <td style="background-color:%s;border-radius:12px;width:40px;height:40px;text-align:center;vertical-align:middle;font-size:20px;line-height:40px;">🍱</td>
                                  <td style="padding-left:10px;font-size:20px;font-weight:800;color:%s;letter-spacing:-0.02em;" class="email-text-primary">TuckZone</td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- Card -->
                          <tr>
                            <td class="email-card" style="background-color:%s;border:1px solid %s;border-radius:16px;padding:36px;" class="email-padding">
                              <p class="email-text-primary" style="margin:0 0 4px;font-size:16px;color:%s;">%s</p>
                              <p class="email-text-secondary" style="margin:0 0 28px;font-size:14px;color:%s;line-height:1.5;">%s</p>

                              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                                <tr>
                                  <td align="center" style="background-color:%s;border-radius:12px;padding:20px 0;">
                                    <span class="email-code" style="font-size:40px;font-weight:800;letter-spacing:10px;color:%s;font-family:'SF Mono',Consolas,Menlo,monospace;">%s</span>
                                  </td>
                                </tr>
                              </table>

                              <p class="email-text-muted" style="margin:20px 0 0;font-size:13px;color:%s;text-align:center;">
                                This code expires in <strong>%d minutes</strong>.
                              </p>

                              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin-top:24px;">
                                <tr>
                                  <td style="background-color:%s;border-radius:10px;padding:14px 16px;">
                                    <p style="margin:0;font-size:12.5px;color:%s;line-height:1.5;">
                                      <strong>Never share this code.</strong> TuckZone staff will never ask you for
                                      it by phone, chat or email. If you did not request this code, you can
                                      safely ignore this message — no action is needed.
                                    </p>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- Footer -->
                          <tr>
                            <td align="center" style="padding-top:24px;">
                              <p class="email-text-muted" style="margin:0;font-size:12px;color:%s;">
                                TuckZone School Canteen &middot; This is an automated message, please don't reply.
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                        escape(subjectFor(purpose)),           // <title>
                        BG,                                     // body bg
                        BG,                                     // preheader color (blends with bg = hidden)
                        safeCode, ttl,                           // preheader text
                        BG,                                     // outer table bg
                        BRAND, TEXT_PRIMARY,                     // logo tile bg, wordmark text color
                        CARD_BG, BORDER,                         // card bg/border
                        TEXT_PRIMARY, greeting,                  // greeting
                        TEXT_SECONDARY, intro,                   // intro line
                        BG, BRAND_DARK, safeCode,                 // code block bg, code color, code
                        TEXT_MUTED, ttl,                          // expiry line
                        WARN_BG, WARN_TEXT,                       // security notice
                        TEXT_MUTED                                // footer
                );
    }

    private String textBody(String recipientName, String code, OtpPurpose purpose) {
        String greeting = (recipientName == null || recipientName.isBlank())
                ? "Hello," : "Hello " + recipientName + ",";
        return """
                %s

                %s

                Your code: %s

                This code expires in %d minutes.

                Never share this code with anyone — TuckZone staff will never ask for it by \
                phone, chat or email. If you did not request this code, you can safely ignore \
                this message.

                — TuckZone School Canteen (automated message, please don't reply)
                """.formatted(greeting, introFor(purpose), code, otpProperties.ttlMinutes());
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
