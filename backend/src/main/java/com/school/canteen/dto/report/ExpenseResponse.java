package com.school.canteen.dto.report;

import com.school.canteen.enums.ExpenseCategory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        LocalDate expenseDate,
        ExpenseCategory category,
        String description,
        BigDecimal amount) {
}
