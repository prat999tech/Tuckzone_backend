package com.school.canteen.dto.payment;

import com.school.canteen.enums.PaymentUseCase;
import com.school.canteen.enums.PlatformFeeType;
import java.math.BigDecimal;

public record PlatformFeeSettingsResponse(
        PaymentUseCase useCase,
        boolean enabled,
        PlatformFeeType feeType,
        BigDecimal feeValue,
        BigDecimal minFee,
        BigDecimal maxFee) {
}
