package com.school.canteen.dto.auth;

/**
 * Confirmation that a passcode was sent.
 *
 * @param devCode populated only by the development sender so the flow can be exercised
 *                without SMS. A real sender leaves this null and the code travels only to
 *                the phone.
 */
public record OtpIssuedResponse(
        String message,
        int expiresInMinutes,
        String devCode) {
}
