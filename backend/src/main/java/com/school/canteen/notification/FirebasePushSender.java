package com.school.canteen.notification;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.school.canteen.config.NotificationProperties;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Real push delivery through Firebase Cloud Messaging.
 *
 * Uses {@code sendEach}, which submits up to 500 *distinct* messages in one call. That is
 * what keeps a recess rush cheap: five hundred orders cost one network round trip rather
 * than five hundred.
 *
 * Active only when {@code app.notification.push-provider=firebase}, so development and
 * tests run without a Firebase project or credentials.
 */
@Component
@ConditionalOnProperty(name = "app.notification.push-provider", havingValue = "firebase")
public class FirebasePushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(FirebasePushSender.class);

    /** FCM's documented ceiling for a single sendEach call. */
    private static final int FCM_MAX_BATCH = 500;
    private static final String APP_NAME = "canteen-fcm";

    /**
     * Must match the channel the Android app creates at startup. If they differ, Android 8+
     * silently discards the notification.
     */
    private static final String ANDROID_CHANNEL_ID = "canteen_orders";

    private final NotificationProperties properties;
    private FirebaseApp firebaseApp;

    public FirebasePushSender(NotificationProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void initialise() throws IOException {
        // Reuse an existing app on context restart (dev reload) instead of failing on a
        // duplicate-name registration.
        firebaseApp = FirebaseApp.getApps().stream()
                .filter(app -> APP_NAME.equals(app.getName()))
                .findFirst()
                .orElseGet(this::createApp);
    }

    private FirebaseApp createApp() {
        try {
            GoogleCredentials credentials = resolveCredentials();
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();
            return FirebaseApp.initializeApp(options, APP_NAME);
        } catch (IOException ex) {
            // Fail fast: booting with push "enabled" but unusable would hide the problem
            // until customers silently stopped receiving order updates.
            throw new IllegalStateException(
                    "Firebase push is enabled but credentials could not be loaded. "
                            + "Set FIREBASE_CREDENTIALS_JSON or GOOGLE_APPLICATION_CREDENTIALS.", ex);
        }
    }

    /**
     * Accepts the service-account JSON directly or base64-encoded, because most hosting
     * platforms mangle multi-line environment variables. Falls back to the standard
     * GOOGLE_APPLICATION_CREDENTIALS file lookup.
     */
    private GoogleCredentials resolveCredentials() throws IOException {
        String configured = properties.firebaseCredentialsJson();
        if (configured == null || configured.isBlank()) {
            return GoogleCredentials.getApplicationDefault();
        }
        String json = configured.trim();
        if (!json.startsWith("{")) {
            json = new String(Base64.getDecoder().decode(json), StandardCharsets.UTF_8);
        }
        return GoogleCredentials.fromStream(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public List<PushOutcome> sendAll(List<PushMessage> messages) {
        List<Message> payloads = messages.stream()
                .map(message -> Message.builder()
                        .setToken(message.token())
                        .setNotification(Notification.builder()
                                .setTitle(message.title())
                                .setBody(message.body())
                                .build())
                        .putAllData(message.data())
                        // Android 8+ drops notifications that do not name a channel the app
                        // has registered, so the push arrives but the user never sees it.
                        // HIGH priority also lets order updates through Doze/battery saver,
                        // which matters when food is minutes from being delivered.
                        .setAndroidConfig(AndroidConfig.builder()
                                .setPriority(AndroidConfig.Priority.HIGH)
                                .setNotification(AndroidNotification.builder()
                                        .setChannelId(ANDROID_CHANNEL_ID)
                                        // No click_action set on purpose: React Native
                                        // Firebase routes taps through
                                        // onNotificationOpenedApp/getInitialNotification and
                                        // reads the data map, so a custom intent action
                                        // would only stop the handler from firing.
                                        .build())
                                .build())
                        .build())
                .toList();

        try {
            BatchResponse response = FirebaseMessaging.getInstance(firebaseApp).sendEach(payloads);
            List<PushOutcome> outcomes = new ArrayList<>(messages.size());
            for (SendResponse sendResponse : response.getResponses()) {
                outcomes.add(toOutcome(sendResponse));
            }
            return outcomes;
        } catch (FirebaseMessagingException ex) {
            // Whole-batch failure (network, auth). Report every message as retryable so
            // the outbox holds them rather than dropping them.
            log.warn("FCM batch send failed for {} message(s)", messages.size(), ex);
            return messages.stream().map(m -> PushOutcome.failed(ex.getMessage())).toList();
        }
    }

    private PushOutcome toOutcome(SendResponse sendResponse) {
        if (sendResponse.isSuccessful()) {
            return PushOutcome.ok();
        }
        FirebaseMessagingException error = sendResponse.getException();
        MessagingErrorCode code = (error == null) ? null : error.getMessagingErrorCode();
        String message = (error == null) ? "unknown error" : error.getMessage();

        // UNREGISTERED = the app was uninstalled or the token was replaced.
        // INVALID_ARGUMENT on a send almost always means a malformed/foreign token.
        // Both are permanent for this token, so it gets deleted rather than retried.
        boolean tokenDead = code == MessagingErrorCode.UNREGISTERED
                || code == MessagingErrorCode.INVALID_ARGUMENT;
        return tokenDead ? PushOutcome.invalidToken(message) : PushOutcome.failed(message);
    }

    @Override
    public int maxBatchSize() {
        return FCM_MAX_BATCH;
    }
}
