package com.school.canteen.dto.report;

import com.school.canteen.enums.MenuType;
import java.math.BigDecimal;

public record TopItemRow(
        String itemName,
        MenuType menuType,
        long quantitySold,
        BigDecimal revenue) {
}
