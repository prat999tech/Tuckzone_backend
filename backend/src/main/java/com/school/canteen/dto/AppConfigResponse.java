package com.school.canteen.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Server capabilities the mobile app reads once at startup.
 *
 * Without this the app would have to hardcode things that legitimately change on the
 * server — OTP length, top-up limits, whether real payments are live — and every change
 * would need a Play Store release to stay in sync.
 *
 * @param mockPaymentsEnabled true while wallet top-ups are simulated; the app should show
 *                            a clear "test mode" affordance instead of a real checkout
 * @param otpDevCodeReturned  true when the OTP response carries the code (development
 *                            only), so the app can prefill it rather than asking for SMS
 *                            that will never arrive
 * @param minimumAppVersion   clients older than this should prompt the user to update
 */
public record AppConfigResponse(
        String currency,
        String timezone,
        int otpLength,
        int otpTtlMinutes,
        boolean otpDevCodeReturned,
        boolean mockPaymentsEnabled,
        BigDecimal maxTopupAmount,
        int passwordMinLength,
        List<Integer> quickTopupAmounts,
        String minimumAppVersion) {
}
