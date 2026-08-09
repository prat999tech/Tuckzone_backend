package com.school.canteen.auth.firebase;

/**
 * What a Firebase ID token actually proves, once verified — nothing more. Deliberately
 * carries no application concept (no role, no User reference): resolving this identity to
 * an application account is {@link FirebaseAccountService}'s job, not this package's.
 *
 * @param uid          Firebase's permanent identifier for this identity. The only field
 *                     safe to treat as a stable foreign key.
 * @param email        null unless the sign-in method was email-based.
 * @param emailVerified per Firebase's own verification (e.g. clicked the confirmation
 *                      link). False for every non-email sign-in method.
 * @param phoneNumber  E.164 format (e.g. {@code +919876543210}), null unless the sign-in
 *                     method was phone-based.
 */
public record FirebaseVerifiedIdentity(String uid, String email, boolean emailVerified, String phoneNumber) {
}
