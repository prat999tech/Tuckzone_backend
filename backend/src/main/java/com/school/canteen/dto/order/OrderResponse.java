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
        String deliveryLocation,
        String deliveryPersonName,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        BigDecimal totalAmount,
        List<OrderItemResponse> items,
        Instant createdAt) {
}
