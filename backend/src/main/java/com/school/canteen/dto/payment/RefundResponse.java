package com.school.canteen.dto.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundResponse(
        UUID refundId,
        BigDecimal walletAmount,
        BigDecimal gatewayAmount,
        BigDecimal totalAmount,
        String status) {
}
