package com.school.canteen.dto.auth;

/**
 * Confirmation that a passcode was sent.
 *
 * @param devCode populated only by the development sender so the flow can be exercised
 *                without SMS. A real sender leaves this null and the code travels only to
 *                the phone.
 * @param resendAfterSeconds how long the client must wait before calling this endpoint
 *                           again for the same address; requesting sooner gets a 429. Lets
 *                           the frontend's "Resend in Ns" countdown reflect the server's
 *                           actual policy instead of a hardcoded, potentially-stale guess.
 */
public record OtpIssuedResponse(
        String message,
        int expiresInMinutes,
        String devCode,
        int resendAfterSeconds) {
}
