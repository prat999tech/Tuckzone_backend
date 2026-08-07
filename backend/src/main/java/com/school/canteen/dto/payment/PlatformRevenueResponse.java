package com.school.canteen.dto.payment;

import java.math.BigDecimal;
import java.util.List;

/**
 * Platform fee revenue — never gross payment volume, which belongs to the restaurant or
 * the wallet, not this app. A refunded payment still counts (the fee itself is never
 * refunded — see Payment#getPlatformFee), so this number only ever changes when a NEW
 * fee is collected.
 */
public record PlatformRevenueResponse(
        BigDecimal todayRevenue,
        BigDecimal monthRevenue,
        BigDecimal totalRevenue,
        List<BreakdownRow> byProvider,
        List<BreakdownRow> byUseCase) {

    public record BreakdownRow(String key, BigDecimal platformFee, BigDecimal grossVolume) {
    }
}
