package com.school.canteen.dto.report;

import java.math.BigDecimal;

public record TopItemRow(
        String itemName,
        long quantitySold,
        BigDecimal revenue) {
}
