package com.school.canteen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Notification delivery settings.
 *
 * Email connection details are NOT here — they use Spring Boot's standard
 * {@code spring.mail.*} properties, which keeps them portable across any SMTP provider.
 *
 * @param pushProvider   which {@link com.school.canteen.notification.PushSender} is active
 *                       ("log" for development, "firebase" for real FCM)
 * @param batchSize      how many outbox rows the sweeper claims per pass; also the size of
 *                       a single batched FCM call
 * @param maxAttempts    delivery attempts before a notification is abandoned
 * @param retryBackoffSeconds base delay between retries, multiplied by the attempt count
 * @param firebaseCredentialsJson service-account JSON (or base64 of it) for FCM
 * @param emailProvider  which {@link com.school.canteen.notification.EmailSender} is active
 *                       ("log" for development, "smtp" for SMTP delivery, "ses" for
 *                       Amazon SES delivery)
 * @param emailFromAddress the address recipients see as the sender. When emailProvider is
 *                          "ses" this must be a verified SES identity (a verified address,
 *                          or any address on a verified domain) or SES will reject the send.
 * @param emailFromName    the display name recipients see as the sender
 * @param sesRegion        AWS region SES calls are sent to, e.g. "ap-south-1". Optional —
 *                         blank falls back to the AWS SDK's default region provider chain
 *                         (the AWS_REGION env var, ~/.aws/config, etc).
 */
@ConfigurationProperties(prefix = "app.notification")
public record NotificationProperties(
        String pushProvider,
        int batchSize,
        int maxAttempts,
        int retryBackoffSeconds,
        String firebaseCredentialsJson,
        String emailProvider,
        String emailFromAddress,
        String emailFromName,
        String sesRegion) {
}
