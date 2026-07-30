package com.school.canteen.dto.report;

import com.school.canteen.enums.ExpenseCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(
        @NotNull LocalDate expenseDate,
        @NotNull ExpenseCategory category,
        @Size(max = 255) String description,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount) {
}
