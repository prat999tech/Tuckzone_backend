package com.school.canteen.service;

import com.school.canteen.enums.OtpPurpose;

/** Issues and verifies one-time passcodes, delivered by email. */
public interface OtpService {

    /**
     * Generates, stores and emails a passcode.
     *
     * @return the raw code when the active sender permits echoing it (development only),
     *         otherwise null
     */
    String issue(String email, String recipientName, OtpPurpose purpose);

    /**
     * Consumes the passcode if it matches, is unexpired and is within its attempt budget.
     * Throws when it does not; a code is single-use whether or not it succeeded.
     */
    void verify(String email, String code, OtpPurpose purpose);
}
