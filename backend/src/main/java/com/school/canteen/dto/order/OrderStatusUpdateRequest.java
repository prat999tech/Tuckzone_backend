package com.school.canteen.dto.order;

import com.school.canteen.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Canteen-admin status change. deliveryPersonName is required only when moving to
 *  OUT_FOR_DELIVERY. */
public record OrderStatusUpdateRequest(
        @NotNull OrderStatus status,
        @Size(max = 120) String deliveryPersonName) {
}
