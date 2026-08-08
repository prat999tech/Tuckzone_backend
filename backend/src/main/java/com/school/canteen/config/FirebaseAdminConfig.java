package com.school.canteen.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * Lazily creates the single {@link FirebaseApp} instance shared by every Firebase-backed
 * feature — {@code FirebasePushSender} (FCM) and {@code FirebaseAuthService} (Firebase
 * Authentication) both go through {@link #get()} rather than each initialising their own,
 * since the SDK throws if the same named app is registered twice.
 *
 * Deliberately NOT a {@code @Bean} returning {@code FirebaseApp} directly: Spring would
 * then construct it eagerly at context startup (both consumers are always-present
 * singletons), so a deployment that has never configured Firebase — which describes every
 * environment before this feature is set up, including the current production deployment —
 * would fail to boot at all instead of simply not offering Firebase-backed features yet.
 * Failing only when Firebase is actually used mirrors how {@code RazorpayProvider} fails
 * only once {@code app.payment.provider=razorpay} is actually selected, not at every boot.
 */
@Component
public class FirebaseAdminConfig {

    private static final String APP_NAME = "canteen-firebase";

    private final NotificationProperties properties;
    private volatile FirebaseApp firebaseApp;

    public FirebaseAdminConfig(NotificationProperties properties) {
        this.properties = properties;
    }

    /**
     * Returns the shared app, creating it on first call. Throws
     * {@link IllegalStateException} if Firebase credentials aren't configured — callers
     * (FirebaseAuthService, FirebasePushSender) let that surface as a clean 500 rather than
     * catching it, exactly like a misconfigured Razorpay key does today.
     */
    public FirebaseApp get() {
        FirebaseApp app = firebaseApp;
        if (app != null) {
            return app;
        }
        synchronized (this) {
            if (firebaseApp == null) {
                firebaseApp = FirebaseApp.getApps().stream()
                        .filter(existing -> APP_NAME.equals(existing.getName()))
                        .findFirst()
                        .orElseGet(this::createApp);
            }
            return firebaseApp;
        }
    }

    private FirebaseApp createApp() {
        try {
            GoogleCredentials credentials = resolveCredentials();
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();
            return FirebaseApp.initializeApp(options, APP_NAME);
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Firebase credentials could not be loaded. Set FIREBASE_CREDENTIALS_JSON "
                            + "or GOOGLE_APPLICATION_CREDENTIALS.", ex);
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
}
