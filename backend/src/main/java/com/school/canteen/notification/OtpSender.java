package com.school.canteen.notification;

import com.school.canteen.enums.OtpPurpose;

/**
 * Delivers a one-time passcode to a user's email address (Strategy pattern).
 *
 * The OTP lifecycle — generation, hashing, expiry, attempt limits, verification — lives
 * entirely in our service; only the delivery channel varies. Swapping the development
 * sender for real email is therefore a config flip and no change to auth logic.
 */
public interface OtpSender {

    void send(String email, String recipientName, String code, OtpPurpose purpose);

    /**
     * True when the passcode may be echoed back in the API response.
     *
     * Development convenience only: it makes the flow demonstrable without a mail server.
     * A real sender must return false, otherwise anyone could request a code for someone
     * else's address and read it straight out of the response.
     */
    boolean exposesCodeInResponse();
}
