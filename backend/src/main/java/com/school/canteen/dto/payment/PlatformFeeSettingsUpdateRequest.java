package com.school.canteen.dto.payment;

import com.school.canteen.enums.PlatformFeeType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PlatformFeeSettingsUpdateRequest(
        @NotNull Boolean enabled,
        @NotNull PlatformFeeType feeType,
        @NotNull @DecimalMin("0") BigDecimal feeValue,
        BigDecimal minFee,
        BigDecimal maxFee) {
}
