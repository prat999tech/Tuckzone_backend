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
 * Admin-facing order view (Canteen Admin and Sub Admin). A superset of {@link OrderResponse}
 * that additionally surfaces the ordering student's name/class/section/roll number — the
 * only student-identity fields a Sub Admin may see. Deliberately carries no phone, email,
 * wallet balance or address field: those simply do not exist on this record, so a Sub Admin
 * can never receive them through this endpoint regardless of what else changes here.
 */
public record AdminOrderResponse(
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
        Instant createdAt,
        String studentName,
        String studentClass,
        String studentSection,
        String studentRollNumber) {
}
