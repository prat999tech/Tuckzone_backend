package com.school.canteen.dto.order;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID menuItemId,
        String itemName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal) {
}
