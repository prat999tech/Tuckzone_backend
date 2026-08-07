package com.school.canteen.dto.order;

import com.school.canteen.enums.OrderStatus;
import com.school.canteen.enums.OrderType;
import com.school.canteen.enums.PaymentMethod;
import com.school.canteen.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * recipientClass/recipientSection are resolved the same way {@code AdminOrderResponse}
 * resolves them (via the linked or self student profile) — null for a teacher's own order,
 * which has no class. Kept separate from deliveryLocation so a client never needs to parse
 * class out of free-text location, and separate from recipientName so renaming a field
 * never means splitting a combined string.
 */
public record OrderResponse(
        UUID id,
        String orderNumber,
        OrderStatus status,
        OrderType orderType,
        String pickupCode,
        LocalDate menuDate,
        String slotName,
        LocalTime deliveryTime,
        String recipientName,
        String recipientClass,
        String recipientSection,
        String deliveryLocation,
        String deliveryPersonName,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        BigDecimal totalAmount,
        List<OrderItemResponse> items,
        Instant createdAt) {
}
