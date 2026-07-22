package com.school.canteen.dto.wallet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TopupRequest(
        @NotNull @DecimalMin(value = "1.00") @Digits(integer = 8, fraction = 2) BigDecimal amount) {
}
