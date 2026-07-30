package com.school.canteen.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailySalesRow(
        LocalDate date,
        long orders,
        BigDecimal revenue) {
}
